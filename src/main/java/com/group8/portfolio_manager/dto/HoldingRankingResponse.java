package com.group8.portfolio_manager.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HoldingRankingResponse {
    private String topGainerSymbol;
    private Double topGainerRate;
    private String topLoserSymbol;
    private Double topLoserRate;

    public HoldingRankingResponse() {
    }

    public HoldingRankingResponse(String topGainerSymbol, Double topGainerRate,
                                  String topLoserSymbol, Double topLoserRate) {
        this.topGainerSymbol = topGainerSymbol;
        this.topGainerRate = topGainerRate;
        this.topLoserSymbol = topLoserSymbol;
        this.topLoserRate = topLoserRate;
    }


}
