package com.example.portfoilo_manager.dto;

/**
 * Response body for GET /api/performance/ranking.
 */
public class HoldingRankingResponse {
    private String topGainerSymbol;
    private double topGainerRate;
    private String topLoserSymbol;
    private double topLoserRate;

    public HoldingRankingResponse() {
    }

    public HoldingRankingResponse(String topGainerSymbol, double topGainerRate,
                                   String topLoserSymbol, double topLoserRate) {
        this.topGainerSymbol = topGainerSymbol;
        this.topGainerRate = topGainerRate;
        this.topLoserSymbol = topLoserSymbol;
        this.topLoserRate = topLoserRate;
    }

    public String getTopGainerSymbol() {
        return topGainerSymbol;
    }

    public void setTopGainerSymbol(String topGainerSymbol) {
        this.topGainerSymbol = topGainerSymbol;
    }

    public double getTopGainerRate() {
        return topGainerRate;
    }

    public void setTopGainerRate(double topGainerRate) {
        this.topGainerRate = topGainerRate;
    }

    public String getTopLoserSymbol() {
        return topLoserSymbol;
    }

    public void setTopLoserSymbol(String topLoserSymbol) {
        this.topLoserSymbol = topLoserSymbol;
    }

    public double getTopLoserRate() {
        return topLoserRate;
    }

    public void setTopLoserRate(double topLoserRate) {
        this.topLoserRate = topLoserRate;
    }
}

