package com.example.portfoilo_manager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHistory {
    private int id;
    private LocalDate recordDate;
    private BigDecimal portfolioValue;
    private BigDecimal returnRate;
}

