package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.dto.HoldingTradeRequest;
import com.group8.portfolio_manager.model.Holding;
import com.group8.portfolio_manager.model.TradeRecordWide;
import com.group8.portfolio_manager.repository.HoldingRepository;
import com.group8.portfolio_manager.repository.TradeRecordWideRepository;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class HoldingService {
    private static final String SAMPLE_PRICE_API = "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker=";
    private static final Set<String> SUPPORTED_TICKERS = Set.of("C", "AMZN", "TSLA", "FB", "AAPL");
    private static final String CASH_SYMBOL = "USD_CASH";
    private static final String CASH_ASSET_NAME = "US Dollar Cash";
    private static final String CASH_CATEGORY_NAME = "Cash";
    private static final int CASH_CATEGORY_ID = 3;

    private final HoldingRepository repository;
    private final TradeRecordWideRepository tradeRecordWideRepository;
    private final HttpClient httpClient;
    private final JsonParser jsonParser;

    public HoldingService(HoldingRepository repository, TradeRecordWideRepository tradeRecordWideRepository) {
        this.repository = repository;
        this.tradeRecordWideRepository = tradeRecordWideRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.jsonParser = JsonParserFactory.getJsonParser();
    }

    public List<Holding> getAllHoldings() {
        return repository.findAll();
    }

    public Holding getHoldingById(int id) {
        return repository.findById(id);
    }

    public Holding addHolding(Holding holding) {
        if (holding == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (holding.getSymbol() == null || holding.getSymbol().isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (holding.getCompanyName() == null || holding.getCompanyName().isBlank()) {
            throw new IllegalArgumentException("companyName is required");
        }
        if (holding.getCategoryId() == null || holding.getCategoryId() <= 0) {
            throw new IllegalArgumentException("categoryId must be a positive integer");
        }
        if (holding.getShares() == null) {
            throw new IllegalArgumentException("shares is required");
        }
        if (holding.getPurchasePrice() == null) {
            throw new IllegalArgumentException("purchasePrice is required");
        }
        if (holding.getCurrentPrice() == null) {
            throw new IllegalArgumentException("currentPrice is required");
        }
        if (holding.getPurchaseDate() == null) {
            throw new IllegalArgumentException("purchaseDate is required");
        }

        int id = repository.save(holding);
        holding.setId(id);
        return holding;
    }

    public boolean deleteHolding(int id) {
        return repository.deleteById(id) > 0;
    }

    @Transactional
    public TradeRecordWide tradeHolding(HoldingTradeRequest request) {
        validateTradeRequest(request);

        String symbol = request.getAssetSymbol().trim().toUpperCase(Locale.ROOT);
        String tradeType = request.getTradeTypeCode().trim().toUpperCase(Locale.ROOT);
        BigDecimal tradeShares = request.getTradeShares();
        BigDecimal tradePrice = request.getTradePrice() == null ? BigDecimal.ONE : request.getTradePrice();
        BigDecimal fee = request.getFee() == null ? BigDecimal.ZERO : request.getFee();
        LocalDate tradeDate = request.getTradeDate() == null ? LocalDate.now() : request.getTradeDate();
        BigDecimal tradeAmount = tradeShares.multiply(tradePrice);

        Holding cashHolding = ensureCashHolding();
        BigDecimal cashChange;
        Holding assetHolding = null;

        if (CASH_SYMBOL.equals(symbol)) {
            if (!"DEPOSIT".equals(tradeType) && !"WITHDRAW".equals(tradeType)) {
                throw new IllegalArgumentException("cash only supports DEPOSIT or WITHDRAW");
            }
            cashChange = applyCashOnlyTrade(cashHolding, tradeType, tradeAmount, fee, tradePrice);
        } else {
            if (!"BUY".equals(tradeType) && !"SELL".equals(tradeType)) {
                throw new IllegalArgumentException("non-cash assets only support BUY or SELL");
            }
            assetHolding = repository.findBySymbol(symbol);
            if (assetHolding == null) {
                throw new IllegalArgumentException("asset symbol not found: " + symbol);
            }
            cashChange = applyAssetTrade(assetHolding, cashHolding, tradeType, tradeShares, tradePrice, tradeAmount, fee);
        }

        TradeRecordWide tradeRecord = buildTradeRecord(
                request,
                symbol,
                tradeType,
                tradeDate,
                tradeAmount,
                cashChange,
                assetHolding,
                tradePrice,
                fee
        );
        long id = tradeRecordWideRepository.save(tradeRecord);
        tradeRecord.setId(id);
        return tradeRecord;
    }

    public List<TradeRecordWide> getRecentTrades(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return tradeRecordWideRepository.findRecent(safeLimit);
    }

    public Map<String, Object> refreshCurrentPrices() {
        Set<String> symbols = new TreeSet<>();
        for (Holding holding : repository.findAll()) {
            if (holding.getSymbol() != null && !holding.getSymbol().isBlank()) {
                symbols.add(holding.getSymbol().trim().toUpperCase(Locale.ROOT));
            }
        }

        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        int updatedRows = 0;

        for (String symbol : symbols) {
            if (!SUPPORTED_TICKERS.contains(symbol)) {
                skipped.add(symbol);
                continue;
            }
            try {
                BigDecimal latestPrice = fetchLatestClosePrice(symbol);
                updatedRows += repository.updateCurrentPriceBySymbol(symbol, latestPrice);
            } catch (IOException e) {
                failed.add(symbol + " (IO error: " + e.getMessage() + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failed.add(symbol + " (interrupted)");
                break;
            } catch (IllegalStateException | ClassCastException | NumberFormatException e) {
                failed.add(symbol + " (parse error: " + e.getMessage() + ")");
            } catch (RuntimeException e) {
                failed.add(symbol + " (unexpected error: " + e.getMessage() + ")");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestedSymbols", new ArrayList<>(symbols));
        result.put("updatedRows", updatedRows);
        result.put("skippedUnsupported", skipped);
        result.put("failed", failed);
        return result;
    }

    public Map<String, Object> getPriceSeries(String ticker) throws IOException, InterruptedException {
        String symbol = normalizeSupportedTicker(ticker);
        Map<String, Object> priceDataMap = fetchPriceDataMap(symbol);

        Object closeObj = priceDataMap.get("close");
        if (!(closeObj instanceof List<?> closes) || closes.isEmpty()) {
            throw new IllegalStateException("missing close data");
        }
        Object timestampObj = priceDataMap.get("timestamp");
        if (!(timestampObj instanceof List<?> timestamps) || timestamps.isEmpty()) {
            throw new IllegalStateException("missing timestamp data");
        }

        List<BigDecimal> closeSeries = new ArrayList<>(closes.size());
        for (Object close : closes) {
            closeSeries.add(new BigDecimal(String.valueOf(close)));
        }

        List<String> timestampSeries = new ArrayList<>(timestamps.size());
        for (Object timestamp : timestamps) {
            timestampSeries.add(String.valueOf(timestamp));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticker", symbol);
        result.put("close", closeSeries);
        result.put("timestamp", timestampSeries);
        return result;
    }

    private BigDecimal fetchLatestClosePrice(String symbol) throws IOException, InterruptedException {
        Map<String, Object> priceDataMap = fetchPriceDataMap(symbol);
        Object closeObj = priceDataMap.get("close");
        if (!(closeObj instanceof List<?> closes) || closes.isEmpty()) {
            throw new IllegalStateException("missing close data");
        }
        for (int i = closes.size() - 1; i >= 0; i--) {
            Object latest = closes.get(i);
            if (latest == null) {
                continue;
            }

            String value = String.valueOf(latest).trim();
            if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                continue;
            }
            return new BigDecimal(value);
        }

        throw new IllegalStateException("missing valid close data");
    }

    private Map<String, Object> fetchPriceDataMap(String symbol) throws IOException, InterruptedException {
        String url = SAMPLE_PRICE_API + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        String sanitizedBody = response.body().replaceAll("\\bNaN\\b", "null");
        Map<String, Object> root = jsonParser.parseMap(sanitizedBody);
        Object priceDataObj = root.get("price_data");
        if (!(priceDataObj instanceof Map<?, ?> priceDataMap)) {
            throw new IllegalStateException("missing price_data");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) priceDataMap;
        return typed;
    }

    private String normalizeSupportedTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("ticker is required");
        }
        String symbol = ticker.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_TICKERS.contains(symbol)) {
            throw new IllegalArgumentException("unsupported ticker: " + symbol + " (supported: " + SUPPORTED_TICKERS + ")");
        }
        return symbol;
    }

    private void validateTradeRequest(HoldingTradeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.getAssetSymbol() == null || request.getAssetSymbol().isBlank()) {
            throw new IllegalArgumentException("assetSymbol is required");
        }
        if (request.getTradeTypeCode() == null || request.getTradeTypeCode().isBlank()) {
            throw new IllegalArgumentException("tradeTypeCode is required");
        }
        if (request.getTradeShares() == null || request.getTradeShares().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("tradeShares must be > 0");
        }
        if (request.getTradePrice() != null && request.getTradePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("tradePrice must be > 0 when provided");
        }
        if (request.getFee() != null && request.getFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("fee must be >= 0");
        }
    }

    private BigDecimal applyCashOnlyTrade(Holding cashHolding, String tradeType, BigDecimal tradeAmount, BigDecimal fee, BigDecimal tradePrice) {
        BigDecimal oldCash = safeValue(cashHolding.getShares());
        BigDecimal cashChange;
        if ("DEPOSIT".equals(tradeType)) {
            cashChange = tradeAmount.subtract(fee);
            cashHolding.setShares(oldCash.add(cashChange));
        } else {
            cashChange = tradeAmount.add(fee).negate();
            BigDecimal newCash = oldCash.add(cashChange);
            if (newCash.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("cash is not enough");
            }
            cashHolding.setShares(newCash);
        }
        cashHolding.setCurrentPrice(tradePrice);
        cashHolding.setPurchasePrice(BigDecimal.ONE);
        repository.updateHoldingAfterTrade(cashHolding.getId(), cashHolding.getShares(), cashHolding.getPurchasePrice(), cashHolding.getCurrentPrice());
        return cashChange;
    }

    private BigDecimal applyAssetTrade(Holding assetHolding,
                                       Holding cashHolding,
                                       String tradeType,
                                       BigDecimal tradeShares,
                                       BigDecimal tradePrice,
                                       BigDecimal tradeAmount,
                                       BigDecimal fee) {
        BigDecimal oldAssetShares = safeValue(assetHolding.getShares());
        BigDecimal oldCash = safeValue(cashHolding.getShares());

        if ("BUY".equals(tradeType)) {
            BigDecimal cashNeed = tradeAmount.add(fee);
            if (oldCash.compareTo(cashNeed) < 0) {
                throw new IllegalArgumentException("cash is not enough for buy trade");
            }

            BigDecimal newAssetShares = oldAssetShares.add(tradeShares);
            BigDecimal oldCostAmount = oldAssetShares.multiply(safeValue(assetHolding.getPurchasePrice()));
            BigDecimal newCostAmount = tradeShares.multiply(tradePrice);
            BigDecimal avgCost = oldCostAmount.add(newCostAmount)
                    .divide(newAssetShares, 2, RoundingMode.HALF_UP);

            assetHolding.setShares(newAssetShares);
            assetHolding.setPurchasePrice(avgCost);
            assetHolding.setCurrentPrice(tradePrice);
            repository.updateHoldingAfterTrade(assetHolding.getId(), assetHolding.getShares(), assetHolding.getPurchasePrice(), assetHolding.getCurrentPrice());

            BigDecimal cashChange = cashNeed.negate();
            cashHolding.setShares(oldCash.add(cashChange));
            repository.updateHoldingSharesAndPrice(cashHolding.getId(), cashHolding.getShares(), BigDecimal.ONE);
            return cashChange;
        }

        if (oldAssetShares.compareTo(tradeShares) < 0) {
            throw new IllegalArgumentException("asset shares are not enough for sell trade");
        }

        BigDecimal newAssetShares = oldAssetShares.subtract(tradeShares);
        assetHolding.setShares(newAssetShares);
        assetHolding.setCurrentPrice(tradePrice);
        repository.updateHoldingSharesAndPrice(assetHolding.getId(), assetHolding.getShares(), assetHolding.getCurrentPrice());

        BigDecimal cashChange = tradeAmount.subtract(fee);
        cashHolding.setShares(oldCash.add(cashChange));
        repository.updateHoldingSharesAndPrice(cashHolding.getId(), cashHolding.getShares(), BigDecimal.ONE);
        return cashChange;
    }

    private Holding ensureCashHolding() {
        Holding cash = repository.findBySymbol(CASH_SYMBOL);
        if (cash != null) {
            return cash;
        }

        Holding newCash = new Holding();
        newCash.setSymbol(CASH_SYMBOL);
        newCash.setCompanyName(CASH_ASSET_NAME);
        newCash.setCategoryId(CASH_CATEGORY_ID);
        newCash.setShares(BigDecimal.ZERO);
        newCash.setPurchasePrice(BigDecimal.ONE);
        newCash.setCurrentPrice(BigDecimal.ONE);
        newCash.setPurchaseDate(LocalDate.now());
        int id = repository.save(newCash);
        return repository.findById(id);
    }

    private TradeRecordWide buildTradeRecord(HoldingTradeRequest request,
                                             String symbol,
                                             String tradeType,
                                             LocalDate tradeDate,
                                             BigDecimal tradeAmount,
                                             BigDecimal cashChange,
                                             Holding assetHolding,
                                             BigDecimal tradePrice,
                                             BigDecimal fee) {
        TradeRecordWide trade = new TradeRecordWide();
        trade.setTradeNo(createTradeNo(tradeDate));
        trade.setAssetSymbol(symbol);
        trade.setTradeTypeCode(tradeType);
        trade.setTradeTypeName(toTradeTypeName(tradeType));
        trade.setTradeShares(request.getTradeShares());
        trade.setTradeAmount(tradeAmount.setScale(2, RoundingMode.HALF_UP));
        trade.setFee(fee.setScale(2, RoundingMode.HALF_UP));
        trade.setCurrency("USD");
        trade.setCashAssetSymbol(CASH_SYMBOL);
        trade.setCashChange(cashChange.setScale(2, RoundingMode.HALF_UP));
        trade.setTradeDate(tradeDate);
        trade.setNote(request.getNote());

        if ("BUY".equals(tradeType) || "DEPOSIT".equals(tradeType)) {
            trade.setBuyPrice(tradePrice);
            trade.setSellPrice(null);
        } else {
            trade.setBuyPrice(null);
            trade.setSellPrice(tradePrice);
        }

        if (CASH_SYMBOL.equals(symbol)) {
            trade.setCashAsset(true);
            trade.setAssetName(CASH_ASSET_NAME);
            trade.setAssetCategoryName(CASH_CATEGORY_NAME);
        } else {
            trade.setCashAsset(false);
            trade.setAssetName(assetHolding.getCompanyName());
            trade.setAssetCategoryName(assetHolding.getCategoryName());
        }

        return trade;
    }

    private String createTradeNo(LocalDate tradeDate) {
        String datePart = tradeDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        String timePart = String.valueOf(System.currentTimeMillis() % 1_000_000L);
        return "TRX" + datePart + String.format("%06d", Integer.parseInt(timePart));
    }

    private String toTradeTypeName(String tradeTypeCode) {
        if ("BUY".equals(tradeTypeCode)) {
            return "Buy";
        }
        if ("SELL".equals(tradeTypeCode)) {
            return "Sell";
        }
        if ("DEPOSIT".equals(tradeTypeCode)) {
            return "Cash Deposit";
        }
        return "Cash Withdraw";
    }

    private BigDecimal safeValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
