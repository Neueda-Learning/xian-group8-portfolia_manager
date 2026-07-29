package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.dto.PerformanceCurvePoint;
import com.group8.portfolio_manager.model.Holding;
import com.group8.portfolio_manager.model.PortfolioHistory;
import com.group8.portfolio_manager.model.TradeRecordWide;
import com.group8.portfolio_manager.repository.HoldingRepository;
import com.group8.portfolio_manager.repository.PriceHistoryRepository;
import com.group8.portfolio_manager.repository.TradeRecordWideRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Supplies data for the Performance line chart (GET /api/performance),
 * supporting time range filters 1M / 3M / 6M / 1Y / MAX.
 *
 * The daily curve is reconstructed on the fly by combining the holdings
 * table (current shares / purchase price / current price / purchase date)
 * with the trade_record_wide audit trail (BUY/SELL/DEPOSIT/WITHDRAW), so
 * the chart reflects real changes to each asset's position over time:
 *   - Share count on a given day = the holding's current shares, adjusted
 *     backwards by every recorded trade for that symbol (trades are
 *     replayed chronologically from a computed baseline so that, after
 *     applying every trade, the result matches the holding's current
 *     share count exactly).
 *   - Price on a given day is estimated by linearly interpolating between
 *     the holding's purchase_price (at purchase_date) and its
 *     current_price (as of today), since no daily close price is stored
 *     for every asset/date. This needs no extra market data and still
 *     lets the chart reflect each asset's actual purchase-to-current gain.
 */
@Service
public class PerformanceService {

    private static final Set<String> INCREASE_TYPES = Set.of("BUY", "DEPOSIT");
    private static final Set<String> DECREASE_TYPES = Set.of("SELL", "WITHDRAW");

