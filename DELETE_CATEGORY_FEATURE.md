# 删除资产分类功能说明文档

## 功能概述
用户可以删除资产分类，系统会先确认删除，再由后端判断该分类是否仍被持仓引用。

- 没有持仓引用：删除成功，刷新列表
- 仍有持仓引用：返回友好错误提示
- 如果页面之前出现过错误，后续成功加载或重新操作时会自动清除旧的错误横幅

---

## 关联关系

`holdings.category_id` 是指向 `asset_category.id` 的外键，因此正在被持仓使用的分类不能直接删除。

```text
asset_category
    ↑
    │ category_id (FK)
    │
holdings
```

---

## 当前实现

### 1. Service 层
文件：`src/main/java/com/group8/portfolio_manager/service/AssetCategoryService.java`

```java
public String deleteCategory(Integer id) {
    try {
        int deleted = repository.deleteById(id);
        if (deleted > 0) {
            return "SUCCESS";
        }
        return "FAILED_NOT_FOUND";
    } catch (Exception e) {
        if (e.getMessage() != null && e.getMessage().contains("foreign key")) {
            return "FAILED_HAS_HOLDINGS";
        }
        return "FAILED_UNKNOWN";
    }
}
```

作用：
- 删除成功返回 `SUCCESS`
- 如果被持仓引用，返回 `FAILED_HAS_HOLDINGS`
- 如果分类不存在，返回 `FAILED_NOT_FOUND`

---

### 2. Controller 层
文件：`src/main/java/com/group8/portfolio_manager/controller/AssetCategoryController.java`

```java
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
```

作用：
- 把 Service 返回值转换成标准 HTTP 响应
- 前端可以根据 `error` 字段识别特定错误

---

### 3. API 层
文件：`src/main/resources/static/js/api.js`

```javascript
if (!response.ok) {
    const contentType = response.headers.get("content-type") || "";
    let errorMessage = "";
    let errorData = {};

    try {
        if (contentType.includes("application/json")) {
            errorData = await response.json();
            errorMessage = errorData.message || "Unknown error";
            if (errorData.error) {
                errorMessage += ` [${errorData.error}]`;
            }
        } else {
            errorMessage = await response.text().catch(() => "");
        }
    } catch (e) {
        errorMessage = "Failed to parse error response";
    }

    throw new Error(`Request to ${path} failed (${response.status}): ${errorMessage}`);
}
```

作用：
- 统一解析后端 JSON 错误
- 把 `HAS_HOLDINGS` 等错误码带给页面逻辑

---

### 4. 页面交互层
文件：`src/main/resources/static/categories.html`

```javascript
async function deleteCategory(id) {
    if (!confirm("Delete this asset category?")) return;

    const deleteBtn = event?.target;
    if (deleteBtn) deleteBtn.disabled = true;
    clearError("error");

    try {
        await Api.deleteCategory(id);
        await loadCategories();
    } catch (e) {
        if (e.message && (e.message.includes("HAS_HOLDINGS") || e.message.includes("cannot delete category"))) {
            showError("error", "⚠️ Cannot delete category: This category still has holdings. Please delete all holdings in this category first.");
        } else {
            showError("error", "Failed to delete category: " + e.message);
        }
        if (deleteBtn) deleteBtn.disabled = false;
    }
}
```

作用：
- 删除前弹确认框
- 删除时禁用按钮，防止重复点击
- 开始新操作前先清除旧错误
- 删除失败时显示友好提示

---

## 错误横幅清理

文件：`src/main/resources/static/js/api.js`

```javascript
function clearError(containerId) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.textContent = "";
    el.style.display = "none";
}
```

调用时机：
- `loadCategories()` 成功后
- `deleteCategory()` 开始时
- 新增分类提交开始时

这样可以避免：
- 上一次删除失败后，错误横幅一直挂在页面上
- 后面操作已经成功了，但页面还保留旧错误信息

---

## HTTP 响应设计

| 场景 | 状态码 | error | message |
|------|--------|-------|---------|
| 删除成功 | `200` | — | `Category deleted successfully.` |
| 分类仍有关联持仓 | `409` | `HAS_HOLDINGS` | `Cannot delete category with associated holdings...` |
| 分类不存在 | `404` | — | `Category not found.` |
| 未知错误 | `500` | — | `Failed to delete category due to an unknown error.` |

---

## 用户看到的效果

### 情况 1：删除成功
1. 点击 Delete
2. 确认删除
3. 列表刷新
4. 如果之前有错误横幅，会自动消失

### 情况 2：分类仍有持仓
1. 点击 Delete
2. 确认删除
3. 后端返回 `409 + HAS_HOLDINGS`
4. 页面显示：

```text
⚠️ Cannot delete category: This category still has holdings. Please delete all holdings in this category first.
```

### 情况 3：上一次报错，这次成功
1. 上一次操作失败，页面出现错误横幅
2. 用户再次执行成功操作
3. 页面自动 `clearError("error")`
4. 旧错误横幅被清掉

---

## 总结

删除功能现在具备：
- 明确的后端错误分类
- 规范的 HTTP 状态码
- 前端友好的错误提示
- 防重复点击
- 成功后自动清理旧错误横幅
