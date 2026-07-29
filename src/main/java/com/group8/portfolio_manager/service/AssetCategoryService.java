package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.model.AssetCategory;
import com.group8.portfolio_manager.repository.AssetCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetCategoryService {

    private final AssetCategoryRepository repository;

    public AssetCategoryService(AssetCategoryRepository repository) {
        this.repository = repository;
    }

    public List<AssetCategory> getAllCategories() {
        return repository.findAll();
    }

    public AssetCategory getCategoryById(Integer id) {
        return repository.findById(id);
    }

    public AssetCategory addCategory(AssetCategory category) {
        // Trim whitespace to prevent duplicate categories like "bike" and "bike "
        if (category.getCategoryName() != null) {
            category.setCategoryName(category.getCategoryName().trim());
        }
        if (category.getDescription() != null) {
            category.setDescription(category.getDescription().trim());
        }
        
        // Validate category name is not empty after trimming
        if (category.getCategoryName() == null || category.getCategoryName().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        
        int id = repository.save(category);
        category.setId(id);
        return category;
    }

    public String deleteCategory(Integer id) {
        try {
            int deleted = repository.deleteById(id);
            if (deleted > 0) {
                return "SUCCESS";
            }
            return "FAILED_NOT_FOUND";
        } catch (Exception e) {
            // 外键约束异常 - 分类还有关联的持仓
            if (e.getMessage() != null && e.getMessage().contains("foreign key")) {
                return "FAILED_HAS_HOLDINGS";
            }
            return "FAILED_UNKNOWN";
        }
    }
}

