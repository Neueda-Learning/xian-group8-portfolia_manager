package com.group8.portfolio_manager.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight test that hits the real sample price API (no Spring context / DB needed)
 * to confirm MarketDataService correctly extracts the latest close price.
 */
class MarketDataServiceLiveApiTest {

    @Test
    void extractsLatestCloseFromRealApiForKnownTicker() {
        MarketDataService service = new MarketDataService(
                RestClient.builder(),
                "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData"
        );

        Optional<BigDecimal> price = service.getLatestPrice("AAPL");

        assertThat(price).isPresent();
        assertThat(price.get()).isGreaterThan(BigDecimal.ZERO);
        System.out.println("AAPL latest price resolved by MarketDataService: " + price.get());
    }

    @Test
    void returnsEmptyForUnknownTicker() {
        MarketDataService service = new MarketDataService(
                RestClient.builder(),
                "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData"
        );

        Optional<BigDecimal> price = service.getLatestPrice("NOT_A_REAL_TICKER_XYZ");

        assertThat(price).isEmpty();
    }
}

