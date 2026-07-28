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
        if (category == null || category.getCategoryName() == null || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("categoryName is required");
        }
        int id = repository.save(category);
        category.setId(id);
        return category;
    }

    public boolean deleteCategory(Integer id) {
        return repository.deleteById(id) > 0;
    }
}
