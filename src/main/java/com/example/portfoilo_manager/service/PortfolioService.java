package com.example.portfoilo_manager.service;

import com.example.portfoilo_manager.dto.DashboardResponse;
import com.example.portfoilo_manager.dto.HoldingRankingResponse;
import com.example.portfoilo_manager.model.Holding;
import com.example.portfoilo_manager.repository.HoldingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates data across holdings/asset categories for the Dashboard and
 * Asset Allocation endpoints (see group8-pm.md, Person 2 & 3 responsibilities).
 */
@Service
public class PortfolioService {

    private final HoldingRepository holdingRepository;

    public PortfolioService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    public DashboardResponse getDashboardSummary() {
        List<Holding> holdings = holdingRepository.findAll();

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryValues = new LinkedHashMap<>();

        for (Holding h : holdings) {
            BigDecimal marketValue = h.getMarketValue();
            BigDecimal cost = h.getShares().multiply(h.getPurchasePrice());
            totalValue = totalValue.add(marketValue);
            totalCost = totalCost.add(cost);

            String category = h.getCategoryName() == null ? "" : h.getCategoryName();
            if (!category.isBlank()) {
                categoryValues.merge(category, marketValue, BigDecimal::add);
            }
        }

        double returnRate = totalCost.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : totalValue.subtract(totalCost)
                .divide(totalCost, 6, RoundingMode.HALF_UP)
                .doubleValue();

        DashboardResponse response = new DashboardResponse();
        response.setTotalValue(totalValue);
        response.setReturnRate(returnRate);
        response.setCash(categoryValues.getOrDefault("Cash", BigDecimal.ZERO));
        response.setStocks(categoryValues.getOrDefault("Stock", BigDecimal.ZERO));
        response.setBonds(categoryValues.getOrDefault("Bond", BigDecimal.ZERO));
        response.setCrypto(categoryValues.getOrDefault("Cryptocurrency", BigDecimal.ZERO));
        response.setCategoryValues(categoryValues);
        return response;
    }

    /** Returns each asset category's share of total portfolio value as a percentage (0-100). */
    public Map<String, Double> getAssetAllocation() {
        Map<String, BigDecimal> valueByCategory = holdingRepository.sumValueByCategory();

        BigDecimal total = valueByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Double> allocation = new LinkedHashMap<>();
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return allocation;
        }
        for (Map.Entry<String, BigDecimal> entry : valueByCategory.entrySet()) {
            double percentage = entry.getValue()
                    .divide(total, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            allocation.put(entry.getKey(), percentage);
        }
        return allocation;
    }

    /** Best/worst performing holdings by profit rate, for the Performance page ranking card. */
    public HoldingRankingResponse getPerformanceRanking() {
        List<Holding> holdings = holdingRepository.findAll();

        Holding topGainer = holdings.stream()
                .max(Comparator.comparingDouble(Holding::getProfitRate))
                .orElse(null);
        Holding topLoser = holdings.stream()
                .min(Comparator.comparingDouble(Holding::getProfitRate))
                .orElse(null);

        HoldingRankingResponse response = new HoldingRankingResponse();
        if (topGainer != null) {
            response.setTopGainerSymbol(topGainer.getSymbol());
            response.setTopGainerRate(topGainer.getProfitRate());
        }
        if (topLoser != null) {
            response.setTopLoserSymbol(topLoser.getSymbol());
            response.setTopLoserRate(topLoser.getProfitRate());
        }
        return response;
    }
}

