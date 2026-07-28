package com.example.portfoilo_manager.service;

import com.example.portfoilo_manager.dto.PerformanceCurvePoint;
import com.example.portfoilo_manager.model.PortfolioHistory;
import com.example.portfoilo_manager.repository.PortfolioHistoryRepository;
import com.example.portfoilo_manager.repository.PriceHistoryRepository;
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

    /**
     * Fine-grained performance curve computed from raw intraday price_history ticks
     * (re-priced at every timestamp the market API provides), rather than the coarse
     * one-point-per-day portfolio_history table. Gives a much smoother "curve change graph".
     */
    public List<PerformanceCurvePoint> getCurve() {
        Map<Timestamp, BigDecimal> curve = priceHistoryRepository.computePortfolioValueCurve();

        List<PerformanceCurvePoint> points = new ArrayList<>();
        BigDecimal firstValue = null;
        for (Map.Entry<Timestamp, BigDecimal> entry : curve.entrySet()) {
            BigDecimal value = entry.getValue();
            if (firstValue == null) {
                firstValue = value;
            }
            BigDecimal returnRate = firstValue.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : value.subtract(firstValue).divide(firstValue, 4, RoundingMode.HALF_UP);
            points.add(new PerformanceCurvePoint(entry.getKey().toLocalDateTime(), value, returnRate));
        }
        return points;
    }
}


