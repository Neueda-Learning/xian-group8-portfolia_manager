package com.example.product.Repository;

import com.example.product.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProdRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Product findProduct(int id){
        String sql = "select * from product where prodid=?";
        return jdbcTemplate.queryForObject(sql , (rs , rowNum) ->
                new Product(
                        rs.getInt("prodid"),
                        rs.getString("prodname"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                ) , id);
    }
}
