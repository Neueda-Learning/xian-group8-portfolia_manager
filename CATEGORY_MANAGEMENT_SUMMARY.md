# Category Management 功能总结文档

## 1. 功能概述

本次主要完成的是 **Asset Category（资产分类）管理功能** 的一整套优化，包括：

- 分类列表展示
- 新增分类
- 删除分类
- 重复分类校验
- 删除前依赖检查（分类下仍有持仓时禁止删除）
- 统一错误提示
- 错误提示自动清理
- 前后端错误码联动

这次实现不只是“把功能做出来”，而是进一步考虑了：

- **用户体验**
- **错误提示是否清晰**
- **前后端职责是否明确**
- **业务规则是否真正落地**
- **边界情况是否被处理**

---

## 2. 主要功能

### 2.1 分类列表展示
文件：`src/main/resources/static/categories.html`

页面加载时，通过：

```javascript
loadCategories();
```

调用后端接口获取分类数据并渲染表格。

实现效果：
- 页面打开自动加载分类列表
- 没有分类时显示空状态提示
- 有分类时显示名称、描述和删除按钮

这部分让用户可以直观看到当前所有资产分类，是后续新增/删除功能的基础。

---

### 2.2 新增分类功能
文件：
- `src/main/resources/static/categories.html`
- `src/main/java/com/group8/portfolio_manager/controller/AssetCategoryController.java`
- `src/main/java/com/group8/portfolio_manager/service/AssetCategoryService.java`
- `src/main/java/com/group8/portfolio_manager/repository/AssetCategoryRepository.java`

用户在表单中输入分类名称和描述后，可以提交新增分类。

前端处理：
- 获取输入值
- `trim()` 去掉首尾空格
- 前端先判断是否为空
- 调用 API 提交数据

后端处理：
- 再次清理输入
- 检查名称是否为空
- 检查是否重复
- 保存到数据库
- 返回新增结果

实现效果：
- 正常分类可以成功添加
- 空名称会被拦截
- 重复名称会被拦截
- 成功后自动刷新列表
- 成功后自动清空表单

---

### 2.3 删除分类功能
文件：
- `src/main/resources/static/categories.html`
- `src/main/java/com/group8/portfolio_manager/controller/AssetCategoryController.java`
- `src/main/java/com/group8/portfolio_manager/service/AssetCategoryService.java`

用户点击 Delete 按钮可以删除分类。

实现流程：
1. 点击 Delete
2. 弹出确认框
3. 确认后调用后端删除接口
4. 删除成功则刷新列表
5. 删除失败则显示明确错误原因

实现效果：
- 防止误删（确认框）
- 删除成功后界面自动刷新
- 删除失败时提示友好，不会让用户一脸懵

---

### 2.4 删除前检查：分类下还有持仓时禁止删除
这是这次很重要的一个业务功能。

因为 `holdings.category_id` 依赖 `asset_category.id`，所以当某个分类下还有持仓时，不允许删除。

当前实现：
- 后端在删除失败时捕获外键约束异常
- Service 返回 `FAILED_HAS_HOLDINGS`
- Controller 转换成 `409 Conflict`
- 前端识别 `HAS_HOLDINGS`
- 页面显示友好提示

用户看到的提示大概是：

```text
Cannot delete category: This category still has holdings. Please delete all holdings in this category first.
```

这让错误从“技术报错”变成了“业务可理解提示”。

---

### 2.5 重复分类校验
这是本次最核心的改进之一。

#### 已解决的问题
系统现在不仅能拦住：
- `bike`
- `bike `
- ` bike`
- `BIKE`

还可以拦住：
- `rtf`
- `rt f`
- `R T F`
- `Real Estate`
- `realestate`

也就是说，系统现在会把“**只在空格和大小写上不同**”的输入当成同一个分类。

#### 后端规则
在 `AssetCategoryService.java` 中：

```java
private String normalizeCategoryName(String value) {
    return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
}
```

在 `AssetCategoryRepository.java` 中：

```java
public boolean existsByNormalizedName(String normalizedName) {
    String sql = "select count(*) from asset_category where lower(replace(category_name, ' ', '')) = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, normalizedName);
    return count != null && count > 0;
}
```

这意味着：
- 去掉所有空白
- 转成小写
- 再进行重复判断

这是一个非常实用的业务规范化处理。

---

## 3. 错误处理设计

### 3.1 后端结构化错误响应
Controller 不是直接把异常扔出去，而是转换成明确的 HTTP 响应：

#### 新增分类
- `400` + `VALIDATION_ERROR`：输入非法，例如空名称
- `409` + `DUPLICATE_CATEGORY`：重复分类

#### 删除分类
- `409` + `HAS_HOLDINGS`：分类下还有持仓
- `404`：分类不存在
- `500`：未知错误

这样的设计好处是：
- 前端能识别不同错误类型
- 错误含义明确
- 不会把所有问题都变成 500

---

### 3.2 前端统一错误展示
文件：`src/main/resources/static/js/api.js`

前端使用统一的 `request()` 方法解析 API 错误：

```javascript
throw new Error(`Request to ${path} failed (${response.status}): ${errorMessage}`);
```

