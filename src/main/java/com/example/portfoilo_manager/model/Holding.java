package com.example.portfoilo_manager.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class Holding {
    private int id;
    private String symbol;
    private String companyName;
    private int categoryId;
    private String categoryName; // populated via join with asset_category, not persisted directly
    private BigDecimal shares;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private LocalDate purchaseDate;

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

    /** Profit rate as a decimal, e.g. 0.25 = +25%. */
    public double getProfitRate() {
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) == 0 || currentPrice == null) {
            return 0.0;
        }
        return currentPrice.subtract(purchasePrice)
                .divide(purchasePrice, 6, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