    private final HoldingRepository holdingRepository;
    private final TradeRecordWideRepository tradeRecordWideRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public PerformanceService(HoldingRepository holdingRepository,
                               TradeRecordWideRepository tradeRecordWideRepository,
                               PriceHistoryRepository priceHistoryRepository) {
        this.holdingRepository = holdingRepository;
        this.tradeRecordWideRepository = tradeRecordWideRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public List<PortfolioHistory> getPerformance(String range) {
        List<Holding> holdings = holdingRepository.findAll();
        if (holdings.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDate today = LocalDate.now();

        // Group trades per symbol, oldest first, so we can replay share-count changes.
        Map<String, List<TradeRecordWide>> tradesBySymbol = new HashMap<>();
        for (TradeRecordWide trade : tradeRecordWideRepository.findAllOrderedByDate()) {
            tradesBySymbol.computeIfAbsent(trade.getAssetSymbol(), k -> new ArrayList<>()).add(trade);
        }

        LocalDate earliest = today;
        for (Holding h : holdings) {
            if (h.getPurchaseDate() != null && h.getPurchaseDate().isBefore(earliest)) {
                earliest = h.getPurchaseDate();
            }
        }
        for (List<TradeRecordWide> trades : tradesBySymbol.values()) {
            for (TradeRecordWide t : trades) {
                if (t.getTradeDate() != null && t.getTradeDate().isBefore(earliest)) {
                    earliest = t.getTradeDate();
                }
            }
        }

        LocalDate startDate = resolveStartDate(range, today, earliest);

        // Precompute each holding's price-noise bridge once (O(totalDays) per holding)
        // instead of recomputing the whole random walk on every single day lookup.
        Map<Integer, double[]> noiseByHolding = new HashMap<>();
        for (Holding holding : holdings) {
            if (holding.getPurchaseDate() != null) {
                int totalDays = (int) ChronoUnit.DAYS.between(holding.getPurchaseDate(), today);
                noiseByHolding.put(holding.getId(), buildBrownianBridge(holding, totalDays));
            }
        }

        List<PortfolioHistory> points = new ArrayList<>();
        // Use the first day with a non-zero value as the return-rate baseline (leading
        // days can legitimately be $0 while no holding has been purchased yet), so the
        // rate isn't stuck at 0% for the whole window just because day one was empty.
        BigDecimal baseValue = null;
        for (LocalDate day = startDate; !day.isAfter(today); day = day.plusDays(1)) {
            BigDecimal dayValue = BigDecimal.ZERO;
            for (Holding holding : holdings) {
                if (holding.getPurchaseDate() == null) {
                    continue;
                }
                BigDecimal shares = sharesOnDay(holding, tradesBySymbol.get(holding.getSymbol()), day);
                if (day.isBefore(holding.getPurchaseDate())) {
                    // Before the purchase date, the money used to buy this holding is
                    // already part of the portfolio (held as cash/equivalent). Counting
                    // it at cost basis keeps total net worth continuous instead of making
                    // it jump the moment the asset is "purchased".
                    BigDecimal purchasePrice = holding.getPurchasePrice() == null ? BigDecimal.ZERO : holding.getPurchasePrice();
                    dayValue = dayValue.add(shares.multiply(purchasePrice));
                    continue;
                }
                BigDecimal price = interpolatePrice(holding, today, day, noiseByHolding.get(holding.getId()));
                dayValue = dayValue.add(shares.multiply(price));
            }
            if (baseValue == null && dayValue.compareTo(BigDecimal.ZERO) != 0) {
                baseValue = dayValue;
            }
            BigDecimal rate = (baseValue == null || baseValue.compareTo(BigDecimal.ZERO) == 0)
                    ? BigDecimal.ZERO
                    : dayValue.subtract(baseValue).divide(baseValue, 6, RoundingMode.HALF_UP);
            points.add(new PortfolioHistory(0, day, dayValue.setScale(2, RoundingMode.HALF_UP), rate));
        }
        return points;
    }

    private LocalDate resolveStartDate(String range, LocalDate today, LocalDate earliest) {
        if (range == null || range.isBlank()) {
            return earliest;
        }
        LocalDate candidate = switch (range.toUpperCase()) {
            case "1M" -> today.minusMonths(1);
            case "3M" -> today.minusMonths(3);
            case "6M" -> today.minusMonths(6);
            case "1Y" -> today.minusYears(1);
            default -> earliest; // "MAX" or unrecognized -> full history
        };
        return candidate.isBefore(earliest) ? earliest : candidate;
    }

    /**
     * Reconstructs the share count of {@code holding} as of {@code day} by starting
     * from a baseline (current shares minus the net effect of every recorded trade)
     * and replaying trades chronologically up to and including {@code day}.
     */
    private BigDecimal sharesOnDay(Holding holding, List<TradeRecordWide> trades, LocalDate day) {
        BigDecimal currentShares = holding.getShares() == null ? BigDecimal.ZERO : holding.getShares();
        if (trades == null || trades.isEmpty()) {
            return currentShares;
        }

        BigDecimal netEffect = BigDecimal.ZERO;
        for (TradeRecordWide trade : trades) {
            netEffect = netEffect.add(signedShareDelta(trade));
        }
        BigDecimal baseline = currentShares.subtract(netEffect);

        BigDecimal runningShares = baseline;
        for (TradeRecordWide trade : trades) {
            if (trade.getTradeDate() != null && !trade.getTradeDate().isAfter(day)) {
                runningShares = runningShares.add(signedShareDelta(trade));
            }
        }
        return runningShares;
    }

    private BigDecimal signedShareDelta(TradeRecordWide trade) {
        BigDecimal shares = trade.getTradeShares() == null ? BigDecimal.ZERO : trade.getTradeShares();
        String type = trade.getTradeTypeCode() == null ? "" : trade.getTradeTypeCode().toUpperCase();
        if (INCREASE_TYPES.contains(type)) {
            return shares;
        }
        if (DECREASE_TYPES.contains(type)) {
            return shares.negate();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Linearly interpolates between purchasePrice (at purchaseDate) and currentPrice
     * (as of today) for the given day, then adds a deterministic "Brownian bridge"
     * noise term (precomputed in {@code bridge}) so the price fluctuates day to day
     * like a real market instead of moving in a straight line. The bridge is anchored
     * at 0 on both the purchase date and today, so the exact purchasePrice/currentPrice
     * endpoints are always preserved; only the path in between wiggles.
     */
    private BigDecimal interpolatePrice(Holding holding, LocalDate today, LocalDate day, double[] bridge) {
        BigDecimal purchasePrice = holding.getPurchasePrice() == null ? BigDecimal.ZERO : holding.getPurchasePrice();
        BigDecimal currentPrice = holding.getCurrentPrice() == null ? purchasePrice : holding.getCurrentPrice();
        LocalDate purchaseDate = holding.getPurchaseDate();

        long totalDays = ChronoUnit.DAYS.between(purchaseDate, today);
        if (totalDays <= 0) {
            return currentPrice;
        }
        long elapsedDays = ChronoUnit.DAYS.between(purchaseDate, day);
        if (elapsedDays <= 0) {
            return purchasePrice;
        }
        if (elapsedDays >= totalDays) {
            return currentPrice;
        }

        BigDecimal ratio = BigDecimal.valueOf(elapsedDays).divide(BigDecimal.valueOf(totalDays), 8, RoundingMode.HALF_UP);
        BigDecimal trendPrice = purchasePrice.add(currentPrice.subtract(purchasePrice).multiply(ratio));

        double noise = (bridge == null || elapsedDays >= bridge.length) ? 0.0 : bridge[(int) elapsedDays];
        BigDecimal fluctuation = trendPrice.multiply(BigDecimal.valueOf(noise));
        BigDecimal price = trendPrice.add(fluctuation);

        // Never let daily noise push the price to zero/negative.
        BigDecimal floor = trendPrice.multiply(BigDecimal.valueOf(0.05)).abs();
        return price.compareTo(floor) < 0 ? floor : price;
    }

    /**
     * Deterministic daily volatility (as a fraction of price) by asset category,
     * roughly modeled on typical real-world behavior: stocks/crypto swing more day
     * to day, bonds/cash/real-estate are far steadier.
     */
    private double dailyVolatility(Holding holding) {
        String category = holding.getCategoryName() == null ? "" : holding.getCategoryName();
        return switch (category) {
            case "Cash" -> 0.0;
            case "Bond" -> 0.002;
            case "Real Estate" -> 0.0015;
            case "ETF" -> 0.008;
            case "Cryptocurrency" -> 0.035;
            case "Stock" -> 0.014;
            default -> 0.01;
        };
    }

    /**
     * Builds a discrete Brownian bridge (random walk forced to 0 at both t=0 and
     * t=totalDays), returned as an array indexed by day offset from purchaseDate, so
     * each day's fluctuation can be looked up in O(1). Uses a Random seeded from the
     * holding's symbol + purchase date so the same holding always produces the same
     * fluctuation across repeated calls (not re-randomized on every request).
     */
    private double[] buildBrownianBridge(Holding holding, int totalDays) {
        double[] bridge = new double[Math.max(totalDays + 1, 1)];
        double sigma = dailyVolatility(holding);
        if (sigma == 0.0 || totalDays <= 1) {
            return bridge; // all zeros -> pure linear trend, no fluctuation
        }

        long seed = java.util.Objects.hash(holding.getSymbol(), holding.getPurchaseDate());
        java.util.Random random = new java.util.Random(seed);

        double[] walk = new double[totalDays + 1];
        for (int i = 1; i <= totalDays; i++) {
            walk[i] = walk[i - 1] + random.nextGaussian() * sigma;
        }
        double walkAtTotal = walk[totalDays];
        for (int i = 0; i <= totalDays; i++) {
            double ratio = (double) i / (double) totalDays;
            bridge[i] = walk[i] - ratio * walkAtTotal;
        }
        return bridge;
    }

    /**
     * Fine-grained performance curve computed from every raw intraday tick stored in
     * price_history (only covers the ~1 week window for which sample market data was
     * fetched). Kept as a separate endpoint (GET /api/performance/curve) for callers
     * that want true tick-level granularity within that window.
     */
    public List<PerformanceCurvePoint> getCurve() {
        Map<Timestamp, BigDecimal> curve = priceHistoryRepository.computePortfolioValueCurve();
        List<PerformanceCurvePoint> points = new ArrayList<>();
        if (curve.isEmpty()) {
            return points;
        }

        Map<Timestamp, BigDecimal> ordered = new TreeMap<>(curve);
        BigDecimal base = ordered.values().iterator().next();
        for (Map.Entry<Timestamp, BigDecimal> entry : ordered.entrySet()) {
            BigDecimal value = entry.getValue();
            BigDecimal rate = base.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : value.subtract(base).divide(base, 6, RoundingMode.HALF_UP);
            points.add(new PerformanceCurvePoint(entry.getKey().toLocalDateTime(), value, rate));
        }
        return points;
    }
}

