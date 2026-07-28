package com.group8.portfolio_manager.controller;

import com.group8.portfolio_manager.model.AssetCategory;
import com.group8.portfolio_manager.service.AssetCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class AssetCategoryController {
    private final AssetCategoryService service;

    public AssetCategoryController(AssetCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<AssetCategory> getAllCategories() {
        return service.getAllCategories();
    }

    @GetMapping("/{id}")
    public AssetCategory getCategoryById(@PathVariable Integer id) {
        return service.getCategoryById(id);
    }

    @PostMapping
    public AssetCategory addCategory(@RequestBody AssetCategory category) {
        try {
            return service.addCategory(category);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @DeleteMapping("/{id}")
    public String deleteCategory(@PathVariable Integer id) {
        return service.deleteCategory(id)
                ? "Category deleted successfully."
                : "Failed to delete category.";
    }
}
