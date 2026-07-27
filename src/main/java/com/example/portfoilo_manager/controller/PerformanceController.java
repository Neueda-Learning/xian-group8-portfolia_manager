package com.example.portfoilo_manager.controller;

import com.example.portfoilo_manager.dto.HoldingRankingResponse;
import com.example.portfoilo_manager.model.PortfolioHistory;
import com.example.portfoilo_manager.service.PerformanceService;
import com.example.portfoilo_manager.service.PortfolioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PortfolioService portfolioService;

    public PerformanceController(PerformanceService performanceService, PortfolioService portfolioService) {
        this.performanceService = performanceService;
        this.portfolioService = portfolioService;
    }

    /** e.g. GET /api/performance?range=1M|3M|6M|1Y|MAX */
    @GetMapping("/api/performance")
    public List<PortfolioHistory> getPerformance(@RequestParam(required = false) String range) {
        return performanceService.getPerformance(range);
    }

    @GetMapping("/api/performance/ranking")
    public HoldingRankingResponse getRanking() {
        return portfolioService.getPerformanceRanking();
    }
}

