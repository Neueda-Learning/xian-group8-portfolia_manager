package com.group8.portfolio_manager.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TradeRecordWide {
    private static final String CASH_SYMBOL = "USD_CASH";
    private static final BigDecimal CENT_FACTOR = new BigDecimal("100");

    private Long id;
    private String tradeNo;
    private String assetSymbol;
    private String assetName;
    private String assetCategoryName;
    private Boolean cashAsset;
    private String tradeTypeCode;
    private String tradeTypeName;
    private BigDecimal tradeShares;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private BigDecimal tradeAmount;
    private BigDecimal fee;
    private String currency;
    private String cashAssetSymbol;
    private BigDecimal cashChange;
    private LocalDate tradeDate;
    private String note;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetCategoryName() {
        return assetCategoryName;
    }

    public void setAssetCategoryName(String assetCategoryName) {
        this.assetCategoryName = assetCategoryName;
    }

    public Boolean getCashAsset() {
        return cashAsset;
    }

    public void setCashAsset(Boolean cashAsset) {
        this.cashAsset = cashAsset;
    }

    public String getTradeTypeCode() {
        return tradeTypeCode;
    }

    public void setTradeTypeCode(String tradeTypeCode) {
        this.tradeTypeCode = tradeTypeCode;
    }

    public String getTradeTypeName() {
        return tradeTypeName;
    }

    public void setTradeTypeName(String tradeTypeName) {
        this.tradeTypeName = tradeTypeName;
    }

    public BigDecimal getTradeShares() {
        return tradeShares;
    }

    public void setTradeShares(BigDecimal tradeShares) {
        this.tradeShares = tradeShares;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
    }

    public BigDecimal getTradeAmount() {
        return tradeAmount;
    }

    public void setTradeAmount(BigDecimal tradeAmount) {
        this.tradeAmount = tradeAmount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCashAssetSymbol() {
        return cashAssetSymbol;
    }

    public void setCashAssetSymbol(String cashAssetSymbol) {
        this.cashAssetSymbol = cashAssetSymbol;
    }

    public BigDecimal getCashChange() {
        return cashChange;
    }

    public void setCashChange(BigDecimal cashChange) {
        this.cashChange = cashChange;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getDisplayShares() {
        if (tradeShares == null) {
            return BigDecimal.ZERO;
        }
        if (CASH_SYMBOL.equalsIgnoreCase(assetSymbol)) {
            return tradeShares.divide(CENT_FACTOR, 2, java.math.RoundingMode.HALF_UP);
        }
        return tradeShares;
    }

    public BigDecimal getUnitPrice() {
        if (buyPrice != null) {
            return buyPrice;
        }
        if (sellPrice != null) {
            return sellPrice;
        }
        return BigDecimal.ZERO;
    }
}

