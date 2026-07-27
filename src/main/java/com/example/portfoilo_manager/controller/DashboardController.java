package com.example.portfoilo_manager.controller;

import com.example.portfoilo_manager.dto.DashboardResponse;
import com.example.portfoilo_manager.service.PortfolioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DashboardController {

    private final PortfolioService portfolioService;

    public DashboardController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse getDashboard() {
        return portfolioService.getDashboardSummary();
    }

    @GetMapping("/api/allocation")
    public Map<String, Double> getAllocation() {
        return portfolioService.getAssetAllocation();
    }
}

