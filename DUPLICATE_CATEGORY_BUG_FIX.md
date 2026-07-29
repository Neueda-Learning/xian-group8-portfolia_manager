# 修复分类重复 Bug（忽略空格和大小写）

## 问题描述

### 现象
以下看起来只是空格或大小写不同的分类，本来都不应该重复添加：

- `bike`
- `bike `
- ` bike`
- `BIKE`
- `b i k e`
- `Real Estate`
- `realestate`

旧实现中，这些值可能被当成不同字符串处理，从而出现：
- 重复分类被插入成功
- 某些场景直接抛出 `500`，用户只能看到通用错误

---

## 根本原因

数据库里的 `UNIQUE` 约束只按原始字符串比较：

```sql
"bike" != "bike " != " bike" != "b i k e"
```

所以单纯依赖数据库原始 `UNIQUE`，无法表达业务上“忽略空格和大小写”的重复规则。

---

## 最终解决方案

当前实现使用三层策略：

1. **前端**：先 `trim()`，做空值校验
2. **后端 Service**：统一规范化名称
3. **Repository**：直接按规范化后的值查数据库，判断是否重复

---

## 1. 前端表单层
文件：`src/main/resources/static/categories.html`

```javascript
document.getElementById("addCategoryForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    clearError("error");

    const categoryNameInput = document.getElementById("categoryName").value.trim();
    const descriptionInput = document.getElementById("description").value.trim();

    if (!categoryNameInput) {
        showError("error", "Category name cannot be empty");
        return;
    }

    const category = {
        categoryName: categoryNameInput,
        description: descriptionInput
    };

    try {
        await Api.addCategory(category);
        event.target.reset();
        await loadCategories();
    } catch (e) {
        if (e.message && (e.message.includes("DUPLICATE_CATEGORY") || e.message.includes("Category already exists"))) {
            showError("error", "⚠️ This category already exists. Names that only differ by spaces or letter case are treated as duplicates.");
        } else if (e.message && (e.message.includes("VALIDATION_ERROR") || e.message.includes("cannot be empty"))) {
            showError("error", "⚠️ Category name cannot be empty");
        } else {
            showError("error", "Failed to add category: " + e.message);
        }
    }
});
```

作用：
- 前端提前过滤空输入
- 开始新操作时清理旧错误横幅
- 根据后端错误码显示不同提示

---

## 2. Service 层规范化
文件：`src/main/java/com/group8/portfolio_manager/service/AssetCategoryService.java`

```java
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
```

辅助方法：

```java
private String cleanText(String value) {
    return value == null ? null : value.trim();
}

private String normalizeCategoryName(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
}
```

作用：
- 先做首尾空格清理
- 再把名称转换成“去掉所有空白 + 全小写”的标准形式
- 防止 `rtf` / `rt f` / `R T F` 被当成不同分类

---

## 3. Repository 层查重
文件：`src/main/java/com/group8/portfolio_manager/repository/AssetCategoryRepository.java`

```java
public boolean existsByNormalizedName(String normalizedName) {
    String sql = "select count(*) from asset_category where lower(replace(category_name, ' ', '')) = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, normalizedName);
    return count != null && count > 0;
}
```

作用：
- 直接在数据库层按规范化后的名称查重
- 不是只把所有数据查出来再在内存里比较
- 更贴近真实数据源，也更稳定

---

## 4. Controller 层错误响应
文件：`src/main/java/com/group8/portfolio_manager/controller/AssetCategoryController.java`

```java
@PostMapping
public ResponseEntity<?> addCategory(@RequestBody AssetCategory category) {
    try {
        return ResponseEntity.ok(service.addCategory(category));
    } catch (IllegalArgumentException e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", e.getMessage());
        response.put("error", "VALIDATION_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    } catch (IllegalStateException e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", e.getMessage());
        response.put("error", "DUPLICATE_CATEGORY");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
```

作用：
- 空名称返回 `400 Bad Request`
- 重复分类返回 `409 Conflict`
- 不再把这些预期错误变成 `500`

---

## 重复规则现在是什么？

系统会把分类名转换成：

```text
去掉所有空白 + 转小写
```

所以这些值都会被视为同一个分类：

| 原始输入 | 规范化结果 |
|---------|-----------|
| `rtf` | `rtf` |
| `rt f` | `rtf` |
| `R T F` | `rtf` |
| ` RealEstate ` | `realestate` |
| `Real Estate` | `realestate` |
| `realestate` | `realestate` |

---

## 错误横幅清理

这次还顺手修了一个交互问题：

### 之前的问题
- 一次操作报错后，页面顶部 `error` 横幅会一直存在
- 即使后续操作已经成功，旧错误还挂在页面上

### 现在的处理
在 `api.js` 中新增：

```javascript
function clearError(containerId) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.textContent = "";
    el.style.display = "none";
}
```

并在以下时机调用：
- `loadCategories()` 成功后
- 新增分类开始前
- 删除分类开始前

这样旧错误提示会自动消失。

---

## 测试场景

### 应该拒绝
| 已存在 | 新输入 | 结果 |
|-------|-------|------|
| `rtf` | `rt f` | ❌ 重复 |
| `bike` | `BIKE` | ❌ 重复 |
| `Real Estate` | `realestate` | ❌ 重复 |
| `Bond Fund` | `bondfund` | ❌ 重复 |
| `Stock` | ` s t o c k ` | ❌ 重复 |

### 应该允许
| 已存在 | 新输入 | 结果 |
|-------|-------|------|
| `Stock` | `Bond` | ✅ 成功 |
| `Cash` | `ETF` | ✅ 成功 |

### 空值输入
| 输入 | 结果 |
|------|------|
| `"   "` | ❌ `VALIDATION_ERROR` |

---

## 用户看到的错误提示

### 重复分类
```text
⚠️ This category already exists. Names that only differ by spaces or letter case are treated as duplicates.
```

### 空名称
```text
⚠️ Category name cannot be empty
```

---

## 本次修复的价值

1. 解决了 `rtf` / `rt f` 这种逻辑重复问题
2. 解决了 `Real Estate` / `realestate` 这种大小写+空格问题
3. 预期错误不再返回 `500`
4. 前后端错误提示更清晰
5. 页面旧错误横幅不会一直残留

---

## 总结

这次最终版本的重复分类修复规则是：

- **忽略首尾空格**
- **忽略中间所有空白**
- **忽略大小写**
- **重复时返回 409 + DUPLICATE_CATEGORY**
- **成功后自动清理旧错误横幅**

这样既满足业务规则，也保证了用户体验。
