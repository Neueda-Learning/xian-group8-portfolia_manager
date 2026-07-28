package com.group8.portfolio_manager.repository;

import com.group8.portfolio_manager.model.PriceHistory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PriceHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public PriceHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long total = jdbcTemplate.queryForObject("select count(*) from price_history", Long.class);
        return total == null ? 0L : total;
    }

    public List<PriceHistory> findBySymbol(String symbol) {
        String sql = "select * from price_history where symbol = ? order by price_time";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            PriceHistory p = new PriceHistory();
            p.setId(rs.getInt("id"));
            p.setSymbol(rs.getString("symbol"));
            p.setPriceTime(rs.getTimestamp("price_time").toLocalDateTime());
            p.setOpenPrice(rs.getBigDecimal("open_price"));
            p.setHighPrice(rs.getBigDecimal("high_price"));
            p.setLowPrice(rs.getBigDecimal("low_price"));
            p.setClosePrice(rs.getBigDecimal("close_price"));
            long volume = rs.getLong("volume");
            p.setVolume(rs.wasNull() ? null : volume);
            return p;
        }, symbol);
    }

    /**
     * Recomputes total portfolio value at every timestamp present in price_history,
     * by summing shares (from holdings) * close_price (from price_history) for each
     * holding's symbol, at each common timestamp. Skips timestamps where any held
     * symbol has no valid (non-null) close price, so the resulting curve is always complete.
     *
     * Returns an ordered map of timestamp -> total portfolio value.
     */
    public Map<Timestamp, BigDecimal> computePortfolioValueCurve() {
        String sql = """
                select ph.price_time as price_time,
                       sum(h.shares * ph.close_price) as total_value,
                       count(distinct h.symbol) as symbols_priced
                from price_history ph
                join holdings h on h.symbol = ph.symbol
                where ph.close_price is not null
                group by ph.price_time
                having symbols_priced = (select count(distinct symbol) from holdings)
                order by ph.price_time
                """;

        Map<Timestamp, BigDecimal> curve = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            curve.put(rs.getTimestamp("price_time"), rs.getBigDecimal("total_value"));
        });
        return curve;
    }
}

