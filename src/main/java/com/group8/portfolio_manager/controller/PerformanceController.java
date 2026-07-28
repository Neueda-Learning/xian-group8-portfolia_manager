package com.group8.portfolio_manager.controller;

import com.group8.portfolio_manager.dto.HoldingRankingResponse;
import com.group8.portfolio_manager.dto.PerformanceCurvePoint;
import com.group8.portfolio_manager.model.PortfolioHistory;
import com.group8.portfolio_manager.service.PerformanceService;
import com.group8.portfolio_manager.service.PortfolioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PortfolioService portfolioService;

    public PerformanceController(PerformanceService performanceService, PortfolioService portfolioService) {
        this.performanceService = performanceService;
        this.portfolioService = portfolioService;
    }

    /** e.g. GET /api/performance?range=1M|3M|6M|1Y|MAX (one point per day, from portfolio_history) */
    @GetMapping
    public List<PortfolioHistory> getPerformance(@RequestParam(required = false) String range) {
        return performanceService.getPerformance(range);
    }

    /**
     * Fine-grained performance curve (many points per day), computed on the fly from
     * every raw intraday tick stored in price_history. Powers the main chart on the
     * Performance page so it renders as a real curve, not just a handful of daily dots.
     */
    @GetMapping("/curve")
    public List<PerformanceCurvePoint> getCurve() {
        return performanceService.getCurve();
    }

    @GetMapping("/ranking")
    public HoldingRankingResponse getRanking() {
        return portfolioService.getPerformanceRanking();
    }
}

