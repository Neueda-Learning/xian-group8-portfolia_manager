package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.dto.HoldingTradeRequest;
import com.group8.portfolio_manager.model.Holding;
import com.group8.portfolio_manager.model.TradeRecordWide;
import com.group8.portfolio_manager.repository.HoldingRepository;
import com.group8.portfolio_manager.repository.TradeRecordWideRepository;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.dao.EmptyResultDataAccessException;
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
    private static final String FX_RATE_API_LATEST = "https://api.frankfurter.dev/v1/latest?from=%s&to=USD";
    private static final String FX_RATE_API_BY_DATE = "https://api.frankfurter.dev/v1/%s?from=%s&to=USD";
    private static final Set<String> SUPPORTED_TICKERS = Set.of("C", "AMZN", "TSLA", "FB", "AAPL");
    private static final Set<String> SUPPORTED_CASH_SYMBOLS = Set.of(
            "USD_CASH", "CNY_CASH", "EUR_CASH", "INR_CASH", "GBP_CASH", "JPY_CASH", "KRW_CASH"
    );
    private static final Map<String, String> DEFAULT_COMPANY_NAMES = Map.ofEntries(
            Map.entry("C", "Citigroup Inc"),
            Map.entry("AMZN", "Amazon.com Inc"),
            Map.entry("TSLA", "Tesla Inc"),
            Map.entry("FB", "Meta Platforms Inc"),
            Map.entry("AAPL", "Apple Inc"),
            Map.entry("USD_CASH", "US Dollar Cash"),
            Map.entry("CNY_CASH", "Chinese Yuan Cash"),
            Map.entry("EUR_CASH", "Euro Cash"),
            Map.entry("INR_CASH", "Indian Rupee Cash"),
            Map.entry("GBP_CASH", "British Pound Cash"),
            Map.entry("JPY_CASH", "Japanese Yen Cash"),
            Map.entry("KRW_CASH", "Korean Won Cash")
    );
    private static final String CASH_SYMBOL = "USD_CASH";
    private static final String CASH_ASSET_NAME = "US Dollar Cash";
    private static final String CASH_CATEGORY_NAME = "Cash";
    private static final int STOCK_CATEGORY_ID = 1;
    private static final int CASH_CATEGORY_ID = 3;
    private static final BigDecimal CASH_UNIT_PRICE = BigDecimal.ONE;
    private static final BigDecimal SHARE_ZERO_TOLERANCE = new BigDecimal("0.00000001");
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
        if (!(holding.getCategoryId() == CASH_CATEGORY_ID)) {
            if (holding.getShares().stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("shares must be a whole number for non-cash assets");
            }
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

        Integer categoryId = holding.getCategoryId();
        boolean cashCategory = isCashCategory(categoryId);
        String symbol = holding.getSymbol().trim().toUpperCase(Locale.ROOT);
        if (cashCategory) {
            symbol = normalizeCashSymbol(symbol);
        }
        holding.setSymbol(symbol);
        validateSymbolFormat(symbol, "symbol");

        if (holding.getPurchaseDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("purchaseDate cannot be in the future");
        }

        if (cashCategory) {
            try {
                BigDecimal latestFxUsdRate = fetchCashUsdRate(symbol, null);
                BigDecimal purchaseDateFxUsdRate = fetchCashUsdRate(symbol, holding.getPurchaseDate());
                holding.setCurrentPrice(latestFxUsdRate);
                holding.setPurchasePrice(purchaseDateFxUsdRate);
            } catch (IOException e) {
                throw new IllegalStateException("failed to fetch FX rate from market API", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while fetching FX rate from market API", e);
            }
        } else if (isStockCategory(categoryId)) {
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

        String companyName = holding.getCompanyName();
        if (companyName == null || companyName.isBlank()) {
            holding.setCompanyName(DEFAULT_COMPANY_NAMES.getOrDefault(symbol, symbol));
        } else if (companyName.length() > 100) {
            throw new IllegalArgumentException("companyName length must be <= 100");
        }

        Holding existing = repository.findBySymbol(symbol);
        if (existing != null
                && Objects.equals(existing.getCategoryId(), categoryId)
                && (cashCategory || isStockCategory(categoryId))) {
            return mergeHoldingOnAdd(existing, holding);
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
        BigDecimal fee = request.getFee() == null ? BigDecimal.ZERO : request.getFee();
        LocalDate tradeDate = request.getTradeDate() == null ? LocalDate.now() : request.getTradeDate();
        if (tradeDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("tradeDate cannot be in the future");
        }
        Holding tradeHolding = findHoldingForTrade(request, symbol);
        symbol = tradeHolding.getSymbol().trim().toUpperCase(Locale.ROOT);
        boolean isCashTrade = isCashCategory(tradeHolding.getCategoryId());

        Holding usdCashHolding = ensureUsdCashHolding();
        BigDecimal cashChange;
        Holding assetHoldingForRecord = null;
        BigDecimal tradeAmount;

        if (isCashTrade) {
            if (!"DEPOSIT".equals(tradeType) && !"WITHDRAW".equals(tradeType)) {
                throw new IllegalArgumentException("cash only supports DEPOSIT or WITHDRAW");
            }
            try {
                tradePrice = fetchCashUsdRate(symbol, tradeDate);
            } catch (IOException e) {
                throw new IllegalStateException("failed to fetch FX rate from market API", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while fetching FX rate from market API", e);
            }
            tradeAmount = tradeShares.multiply(tradePrice);
            if (CASH_SYMBOL.equals(symbol)) {
                cashChange = applyUsdCashTrade(tradeHolding, tradeType, tradeAmount, fee);
            } else {
                cashChange = applyNonUsdCashTrade(tradeHolding, usdCashHolding, tradeType, tradeShares, tradePrice, tradeAmount, fee);
            }
        } else {
            if (!"BUY".equals(tradeType) && !"SELL".equals(tradeType)) {
                throw new IllegalArgumentException("non-cash assets only support BUY or SELL");
            }
            if (tradePrice == null) {
                tradePrice = tradeHolding.getCurrentPrice();
                if (tradePrice == null || tradePrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("tradePrice is required when current price is unavailable");
                }
            }
            tradeAmount = tradeShares.multiply(tradePrice);
            cashChange = applyAssetTrade(tradeHolding, usdCashHolding, tradeType, tradeShares, tradePrice, tradeAmount, fee, tradeDate);
            assetHoldingForRecord = tradeHolding;
        }

        TradeRecordWide tradeRecord = buildTradeRecord(
                symbol,
                tradeType,
                tradeDate,
                tradeShares,
                tradeAmount,
                cashChange,
                assetHoldingForRecord,
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
        List<Holding> holdings = repository.findAll();
        List<String> requestedSymbols = new ArrayList<>();
        Map<String, Integer> symbolCategoryMap = new LinkedHashMap<>();
        for (Holding holding : holdings) {
            if (holding.getSymbol() == null || holding.getSymbol().isBlank()) {
                continue;
            }
            String normalizedSymbol = holding.getSymbol().trim().toUpperCase(Locale.ROOT);
            if (!symbolCategoryMap.containsKey(normalizedSymbol)) {
                requestedSymbols.add(normalizedSymbol);
                symbolCategoryMap.put(normalizedSymbol, holding.getCategoryId());
            }
        }

        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        int updatedRows = 0;

        for (Map.Entry<String, Integer> entry : symbolCategoryMap.entrySet()) {
            String symbol = entry.getKey();
            try {
                if (isCashCategory(entry.getValue())) {
                    BigDecimal fxRate = fetchCashUsdRate(symbol);
                    updatedRows += repository.updateCurrentPriceBySymbol(symbol, fxRate);
                } else if (SUPPORTED_TICKERS.contains(symbol)) {
                    BigDecimal latestPrice = fetchLatestClosePrice(symbol);
                    updatedRows += repository.updateCurrentPriceBySymbol(symbol, latestPrice);
                } else {
                    skipped.add(symbol);
                }
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
        result.put("requestedSymbols", requestedSymbols);
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

    public Map<String, Object> getCashFxRate(String symbol, LocalDate date) throws IOException, InterruptedException {
        String normalizedSymbol = normalizeCashSymbol(symbol);
        BigDecimal usdRate = fetchCashUsdRate(normalizedSymbol, date);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("symbol", normalizedSymbol);
        result.put("currency", currencyFromCashSymbol(normalizedSymbol));
        result.put("usdRate", usdRate);
        result.put("date", date == null ? "latest" : date.toString());
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
        if (request.getHoldingId() == null || request.getHoldingId() <= 0) {
            throw new IllegalArgumentException("holdingId is required");
        }
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId is required");
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

    private BigDecimal applyUsdCashTrade(Holding usdCashHolding,
                                         String tradeType,
                                         BigDecimal tradeAmount,
                                         BigDecimal fee) {
        BigDecimal oldUsdShares = safeValue(usdCashHolding.getShares());
        BigDecimal usdChange = "DEPOSIT".equals(tradeType)
                ? tradeAmount.subtract(fee)
                : tradeAmount.add(fee).negate();
        BigDecimal newUsdShares = normalizeNearZero(oldUsdShares.add(usdChange));
        if (newUsdShares.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("cash is not enough");
        }

        usdCashHolding.setShares(newUsdShares);
        if (isEffectivelyZeroShares(newUsdShares)) {
            repository.deleteById(usdCashHolding.getId());
        } else {
            repository.updateHoldingSharesAndPrice(usdCashHolding.getId(), usdCashHolding.getShares(), CASH_UNIT_PRICE);
        }
        return usdChange;
    }

    private BigDecimal applyNonUsdCashTrade(Holding cashHolding,
                                            Holding usdCashHolding,
                                            String tradeType,
                                            BigDecimal tradeShares,
                                            BigDecimal tradePrice,
                                            BigDecimal tradeAmount,
                                            BigDecimal fee) {
        BigDecimal oldCashShares = safeValue(cashHolding.getShares());
        BigDecimal oldUsdShares = safeValue(usdCashHolding.getShares());
        BigDecimal usdChange;

        if ("DEPOSIT".equals(tradeType)) {
            BigDecimal usdNeed = tradeAmount.add(fee);
            if (oldUsdShares.compareTo(usdNeed) < 0) {
                throw new IllegalArgumentException("cash is not enough for buy trade");
            }
            BigDecimal newCashShares = oldCashShares.add(tradeShares);
            BigDecimal avgCost = weightedAveragePrice(oldCashShares, cashHolding.getPurchasePrice(), tradeShares, tradePrice, newCashShares);
            BigDecimal effectiveCurrent = safeCurrentPrice(cashHolding.getCurrentPrice(), tradePrice);
            cashHolding.setShares(newCashShares);
            cashHolding.setPurchasePrice(avgCost);
            cashHolding.setCurrentPrice(effectiveCurrent);
            repository.updateHoldingAfterTrade(cashHolding.getId(), cashHolding.getShares(), cashHolding.getPurchasePrice(), cashHolding.getCurrentPrice());

            usdChange = usdNeed.negate();
        } else {
            if (oldCashShares.compareTo(tradeShares) < 0) {
                throw new IllegalArgumentException("cash is not enough");
            }
            BigDecimal newCashShares = normalizeNearZero(oldCashShares.subtract(tradeShares));
            cashHolding.setShares(newCashShares);
            if (isEffectivelyZeroShares(newCashShares)) {
                repository.deleteById(cashHolding.getId());
            } else {
                repository.updateHoldingSharesAndPrice(cashHolding.getId(), cashHolding.getShares(), cashHolding.getCurrentPrice());
            }
            usdChange = tradeAmount.subtract(fee);
        }

        usdCashHolding.setShares(oldUsdShares.add(usdChange));
        repository.updateHoldingSharesAndPrice(usdCashHolding.getId(), usdCashHolding.getShares(), CASH_UNIT_PRICE);
        return usdChange;
    }

    private BigDecimal applyAssetTrade(Holding assetHolding,
                                       Holding cashHolding,
                                       String tradeType,
                                       BigDecimal tradeShares,
                                       BigDecimal tradePrice,
                                       BigDecimal tradeAmount,
                                       BigDecimal fee, LocalDate tradeDate) {
        BigDecimal oldAssetShares = safeValue(assetHolding.getShares());
        BigDecimal oldCashShares = safeValue(cashHolding.getShares());

        if ("BUY".equals(tradeType)) {
            BigDecimal cashNeedUsd = tradeAmount.add(fee);
            if (oldCashShares.compareTo(cashNeedUsd) < 0) {
                throw new IllegalArgumentException("cash is not enough for buy trade");
            }

            BigDecimal newAssetShares = oldAssetShares.add(tradeShares);
            BigDecimal avgCost = weightedAveragePrice(oldAssetShares, assetHolding.getPurchasePrice(), tradeShares, tradePrice, newAssetShares);
            assetHolding.setShares(newAssetShares);
            assetHolding.setPurchasePrice(avgCost);
            BigDecimal effectiveAssetPrice = tradeDate.equals(LocalDate.now())
                    ? tradePrice
                    : safeCurrentPrice(assetHolding.getCurrentPrice(), tradePrice);
            assetHolding.setCurrentPrice(effectiveAssetPrice);
            repository.updateHoldingAfterTrade(assetHolding.getId(), assetHolding.getShares(), assetHolding.getPurchasePrice(), assetHolding.getCurrentPrice());

            BigDecimal cashChangeUsd = cashNeedUsd.negate();
            cashHolding.setShares(oldCashShares.add(cashChangeUsd));
            BigDecimal effectiveCashPrice = tradeDate.equals(LocalDate.now())
                    ? CASH_UNIT_PRICE
                    : safeCurrentPrice(cashHolding.getCurrentPrice(), CASH_UNIT_PRICE);
            repository.updateHoldingSharesAndPrice(cashHolding.getId(), cashHolding.getShares(), effectiveCashPrice);
            return cashChangeUsd;
        }

        if (oldAssetShares.compareTo(tradeShares) < 0) {
            throw new IllegalArgumentException("asset shares are not enough for sell trade");
        }

        BigDecimal newAssetShares = normalizeNearZero(oldAssetShares.subtract(tradeShares));
        assetHolding.setShares(newAssetShares);
        if (isEffectivelyZeroShares(newAssetShares)) {
            repository.deleteById(assetHolding.getId());
        } else {
            repository.updateHoldingSharesAndPrice(assetHolding.getId(), assetHolding.getShares(), assetHolding.getCurrentPrice());
        }
        BigDecimal cashChangeUsd = tradeAmount.subtract(fee);
        cashHolding.setShares(oldCashShares.add(cashChangeUsd));
        repository.updateHoldingSharesAndPrice(cashHolding.getId(), cashHolding.getShares(), CASH_UNIT_PRICE);
        return cashChangeUsd;
    }

    private Holding ensureUsdCashHolding() {
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

    private Holding findHoldingForTrade(HoldingTradeRequest request, String symbol) {
        if (request.getHoldingId() == null) {
            throw new IllegalArgumentException("holdingId is required");
        }
        Holding holding;
        try {
            holding = repository.findById(request.getHoldingId());
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalArgumentException("holding not found by id: " + request.getHoldingId());
        }
        if (request.getCategoryId() != null && !Objects.equals(request.getCategoryId(), holding.getCategoryId())) {
            throw new IllegalArgumentException("categoryId does not match holding");
        }
        String normalizedHoldingSymbol = holding.getSymbol() == null ? "" : holding.getSymbol().trim().toUpperCase(Locale.ROOT);
        if (!normalizedHoldingSymbol.equals(symbol)) {
            throw new IllegalArgumentException("assetSymbol does not match holding");
        }
        return holding;
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

        if (isCashSymbol(symbol)) {
            trade.setCashAsset(true);
            trade.setAssetName(DEFAULT_COMPANY_NAMES.getOrDefault(symbol, symbol));
            trade.setAssetCategoryName(CASH_CATEGORY_NAME);
            trade.setCategoryId(CASH_CATEGORY_ID);
        } else {
            trade.setCashAsset(false);
            trade.setAssetName(assetHolding.getCompanyName());
            trade.setAssetCategoryName(assetHolding.getCategoryName());
            trade.setCategoryId(assetHolding.getCategoryId());
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

    private boolean isEffectivelyZeroShares(BigDecimal shares) {
        return safeValue(shares).abs().compareTo(SHARE_ZERO_TOLERANCE) <= 0;
    }

    private BigDecimal normalizeNearZero(BigDecimal shares) {
        return isEffectivelyZeroShares(shares) ? BigDecimal.ZERO : shares;
    }

    private BigDecimal weightedAveragePrice(BigDecimal oldShares,
                                            BigDecimal oldPrice,
                                            BigDecimal addedShares,
                                            BigDecimal addedPrice,
                                            BigDecimal totalShares) {
        BigDecimal safeOldShares = safeValue(oldShares);
        BigDecimal safeOldPrice = safeValue(oldPrice);
        BigDecimal oldCostAmount = safeOldShares.multiply(safeOldPrice);
        BigDecimal newCostAmount = addedShares.multiply(addedPrice);
        return oldCostAmount.add(newCostAmount).divide(totalShares, 6, RoundingMode.HALF_UP);
    }

    private Holding mergeHoldingOnAdd(Holding existing, Holding incoming) {
        BigDecimal oldShares = safeValue(existing.getShares());
        BigDecimal incomingShares = safeValue(incoming.getShares());
        BigDecimal newShares = oldShares.add(incomingShares);
        BigDecimal avgCost = weightedAveragePrice(oldShares, existing.getPurchasePrice(), incomingShares, incoming.getPurchasePrice(), newShares);

        existing.setShares(newShares);
        existing.setPurchasePrice(avgCost);
        existing.setCurrentPrice(safeCurrentPrice(incoming.getCurrentPrice(), existing.getCurrentPrice()));
        repository.updateHoldingAfterTrade(existing.getId(), existing.getShares(), existing.getPurchasePrice(), existing.getCurrentPrice());
        return existing;
    }

    private boolean isCashCategory(Integer categoryId) {
        return categoryId != null && categoryId == CASH_CATEGORY_ID;
    }

    private boolean isStockCategory(Integer categoryId) {
        return categoryId != null && categoryId == STOCK_CATEGORY_ID;
    }

    private boolean isCashSymbol(String symbol) {
        return symbol != null && SUPPORTED_CASH_SYMBOLS.contains(symbol.trim().toUpperCase(Locale.ROOT));
    }

    private String normalizeCashSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("cash symbol is required");
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CASH_SYMBOLS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported cash symbol: " + normalized);
        }
        return normalized;
    }

    private String currencyFromCashSymbol(String cashSymbol) {
        String normalized = normalizeCashSymbol(cashSymbol);
        return normalized.substring(0, normalized.indexOf("_CASH"));
    }

    private BigDecimal fetchCashUsdRate(String cashSymbol) throws IOException, InterruptedException {
        return fetchCashUsdRate(cashSymbol, null);
    }

    private BigDecimal fetchCashUsdRate(String cashSymbol, LocalDate rateDate) throws IOException, InterruptedException {
        String normalized = normalizeCashSymbol(cashSymbol);
        String currency = currencyFromCashSymbol(normalized);
        if ("USD".equals(currency)) {
            return CASH_UNIT_PRICE;
        }

        String encodedCurrency = URLEncoder.encode(currency, StandardCharsets.UTF_8);
        String url = rateDate == null
                ? String.format(Locale.ROOT, FX_RATE_API_LATEST, encodedCurrency)
                : String.format(Locale.ROOT, FX_RATE_API_BY_DATE, rateDate, encodedCurrency);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("FX HTTP " + response.statusCode());
        }

        Map<String, Object> root = jsonParser.parseMap(response.body());
        Object ratesObj = root.get("rates");
        if (!(ratesObj instanceof Map<?, ?> ratesMap)) {
            throw new IllegalStateException("missing rates");
        }
        Object usdObj = ratesMap.get("USD");
        if (usdObj == null) {
            throw new IllegalStateException("missing USD rate");
        }
        return new BigDecimal(String.valueOf(usdObj));
    }

    private void validateSymbolFormat(String symbol, String fieldName) {
        if (!SYMBOL_PATTERN.matcher(symbol).matches()) {
            throw new IllegalArgumentException(fieldName + " format is invalid");
        }
    }
}