页面再通过：

```javascript
showError("error", "...");
```

统一显示错误提示。

这比 `alert()` 更一致，也更适合页面型应用。

---

## 4. 这次实现里的“小巧思”

这是这次功能最值得写进总结的部分。

### 4.1 前后端双层校验
前端做了输入校验，但后端没有依赖前端，而是再次校验。

这体现了一个很好的设计原则：

> 永远不要完全信任客户端输入。

这样就算别人绕过页面直接调 API，后端也能保证数据质量。

---

### 4.2 重复判断不是只看原字符串
不是简单比较：

```text
"rtf" == "rt f"
```

而是先做业务规范化：
- 去空格
- 转小写
- 再比较

这说明实现时考虑了“业务意义上的重复”，不是只考虑“数据库层面的字符串是否完全一致”。

这个点很加分。

---

### 4.3 把重复判断下沉到 Repository 查询
不是把全部数据查出来再纯前端/纯内存判断，而是直接在数据库查询里做：

```java
select count(*) from asset_category where lower(replace(category_name, ' ', '')) = ?
```

这样做的好处：
- 更接近真实数据源
- 更稳定
- 更符合后端职责
- 对未来扩展也更友好

---

### 4.4 删除按钮禁用，防止重复点击
在删除操作中：

```javascript
const deleteBtn = event?.target;
if (deleteBtn) deleteBtn.disabled = true;
```

这个小处理非常实用。

作用：
- 防止用户连续点击多次 Delete
- 避免发送重复请求
- 避免界面状态错乱

这是典型的“看起来小，但用户体验差很多”的优化。

---

### 4.5 错误横幅自动清理
这是这次又一个很实用的小巧思。

之前问题：
- 一次报错后，顶部 error 横幅一直存在
- 即使后续操作成功，旧错误还挂着

现在新增了：

```javascript
function clearError(containerId) {
    const el = document.getElementById(containerId);
    if (!el) return;
    el.textContent = "";
    el.style.display = "none";
}
```

并在这些时机调用：
- 分类加载成功后
- 新增分类开始前
- 删除分类开始前

这个设计让页面状态更加自然，也说明你不是只关注“功能能不能跑”，还关注“功能跑起来是不是舒服”。

---

### 4.6 错误提示从技术语言变成用户语言
例如：
- 不直接把数据库异常抛给用户
- 而是提示“该分类还有持仓，请先删除持仓”
- 不直接提示 SQL 或异常堆栈
- 而是提示“分类已存在”

这是很好的“技术实现服务业务表达”的思路。

---

## 5. 当前分类管理功能的完整链路

### 新增分类链路
```text
用户输入
  ↓
前端 trim + 非空校验
  ↓
调用 /api/categories
  ↓
Controller 接收请求
  ↓
Service 清理输入 + 规范化名称
  ↓
Repository 按规范化名称查重
  ↓
保存数据库 / 返回错误
  ↓
前端刷新列表或显示错误
```

### 删除分类链路
```text
用户点击 Delete
  ↓
弹确认框
  ↓
禁用删除按钮
  ↓
调用 /api/categories/{id}
  ↓
Service 尝试删除
  ↓
如果有 holdings 外键引用 → 返回 HAS_HOLDINGS
  ↓
Controller 返回 409
  ↓
前端显示友好提示
```

---

## 6. 这次做出来的价值

### 从功能层面
你完成了一个完整可用的分类管理模块：
- 能看
- 能增
- 能删
- 能校验
- 能提示错误

### 从工程层面
你做的不只是“能跑”，而是：
- 前后端职责清晰
- 错误码规范
- 业务规则明确
- 用户体验更完整

### 从细节层面
你还补上了很多很容易被忽略、但非常加分的点：
- 防重复点击
- 自动清理旧错误
- 忽略空格和大小写的重复判断
- 明确区分 400 / 409 / 404 / 500

这些都属于老师/面试官/队友会特别愿意看到的“细节意识”。

---

## 7. 可以怎么介绍这部分功能

如果你要汇报/答辩/写 README，可以用下面这段：

> 我完成了资产分类管理模块的前后端联调，实现了分类的查询、新增和删除功能。
> 在此基础上，我重点优化了两个方面：
> 第一是重复分类校验，不仅能拦截普通重复，还能识别只在空格和大小写上不同的重复输入；
> 第二是删除约束处理，当分类下仍有关联持仓时，系统会返回友好的业务提示，而不是直接报错。
> 此外，我还统一了前端错误提示，并增加了错误横幅自动清理、防重复点击等细节优化，使整个功能在交互和健壮性上更完整。

---

## 8. 总结

这次你完成的不只是一个“分类页面”，而是一个比较完整的 **Category Management 功能模块**。

它的特点是：
- **功能完整**：列表、新增、删除都有
- **规则清晰**：重复判断、删除限制都明确
- **前后端协作好**：错误码和提示是打通的
- **细节做得好**：交互、提示、异常、边界都考虑到了

如果要用一句话总结：

> 这次的分类管理功能，不只是“能用”，而是已经开始体现出“业务规则 + 工程规范 + 用户体验”三者结合的思路。

