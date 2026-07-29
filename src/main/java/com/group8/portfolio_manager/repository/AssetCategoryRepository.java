package com.group8.portfolio_manager.repository;

import com.group8.portfolio_manager.model.AssetCategory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class AssetCategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssetCategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AssetCategory> findAll() {
        String sql = "select id, category_name, description from asset_category order by id";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new AssetCategory(
                        rs.getInt("id"),
                        rs.getString("category_name"),
                        rs.getString("description")
                ));
    }

    public AssetCategory findById(Integer id) {
        String sql = "select id, category_name, description from asset_category where id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new AssetCategory(
                        rs.getInt("id"),
                        rs.getString("category_name"),
                        rs.getString("description")
                ), id);
    }

    public int save(AssetCategory category) {
        String sql = "insert into asset_category (category_name, description) values (?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, category.getCategoryName());
            ps.setString(2, category.getDescription());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? -1 : key.intValue();
    }

    public int deleteById(Integer id) {
        String sql = "delete from asset_category where id = ?";
        return jdbcTemplate.update(sql, id);
    }

    public int deleteById(Integer id) {
        String sql = "delete from asset_category where id=?";
        return jdbcTemplate.update(sql, id);
    }


}

