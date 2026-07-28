package cn.lito.portfoilomanager.dto;

public class AssetCategoryOption {
    private int id;
    private String categoryName;

    public AssetCategoryOption() {
    }

    public AssetCategoryOption(int id, String categoryName) {
        this.id = id;
        this.categoryName = categoryName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
