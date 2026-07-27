package com.example.portfoilo_manager.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Response body for GET /api/dashboard.
 */
@Data
public class DashboardResponse {
    private BigDecimal totalValue;
    private double returnRate;
    private BigDecimal cash;
    private BigDecimal stocks;
    private BigDecimal bonds;
    private BigDecimal crypto;
}

