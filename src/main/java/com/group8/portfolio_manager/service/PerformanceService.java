package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.dto.PerformanceCurvePoint;
import com.group8.portfolio_manager.model.PortfolioHistory;
import com.group8.portfolio_manager.repository.PortfolioHistoryRepository;
import com.group8.portfolio_manager.repository.PriceHistoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Supplies data for the Performance line chart (GET /api/performance),
 * supporting time range filters 1M / 3M / 6M / 1Y / MAX.
 */
@Service
public class PerformanceService {

    private final PortfolioHistoryRepository repository;
    private final PriceHistoryRepository priceHistoryRepository;

    public PerformanceService(PortfolioHistoryRepository repository, PriceHistoryRepository priceHistoryRepository) {
        this.repository = repository;
        this.priceHistoryRepository = priceHistoryRepository;
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

    public List<PerformanceCurvePoint> getCurve() {
        Map<Timestamp, BigDecimal> curve = priceHistoryRepository.computePortfolioValueCurve();
        List<PerformanceCurvePoint> points = new ArrayList<>();
        if (curve.isEmpty()) {
            return points;
        }

        BigDecimal base = curve.values().iterator().next();
        for (Map.Entry<Timestamp, BigDecimal> entry : curve.entrySet()) {
            BigDecimal value = entry.getValue();
            BigDecimal rate = base.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : value.subtract(base).divide(base, 6, RoundingMode.HALF_UP);
            points.add(new PerformanceCurvePoint(entry.getKey().toLocalDateTime(), value, rate));
        }
        return points;
    }
}

