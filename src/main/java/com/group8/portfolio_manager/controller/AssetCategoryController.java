package com.group8.portfolio_manager.controller;

import com.group8.portfolio_manager.model.AssetCategory;
import com.group8.portfolio_manager.service.AssetCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return service.addCategory(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Integer id) {
        String result = service.deleteCategory(id);
        Map<String, String> response = new HashMap<>();
        
        if ("SUCCESS".equals(result)) {
            response.put("message", "Category deleted successfully.");
            return ResponseEntity.ok(response);
        } else if ("FAILED_HAS_HOLDINGS".equals(result)) {
            response.put("message", "Cannot delete category with associated holdings. Please delete all holdings in this category first.");
            response.put("error", "HAS_HOLDINGS");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } else if ("FAILED_NOT_FOUND".equals(result)) {
            response.put("message", "Category not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else {
            response.put("message", "Failed to delete category due to an unknown error.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

