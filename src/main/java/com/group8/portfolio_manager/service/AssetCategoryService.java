package com.group8.portfolio_manager.service;

import com.group8.portfolio_manager.dto.AssetCategoryOption;
import com.group8.portfolio_manager.repository.AssetCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssetCategoryService {
    private final AssetCategoryRepository repository;

    public AssetCategoryService(AssetCategoryRepository repository) {
        this.repository = repository;
    }

    public List<AssetCategoryOption> getAllCategories() {
        return repository.findAllOptions();
    }
}
