package com.example.portfoilo_manager.service;

import com.example.portfoilo_manager.model.AssetCategory;
import com.example.portfoilo_manager.repository.AssetCategoryRepository;
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

    public AssetCategory getCategoryById(int id) {
        return repository.findById(id);
    }

    public AssetCategory addCategory(AssetCategory category) {
        int id = repository.save(category);
        category.setId(id);
        return category;
    }

    public boolean deleteCategory(int id) {
        return repository.deleteById(id) > 0;
    }
}

