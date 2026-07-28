package com.group8.portfolio_manager.service;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MarketDataService {
    private final RestClient restClient;
    private final String endpoint;
    private final JsonParser jsonParser;

    public MarketDataService(RestClient.Builder builder, String endpoint) {
        this.restClient = builder.build();
        this.endpoint = endpoint;
        this.jsonParser = JsonParserFactory.getJsonParser();
    }

    public Optional<BigDecimal> getLatestPrice(String ticker) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(endpoint).queryParam("ticker", ticker).build())
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank()) {
                return Optional.empty();
            }

            Map<String, Object> root = jsonParser.parseMap(body);
            Object priceDataObj = root.get("price_data");
            if (!(priceDataObj instanceof Map<?, ?> priceDataMap)) {
                return Optional.empty();
            }
            Object closeObj = priceDataMap.get("close");
            if (!(closeObj instanceof List<?> closes) || closes.isEmpty()) {
                return Optional.empty();
            }
            Object latest = closes.get(closes.size() - 1);
            return Optional.of(new BigDecimal(String.valueOf(latest)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

