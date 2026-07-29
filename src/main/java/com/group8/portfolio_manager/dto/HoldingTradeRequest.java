package com.group8.portfolio_manager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HoldingTradeRequest {
    private String assetSymbol;
    private String tradeTypeCode;
    private BigDecimal tradeShares;
    private BigDecimal tradePrice;
    private LocalDate tradeDate;
    private BigDecimal fee;
    private String note;
    private Integer categoryId;
    private Integer holdingId;

    public Integer getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(Integer holdingId) {
        this.holdingId = holdingId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getAssetSymbol() {
        return assetSymbol;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }

    public String getTradeTypeCode() {
        return tradeTypeCode;
    }

    public void setTradeTypeCode(String tradeTypeCode) {
        this.tradeTypeCode = tradeTypeCode;
    }

    public BigDecimal getTradeShares() {
        return tradeShares;
    }

    public void setTradeShares(BigDecimal tradeShares) {
        this.tradeShares = tradeShares;
    }

    public BigDecimal getTradePrice() {
        return tradePrice;
    }

    public void setTradePrice(BigDecimal tradePrice) {
        this.tradePrice = tradePrice;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

