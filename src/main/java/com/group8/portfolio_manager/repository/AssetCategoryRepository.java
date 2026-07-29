package com.group8.portfolio_manager.repository;

import com.group8.portfolio_manager.dto.AssetCategoryOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AssetCategoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public AssetCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AssetCategoryOption> findAllOptions() {
        String sql = "select id, category_name from asset_category order by id";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new AssetCategoryOption(rs.getInt("id"), rs.getString("category_name")));
    }
}
