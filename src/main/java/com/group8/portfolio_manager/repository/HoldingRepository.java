package com.group8.portfolio_manager.repository;

import com.group8.portfolio_manager.model.Holding;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

@Repository
public class HoldingRepository {

    private final JdbcTemplate jdbcTemplate;

    public HoldingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Holding> findAll() {
        String sql = "select h.*, c.category_name from holdings h " +
                "join asset_category c on h.category_id = c.id order by h.id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs));
    }

    public Holding findById(int id) {
        String sql = "select h.*, c.category_name from holdings h " +
                "join asset_category c on h.category_id = c.id where h.id=?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRow(rs), id);
    }

    public int save(Holding holding) {
        String sql = "insert into holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) " +
                "values (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, holding.getSymbol());
            ps.setString(2, holding.getCompanyName());
            ps.setInt(3, holding.getCategoryId());
            ps.setBigDecimal(4, holding.getShares());
            ps.setBigDecimal(5, holding.getPurchasePrice());
            ps.setBigDecimal(6, holding.getCurrentPrice());
            ps.setDate(7, Date.valueOf(holding.getPurchaseDate()));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.intValue() : -1;
    }

    public int deleteById(int id) {
        String sql = "delete from holdings where id=?";
        return jdbcTemplate.update(sql, id);
    }

    public int updateCurrentPriceBySymbol(String symbol, BigDecimal currentPrice) {
        String sql = "update holdings set current_price=? where symbol=?";
        return jdbcTemplate.update(sql, currentPrice, symbol);
    }

    private Holding mapRow(ResultSet rs) throws SQLException {
        Holding h = new Holding();
        h.setId(rs.getInt("id"));
        h.setSymbol(rs.getString("symbol"));
        h.setCompanyName(rs.getString("company_name"));
        h.setCategoryId(rs.getInt("category_id"));
        h.setCategoryName(rs.getString("category_name"));
        h.setShares(rs.getBigDecimal("shares"));
        h.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        h.setCurrentPrice(rs.getBigDecimal("current_price"));
        h.setPurchaseDate(rs.getDate("purchase_date").toLocalDate());
        return h;
    }
}
