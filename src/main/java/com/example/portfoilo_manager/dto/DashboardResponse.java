package com.example.portfoilo_manager.dto;

import java.math.BigDecimal;

/**
 * Response body for GET /api/dashboard.
 */
public class DashboardResponse {
    private BigDecimal totalValue;
    private double returnRate;
    private BigDecimal cash;
    private BigDecimal stocks;
    private BigDecimal bonds;
    private BigDecimal crypto;

    public DashboardResponse() {
    }

    public DashboardResponse(BigDecimal totalValue, double returnRate, BigDecimal cash,
                              BigDecimal stocks, BigDecimal bonds, BigDecimal crypto) {
        this.totalValue = totalValue;
        this.returnRate = returnRate;
        this.cash = cash;
        this.stocks = stocks;
        this.bonds = bonds;
        this.crypto = crypto;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public double getReturnRate() {
        return returnRate;
    }

    public void setReturnRate(double returnRate) {
        this.returnRate = returnRate;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public BigDecimal getStocks() {
        return stocks;
    }

    public void setStocks(BigDecimal stocks) {
        this.stocks = stocks;
    }

    public BigDecimal getBonds() {
        return bonds;
    }

    public void setBonds(BigDecimal bonds) {
        this.bonds = bonds;
    }

    public BigDecimal getCrypto() {
        return crypto;
    }

    public void setCrypto(BigDecimal crypto) {
        this.crypto = crypto;
    }
}

