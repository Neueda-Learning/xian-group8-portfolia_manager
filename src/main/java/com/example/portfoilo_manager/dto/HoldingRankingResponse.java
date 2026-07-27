package com.example.portfoilo_manager.dto;

import lombok.Data;

/**
 * Response body for GET /api/performance/ranking.
 */
@Data
public class HoldingRankingResponse {
    private String topGainerSymbol;
    private double topGainerRate;
    private String topLoserSymbol;
    private double topLoserRate;
}

