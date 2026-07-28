package com.group8.portfolio_manager.controller;

import com.group8.portfolio_manager.dto.AssetCategoryOption;
import com.group8.portfolio_manager.service.AssetCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class AssetCategoryController {
    private final AssetCategoryService service;

    public AssetCategoryController(AssetCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<AssetCategoryOption> getAllCategories() {
        return service.getAllCategories();
    }
}
