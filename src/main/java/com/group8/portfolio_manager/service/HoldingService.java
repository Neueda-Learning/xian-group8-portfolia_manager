package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.model.Holding;
import com.group8.portfolio_manager.repository.HoldingRepository;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class HoldingService {
    private static final String SAMPLE_PRICE_API = "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker=";
    private static final Set<String> SUPPORTED_TICKERS = Set.of("C", "AMZN", "TSLA", "FB", "AAPL");

    private final HoldingRepository repository;
    private final HttpClient httpClient;
    private final JsonParser jsonParser;

    public HoldingService(HoldingRepository repository) {
        this.repository = repository;
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
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestedSymbols", symbols);
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
        Object latest = closes.get(closes.size() - 1);
        return new BigDecimal(String.valueOf(latest));
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

        Map<String, Object> root = jsonParser.parseMap(response.body());
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
}
