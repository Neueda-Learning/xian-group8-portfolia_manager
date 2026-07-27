package com.example.costomerservice.repo;

import com.example.costomerservice.custormer.Custormer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CustormerRepo {
    private final JdbcTemplate jdbcTemplate;

    public CustormerRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Custormer findCustormers(int id){
        String sql = "select * from custormerDetails where custid=?";
        return jdbcTemplate.queryForObject(sql , (rs , rowNum) ->
                new Custormer(
                        rs.getInt("custid"),
                        rs.getString("custname"),
                        rs.getString("email"),
                        rs.getInt("phonenum")
                ) , id);
    }

    public int save(Custormer custormer){
        String sql = "insert into custormerDetails value(?,?,?,?)";
        return jdbcTemplate.update(sql , custormer.getCustid() , custormer.getCustname() , custormer.getEmail() , custormer.getPhonenum());

    }




}
