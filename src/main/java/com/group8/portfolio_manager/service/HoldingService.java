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
import java.util.regex.Pattern;

@Service
public class HoldingService {
    private static final String SAMPLE_PRICE_API = "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker=";
    private static final Set<String> SUPPORTED_TICKERS = Set.of("C", "AMZN", "TSLA", "FB", "AAPL");
    private static final Map<String, String> DEFAULT_COMPANY_NAMES = Map.of(
            "C", "Citigroup Inc",
            "AMZN", "Amazon.com Inc",
            "TSLA", "Tesla Inc",
            "FB", "Meta Platforms Inc",
            "AAPL", "Apple Inc",
            "USD_CASH", "US Dollar Cash"
    );
    private static final String CASH_SYMBOL = "USD_CASH";
    private static final String CASH_ASSET_NAME = "US Dollar Cash";
    private static final String CASH_CATEGORY_NAME = "Cash";
    private static final int CASH_CATEGORY_ID = 3;
    private static final BigDecimal CASH_UNIT_PRICE = new BigDecimal("0.01");
    private static final BigDecimal CENT_FACTOR = new BigDecimal("100");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9_.-]{1,30}$");

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
        return getAllHoldings(null);
    }

    public List<Holding> getAllHoldings(Integer categoryId) {
        if (categoryId == null) {
            return repository.findAll();
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId must be a positive integer");
        }
        return repository.findAllByCategoryId(categoryId);
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
        if (holding.getCategoryId() == null || holding.getCategoryId() <= 0) {
            throw new IllegalArgumentException("categoryId must be a positive integer");
        }
        if (holding.getShares() == null) {
            throw new IllegalArgumentException("shares is required");
        }
        if (holding.getShares().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("shares must be >= 0");
        }
        if (holding.getPurchasePrice() == null) {
            throw new IllegalArgumentException("purchasePrice is required");
        }
        if (holding.getPurchasePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("purchasePrice must be > 0");
        }
        if (holding.getPurchaseDate() == null) {
            throw new IllegalArgumentException("purchaseDate is required");
        }

        String symbol = holding.getSymbol().trim().toUpperCase(Locale.ROOT);
        holding.setSymbol(symbol);
        validateSymbolFormat(symbol, "symbol");

        if (holding.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("purchaseDate cannot be in the future");
        }

        String companyName = holding.getCompanyName();
        if (companyName == null || companyName.isBlank()) {
            holding.setCompanyName(DEFAULT_COMPANY_NAMES.getOrDefault(symbol, symbol));
        } else if (companyName.length() > 100) {
            throw new IllegalArgumentException("companyName length must be <= 100");
        }

        if (CASH_SYMBOL.equals(symbol)) {
            // Cash shares are stored in cent units; input amount is treated as USD.
            holding.setShares(holding.getShares().multiply(CENT_FACTOR));
            holding.setCurrentPrice(CASH_UNIT_PRICE);
            holding.setPurchasePrice(CASH_UNIT_PRICE);
        } else {
            if (SUPPORTED_TICKERS.contains(symbol)) {
                try {
                    holding.setCurrentPrice(fetchLatestClosePrice(symbol));
                } catch (IOException e) {
                    throw new IllegalStateException("failed to fetch currentPrice from market API", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted while fetching currentPrice from market API", e);
                }
            } else {
                if (holding.getCurrentPrice() == null || holding.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("currentPrice is required for unsupported ticker: " + symbol);
                }
            }
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
        validateSymbolFormat(symbol, "assetSymbol");

        BigDecimal tradeShares = request.getTradeShares();
        if (tradeShares.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("tradeShares must be > 0");
        }
        BigDecimal tradePrice = request.getTradePrice();
        BigDecimal actualTradeShares = tradeShares; // Original value for non-cash, will be adjusted for cash

        // For cash asset, fix price at 0.01 and convert amount to cent units
        if (CASH_SYMBOL.equals(symbol)) {
            tradePrice = CASH_UNIT_PRICE;
            // Convert USD amount to cent units: tradeShares should be USD amount, convert to cents
            actualTradeShares = tradeShares.multiply(CENT_FACTOR);
        } else {
            tradePrice = tradePrice == null ? BigDecimal.ONE : tradePrice;
        }

        BigDecimal fee = request.getFee() == null ? BigDecimal.ZERO : request.getFee();
        LocalDate tradeDate = request.getTradeDate() == null ? LocalDate.now() : request.getTradeDate();
        if (tradeDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("tradeDate cannot be in the future");
        }
        BigDecimal tradeAmount = actualTradeShares.multiply(tradePrice);

        Holding cashHolding = ensureCashHolding();
        BigDecimal cashChange;
        Holding assetHolding = null;

        if (CASH_SYMBOL.equals(symbol)) {
            if (!"DEPOSIT".equals(tradeType) && !"WITHDRAW".equals(tradeType)) {
                throw new IllegalArgumentException("cash only supports DEPOSIT or WITHDRAW");
            }
            cashChange = applyCashOnlyTrade(cashHolding, tradeType, tradeAmount, fee, tradePrice, tradeDate);
        } else {
            if (!"BUY".equals(tradeType) && !"SELL".equals(tradeType)) {
                throw new IllegalArgumentException("non-cash assets only support BUY or SELL");
            }
            assetHolding = repository.findBySymbol(symbol);
            if (assetHolding == null) {
                throw new IllegalArgumentException("asset symbol not found: " + symbol);
            }

            if (tradePrice == null) {
                tradePrice = assetHolding.getCurrentPrice();
                if (tradePrice == null || tradePrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("tradePrice is required when current price is unavailable");
                }
                tradeAmount = actualTradeShares.multiply(tradePrice);
            }

            cashChange = applyAssetTrade(assetHolding, cashHolding, tradeType, actualTradeShares, tradePrice, tradeAmount, fee, tradeDate);
        }

        TradeRecordWide tradeRecord = buildTradeRecord(
                symbol,
                tradeType,
                tradeDate,
                actualTradeShares,
                tradeAmount,
                cashChange,
                assetHolding,
                tradePrice,
                fee,
                request.getNote()
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
        String normalizedTradeType = request.getTradeTypeCode().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("BUY", "SELL", "DEPOSIT", "WITHDRAW").contains(normalizedTradeType)) {
            throw new IllegalArgumentException("tradeTypeCode must be one of BUY/SELL/DEPOSIT/WITHDRAW");
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
        if (request.getTradeDate() != null && request.getTradeDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("tradeDate cannot be in the future");
        }
        if (request.getNote() != null && request.getNote().length() > 200) {
            throw new IllegalArgumentException("note length must be <= 200");
        }
    }

    private BigDecimal applyCashOnlyTrade(Holding cashHolding, String tradeType, BigDecimal tradeAmount, BigDecimal fee, BigDecimal tradePrice, LocalDate tradeDate) {
        // cashHolding.shares is stored in cent units (1 share = 0.01 USD).
        BigDecimal oldCashShares = safeValue(cashHolding.getShares());
        BigDecimal cashChangeUsd;
        if ("DEPOSIT".equals(tradeType)) {
            cashChangeUsd = tradeAmount.subtract(fee);
            BigDecimal cashChangeShares = cashChangeUsd.multiply(CENT_FACTOR);
            cashHolding.setShares(oldCashShares.add(cashChangeShares));
        } else {
            cashChangeUsd = tradeAmount.add(fee).negate();
            BigDecimal cashChangeShares = cashChangeUsd.multiply(CENT_FACTOR);
            BigDecimal newCashShares = oldCashShares.add(cashChangeShares);
            if (newCashShares.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("cash is not enough");
            }
            cashHolding.setShares(newCashShares);
        }
        cashHolding.setCurrentPrice(CASH_UNIT_PRICE);
        cashHolding.setPurchasePrice(CASH_UNIT_PRICE);
        repository.updateHoldingAfterTrade(cashHolding.getId(), cashHolding.getShares(), cashHolding.getPurchasePrice(), cashHolding.getCurrentPrice());
        return cashChangeUsd;
    }

    private BigDecimal applyAssetTrade(Holding assetHolding,
                                       Holding cashHolding,
                                       String tradeType,
                                       BigDecimal tradeShares,
                                       BigDecimal tradePrice,
                                       BigDecimal tradeAmount,
                                       BigDecimal fee, LocalDate tradeDate) {
        BigDecimal oldAssetShares = safeValue(assetHolding.getShares());
        // cashHolding.shares is stored in cent units.
        BigDecimal oldCashShares = safeValue(cashHolding.getShares());

        if ("BUY".equals(tradeType)) {
            BigDecimal cashNeedUsd = tradeAmount.add(fee);
            BigDecimal cashNeedShares = cashNeedUsd.multiply(CENT_FACTOR);
            if (oldCashShares.compareTo(cashNeedShares) < 0) {
                throw new IllegalArgumentException("cash is not enough for buy trade");
            }

            BigDecimal newAssetShares = oldAssetShares.add(tradeShares);
            BigDecimal oldCostAmount = oldAssetShares.multiply(safeValue(assetHolding.getPurchasePrice()));
            BigDecimal newCostAmount = tradeShares.multiply(tradePrice);
            BigDecimal avgCost = oldCostAmount.add(newCostAmount)
                    .divide(newAssetShares, 2, RoundingMode.HALF_UP);

            assetHolding.setShares(newAssetShares);
            assetHolding.setPurchasePrice(avgCost);
            BigDecimal effectiveAssetPrice = tradeDate.equals(LocalDate.now())
                    ? tradePrice
                    : safeCurrentPrice(assetHolding.getCurrentPrice(), tradePrice);
            assetHolding.setCurrentPrice(effectiveAssetPrice);
            repository.updateHoldingAfterTrade(assetHolding.getId(), assetHolding.getShares(), assetHolding.getPurchasePrice(), assetHolding.getCurrentPrice());

            BigDecimal cashChangeUsd = cashNeedUsd.negate();
            BigDecimal cashChangeShares = cashChangeUsd.multiply(CENT_FACTOR);
            cashHolding.setShares(oldCashShares.add(cashChangeShares));
            BigDecimal effectiveCashPrice = tradeDate.equals(LocalDate.now())
                    ? CASH_UNIT_PRICE
                    : safeCurrentPrice(cashHolding.getCurrentPrice(), CASH_UNIT_PRICE);
            repository.updateHoldingSharesAndPrice(cashHolding.getId(), cashHolding.getShares(), effectiveCashPrice);
            return cashChangeUsd;
        }

        if (oldAssetShares.compareTo(tradeShares) < 0) {
            throw new IllegalArgumentException("asset shares are not enough for sell trade");
        }

        BigDecimal newAssetShares = oldAssetShares.subtract(tradeShares);
        assetHolding.setShares(newAssetShares);
        BigDecimal effectiveAssetPrice = tradeDate.equals(LocalDate.now())
                ? tradePrice
                : safeCurrentPrice(assetHolding.getCurrentPrice(), tradePrice);
        assetHolding.setCurrentPrice(effectiveAssetPrice);
        repository.updateHoldingSharesAndPrice(assetHolding.getId(), assetHolding.getShares(), assetHolding.getCurrentPrice());

        BigDecimal cashChangeUsd = tradeAmount.subtract(fee);
        BigDecimal cashChangeShares = cashChangeUsd.multiply(CENT_FACTOR);
        cashHolding.setShares(oldCashShares.add(cashChangeShares));
        repository.updateHoldingSharesAndPrice(cashHolding.getId(), cashHolding.getShares(), CASH_UNIT_PRICE);
        return cashChangeUsd;
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
        newCash.setPurchasePrice(CASH_UNIT_PRICE);
        newCash.setCurrentPrice(CASH_UNIT_PRICE);
        newCash.setPurchaseDate(LocalDate.now());
        int id = repository.save(newCash);
        return repository.findById(id);
    }

    private TradeRecordWide buildTradeRecord(String symbol,
                                             String tradeType,
                                             LocalDate tradeDate,
                                             BigDecimal tradeShares,
                                             BigDecimal tradeAmount,
                                             BigDecimal cashChange,
                                             Holding assetHolding,
                                             BigDecimal tradePrice,
                                             BigDecimal fee,
                                             String note) {
        TradeRecordWide trade = new TradeRecordWide();
        trade.setTradeNo(createTradeNo(tradeDate));
        trade.setAssetSymbol(symbol);
        trade.setTradeTypeCode(tradeType);
        trade.setTradeTypeName(toTradeTypeName(tradeType));
        trade.setTradeShares(tradeShares);
        trade.setTradeAmount(tradeAmount.setScale(2, RoundingMode.HALF_UP));
        trade.setFee(fee.setScale(2, RoundingMode.HALF_UP));
        trade.setCurrency("USD");
        trade.setCashAssetSymbol(CASH_SYMBOL);
        trade.setCashChange(cashChange.setScale(2, RoundingMode.HALF_UP));
        trade.setTradeDate(tradeDate);
        trade.setNote(note);

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

    private BigDecimal safeCurrentPrice(BigDecimal currentPrice, BigDecimal fallbackPrice) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return fallbackPrice;
        }
        return currentPrice;
    }

    private void validateSymbolFormat(String symbol, String fieldName) {
        if (!SYMBOL_PATTERN.matcher(symbol).matches()) {
            throw new IllegalArgumentException(fieldName + " format is invalid");
        }
    }
}
