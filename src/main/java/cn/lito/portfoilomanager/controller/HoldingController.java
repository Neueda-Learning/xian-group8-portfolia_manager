package cn.lito.portfoilomanager.controller;

import cn.lito.portfoilomanager.model.Holding;
import cn.lito.portfoilomanager.service.HoldingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public List<Holding> getAllHoldings() {
        return service.getAllHoldings();
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

    @DeleteMapping("/{id}")
    public String deleteHolding(@PathVariable int id) {
        return service.deleteHolding(id)
                ? "Holding deleted successfully."
                : "Failed to delete holding.";
    }
}
