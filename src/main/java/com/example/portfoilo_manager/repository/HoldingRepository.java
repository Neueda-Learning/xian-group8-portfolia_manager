package com.example.portfoilo_manager.repository;

import com.example.portfoilo_manager.model.Holding;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Total market value grouped by asset category name, used for the asset allocation chart. */
    public Map<String, BigDecimal> sumValueByCategory() {
        String sql = "select c.category_name as category_name, " +
                "sum(h.shares * h.current_price) as total_value " +
                "from holdings h join asset_category c on h.category_id = c.id " +
                "group by c.category_name";
        return jdbcTemplate.query(sql, rs -> {
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            while (rs.next()) {
                result.put(rs.getString("category_name"), rs.getBigDecimal("total_value"));
            }
            return result;
        });
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

