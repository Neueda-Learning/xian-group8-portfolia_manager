package com.group8.portfolio_manager.controller;

import com.group8.portfolio_manager.dto.HoldingTradeRequest;
import com.group8.portfolio_manager.model.Holding;
import com.group8.portfolio_manager.model.TradeRecordWide;
import com.group8.portfolio_manager.service.HoldingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

    private final HoldingService service;

    public HoldingController(HoldingService service) {
        this.service = service;
    }

    @GetMapping
    public List<Holding> getAllHoldings(@RequestParam(required = false) Integer categoryId) {
        try {
            return service.getAllHoldings(categoryId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @GetMapping("/{id}")
    public Holding getHoldingById(@PathVariable int id) {
        return service.getHoldingById(id);
    }

    @PostMapping
    public Holding addHolding(@RequestBody Holding holding) {
        try {
            return service.addHolding(holding);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PostMapping("/refresh-prices")
    public Map<String, Object> refreshPrices() {
        return service.refreshCurrentPrices();
    }

    @GetMapping("/price-series")
    public Map<String, Object> getPriceSeries(@RequestParam String ticker) {
        try {
            return service.getPriceSeries(ticker);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch sample price data", e);
        }
    }

    @GetMapping("/fx-rate")
    public Map<String, Object> getFxRate(@RequestParam String symbol,
                                         @RequestParam(required = false) LocalDate date) {
        try {
            return service.getCashFxRate(symbol, date);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch FX rate", e);
        }
    }

    @DeleteMapping("/{id}")
    public String deleteHolding(@PathVariable int id) {
        return service.deleteHolding(id)
                ? "Holding deleted successfully."
                : "Failed to delete holding.";
    }

    @PostMapping("/trade")
    public ResponseEntity<?> tradeHolding(@RequestBody HoldingTradeRequest request) {
        try {
            return ResponseEntity.ok(service.tradeHolding(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Bad Request",
                    "message", e.getMessage(),
                    "path", "/api/holdings/trade"
            ));
        }
    }

    @GetMapping("/trades")
    public List<TradeRecordWide> getRecentTrades(@RequestParam(defaultValue = "30") int limit) {
        return service.getRecentTrades(limit);
    }
}
