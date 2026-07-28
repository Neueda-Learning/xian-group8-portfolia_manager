package com.group8.portfolio_manager.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioHistory {
    private int id;
    private LocalDate recordDate;
    private BigDecimal portfolioValue;
    private BigDecimal returnRate;

    public PortfolioHistory() {
    }

    public PortfolioHistory(int id, LocalDate recordDate, BigDecimal portfolioValue, BigDecimal returnRate) {
        this.id = id;
        this.recordDate = recordDate;
        this.portfolioValue = portfolioValue;
        this.returnRate = returnRate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
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

