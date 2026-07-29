package com.group8.portfolio_manager.repository;

import com.group8.portfolio_manager.model.TradeRecordWide;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;

@Repository
public class TradeRecordWideRepository {
    private final JdbcTemplate jdbcTemplate;

    public TradeRecordWideRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long save(TradeRecordWide trade) {
        String sql = "insert into trade_record_wide (trade_no, asset_symbol, asset_name, asset_category_name, is_cash_asset, " +
                "trade_type_code, trade_type_name, trade_shares, buy_price, sell_price, trade_amount, fee, currency, " +
                "cash_asset_symbol, cash_change, trade_date, note) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, trade.getTradeNo());
            ps.setString(2, trade.getAssetSymbol());
            ps.setString(3, trade.getAssetName());
            ps.setString(4, trade.getAssetCategoryName());
            ps.setBoolean(5, Boolean.TRUE.equals(trade.getCashAsset()));
            ps.setString(6, trade.getTradeTypeCode());
            ps.setString(7, trade.getTradeTypeName());
            ps.setBigDecimal(8, trade.getTradeShares());
            ps.setBigDecimal(9, trade.getBuyPrice());
            ps.setBigDecimal(10, trade.getSellPrice());
            ps.setBigDecimal(11, trade.getTradeAmount());
            ps.setBigDecimal(12, trade.getFee());
            ps.setString(13, trade.getCurrency());
            ps.setString(14, trade.getCashAssetSymbol());
            ps.setBigDecimal(15, trade.getCashChange());
            ps.setDate(16, Date.valueOf(trade.getTradeDate()));
            ps.setString(17, trade.getNote());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? -1L : key.longValue();
    }

    public List<TradeRecordWide> findRecent(int limit) {
        String sql = "select * from trade_record_wide order by trade_date desc, id desc limit ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), limit);
    }

    private TradeRecordWide mapRow(ResultSet rs) throws SQLException {
        TradeRecordWide trade = new TradeRecordWide();
        trade.setId(rs.getLong("id"));
        trade.setTradeNo(rs.getString("trade_no"));
        trade.setAssetSymbol(rs.getString("asset_symbol"));
        trade.setAssetName(rs.getString("asset_name"));
        trade.setAssetCategoryName(rs.getString("asset_category_name"));
        trade.setCashAsset(rs.getBoolean("is_cash_asset"));
        trade.setTradeTypeCode(rs.getString("trade_type_code"));
        trade.setTradeTypeName(rs.getString("trade_type_name"));
        trade.setTradeShares(rs.getBigDecimal("trade_shares"));
        trade.setBuyPrice(rs.getBigDecimal("buy_price"));
        trade.setSellPrice(rs.getBigDecimal("sell_price"));
        trade.setTradeAmount(rs.getBigDecimal("trade_amount"));
        trade.setFee(rs.getBigDecimal("fee"));
        trade.setCurrency(rs.getString("currency"));
        trade.setCashAssetSymbol(rs.getString("cash_asset_symbol"));
        trade.setCashChange(rs.getBigDecimal("cash_change"));
        trade.setTradeDate(rs.getDate("trade_date").toLocalDate());
        Timestamp createdAt = rs.getTimestamp("created_at");
        trade.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        trade.setNote(rs.getString("note"));
        return trade;
    }
}

