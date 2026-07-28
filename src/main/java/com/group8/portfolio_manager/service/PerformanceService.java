package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.model.PortfolioHistory;
import com.group8.portfolio_manager.repository.PortfolioHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Supplies data for the Performance line chart (GET /api/performance),
 * supporting time range filters 1M / 3M / 6M / 1Y / MAX.
 */
@Service
public class PerformanceService {

    private final PortfolioHistoryRepository repository;

    public PerformanceService(PortfolioHistoryRepository repository) {
        this.repository = repository;
    }

    public List<PortfolioHistory> getPerformance(String range) {
        if (range == null || range.isBlank()) {
            return repository.findAll();
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = switch (range.toUpperCase()) {
            case "1M" -> today.minusMonths(1);
            case "3M" -> today.minusMonths(3);
            case "6M" -> today.minusMonths(6);
            case "1Y" -> today.minusYears(1);
            default -> null; // "MAX" or unrecognized -> full history
        };

        return startDate == null ? repository.findAll() : repository.findFrom(startDate);
    }
}

