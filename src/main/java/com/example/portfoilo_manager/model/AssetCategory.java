package com.example.portfoilo_manager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategory {
    private int id;
    private String categoryName;
    private String description;
}

