package com.group8.portfolio_manager.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Holding {
    private Integer id;
    private String symbol;
    private String companyName;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal shares;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private LocalDate purchaseDate;

    public Holding() {
    }

    public Holding(Integer id, String symbol, String companyName, Integer categoryId, String categoryName,
                   BigDecimal shares, BigDecimal purchasePrice, BigDecimal currentPrice, LocalDate purchaseDate) {
        this.id = id;
        this.symbol = symbol;
        this.companyName = companyName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.shares = shares;
        this.purchasePrice = purchasePrice;
        this.currentPrice = currentPrice;
        this.purchaseDate = purchaseDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getShares() {
        return shares;
    }

    public void setShares(BigDecimal shares) {
        this.shares = shares;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    /** Current market value = shares * currentPrice. */
    public BigDecimal getMarketValue() {
        if (shares == null || currentPrice == null) {
            return BigDecimal.ZERO;
        }
        return shares.multiply(currentPrice);
    }

    /** Absolute profit/loss = (currentPrice - purchasePrice) * shares. */
    public BigDecimal getProfitLoss() {
        if (shares == null || currentPrice == null || purchasePrice == null) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(purchasePrice).multiply(shares);
    }

    /** Profit rate as decimal, e.g. 0.15 = 15%. */
    public double getProfitRate() {
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) == 0 || currentPrice == null) {
            return 0.0;
        }
        return currentPrice.subtract(purchasePrice)
                .divide(purchasePrice, 6, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

}
