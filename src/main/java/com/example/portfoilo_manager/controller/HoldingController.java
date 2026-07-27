package com.example.portfoilo_manager.controller;

import com.example.portfoilo_manager.model.Holding;
import com.example.portfoilo_manager.service.HoldingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return service.addHolding(holding);
    }

    @DeleteMapping("/{id}")
    public String deleteHolding(@PathVariable int id) {
        return service.deleteHolding(id)
                ? "Holding deleted successfully."
                : "Failed to delete holding.";
    }
}

