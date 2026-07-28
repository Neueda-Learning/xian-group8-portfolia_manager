package cn.lito.portfoilomanager.service;

import cn.lito.portfoilomanager.dto.AssetCategoryOption;
import cn.lito.portfoilomanager.repository.AssetCategoryRepository;
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
