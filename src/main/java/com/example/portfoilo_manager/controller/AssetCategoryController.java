package com.example.portfoilo_manager.controller;

import com.example.portfoilo_manager.model.AssetCategory;
import com.example.portfoilo_manager.service.AssetCategoryService;
import org.springframework.web.bind.annotation.*;

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
    public AssetCategory getCategoryById(@PathVariable int id) {
        return service.getCategoryById(id);
    }

    @PostMapping
    public AssetCategory addCategory(@RequestBody AssetCategory category) {
        return service.addCategory(category);
    }

    @DeleteMapping("/{id}")
    public String deleteCategory(@PathVariable int id) {
        return service.deleteCategory(id)
                ? "Category deleted successfully."
                : "Failed to delete category.";
    }
}

