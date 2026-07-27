package com.example.product.controller;

import com.example.product.Repository.ProdRepository;
import com.example.product.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProdController {
    @Autowired
    private ProdRepository prodRepository;

    @GetMapping("/product/{id}")
    public Product findProductById(@PathVariable int id) {
        return prodRepository.findProduct(id);
    }
}
