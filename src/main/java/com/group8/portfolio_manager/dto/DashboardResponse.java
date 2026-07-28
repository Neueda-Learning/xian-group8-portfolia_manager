package com.group8.portfolio_manager.dto;

import java.math.BigDecimal;

public class DashboardResponse {
    private BigDecimal totalValue;
    private Double returnRate;
    private BigDecimal cash;
    private BigDecimal stocks;
    private BigDecimal bonds;
    private BigDecimal crypto;

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public Double getReturnRate() {
        return returnRate;
    }

    public void setReturnRate(Double returnRate) {
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

