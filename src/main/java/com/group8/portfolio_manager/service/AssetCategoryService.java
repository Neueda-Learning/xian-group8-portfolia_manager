package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.model.AssetCategory;
import com.group8.portfolio_manager.repository.AssetCategoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
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
        String cleanedName = cleanText(category.getCategoryName());
        String cleanedDescription = cleanText(category.getDescription());

        category.setCategoryName(cleanedName);
        category.setDescription(cleanedDescription);

        if (cleanedName == null || cleanedName.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        String normalizedName = normalizeCategoryName(cleanedName);
        if (repository.existsByNormalizedName(normalizedName)) {
            throw new IllegalStateException("Category already exists");
        }

        int id;
        try {
            id = repository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Category already exists", e);
        }
        category.setId(id);
        return category;
    }

    private String cleanText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeCategoryName(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
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

