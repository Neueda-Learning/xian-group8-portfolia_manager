package com.group8.portfolio_manager.repository;

import com.group8.portfolio_manager.model.PortfolioHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Repository
public class PortfolioHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public PortfolioHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PortfolioHistory> findAll() {
        String sql = "select * from portfolio_history order by record_date";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new PortfolioHistory(
                        rs.getInt("id"),
                        rs.getDate("record_date").toLocalDate(),
                        rs.getBigDecimal("portfolio_value"),
                        rs.getBigDecimal("return_rate")
                ));
    }

    public List<PortfolioHistory> findFrom(LocalDate startDate) {
        String sql = "select * from portfolio_history where record_date >= ? order by record_date";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new PortfolioHistory(
                        rs.getInt("id"),
                        rs.getDate("record_date").toLocalDate(),
                        rs.getBigDecimal("portfolio_value"),
                        rs.getBigDecimal("return_rate")
                ), Date.valueOf(startDate));
    }
}

