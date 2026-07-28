package com.example.portfoilo_manager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single point on the fine-grained performance curve (GET /api/performance/curve).
 * Computed from price_history (raw API ticks) joined with holdings.shares, so the
 * portfolio value is re-priced at every timestamp the market API provides.
 */
public class PerformanceCurvePoint {
    private LocalDateTime time;
    private BigDecimal portfolioValue;
    private BigDecimal returnRate;

    public PerformanceCurvePoint() {
    }

    public PerformanceCurvePoint(LocalDateTime time, BigDecimal portfolioValue, BigDecimal returnRate) {
        this.time = time;
        this.portfolioValue = portfolioValue;
        this.returnRate = returnRate;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public BigDecimal getPortfolioValue() {
        return portfolioValue;
    }

    public void setPortfolioValue(BigDecimal portfolioValue) {
        this.portfolioValue = portfolioValue;
    }

    public BigDecimal getReturnRate() {
        return returnRate;
    }

    public void setReturnRate(BigDecimal returnRate) {
        this.returnRate = returnRate;
    }
}

