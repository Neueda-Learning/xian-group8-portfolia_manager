# README需求与当前功能对照表（中文在前 / English after Chinese）

## 1) 说明 / Notes

- 本表基于 `Readme.md` 的核心目标与前端优先级需求整理。
- This matrix is based on the core goals and front-end priority requirements in `Readme.md`.
- 我已对当前项目代码做全量阅读（后端 controller/service/repository/model/dto、前端 static 页面与 js、SQL、测试文件）。
- I reviewed the full current codebase (backend controller/service/repository/model/dto, static front-end pages/js, SQL, and test files).

---

## 2) README必需功能对照 / README Required Features Matrix

| # | README需要的功能（中文） | README Required Feature (English) | 当前实现情况 | 主要代码证据 |
|---|---|---|---|---|
| 1 | 提供可保存与查询投资组合记录的 REST API | Provide a REST API to save and retrieve portfolio records | 已实现 / Implemented | `HoldingController.java`, `AssetCategoryController.java`, `DashboardController.java`, `PerformanceController.java`; `HoldingRepository.java`, `AssetCategoryRepository.java` |
| 2 | 浏览投资组合（前端最高优先级） | Browse a portfolio (front-end priority #1) | 已实现 / Implemented | `index.html`（Dashboard总览+Recent Holdings）, `holdings.html`（持仓列表） |
| 3 | 图形化查看组合表现（前端优先级） | View portfolio performance graphically | 已实现 / Implemented | `performance.html` + Chart.js；`PerformanceController.java` 的 `/api/performance/curve` |
| 4 | 向组合中添加项目（前端优先级） | Add items to the portfolio | 已实现 / Implemented | `holdings.html` 的 Add Holding 表单；`POST /api/holdings`；`HoldingService.addHolding()` |
| 5 | 从组合中移除项目（前端优先级） | Remove items from the portfolio | 已实现 / Implemented | `holdings.html` 删除按钮；`DELETE /api/holdings/{id}`；`HoldingService.deleteHolding()` |
| 6 | 使用数据库持久化存储 | Use a database for persistent storage | 已实现 / Implemented | `application.properties`（MySQL），`create_PM_sql.sql`（建表与样例数据） |
| 7 | （可选增强）考虑 AI / Quantum | (Stretch) Consider AI / Quantum | 暂未实现核心功能 / Not implemented as core features | `Readme.md` Appendix E 为探索项；当前代码未见 AI/Quantum endpoint |

---

## 3) 你们额外提供的功能（超出README最低要求）
## 3) Extra Features Delivered Beyond Minimum README Scope

| # | 额外功能（中文） | Extra Feature (English) | 价值 | 主要代码证据 |
|---|---|---|---|---|
| 1 | 资产类别管理（查看/新增/删除） | Asset category management (list/add/delete) | 把“持仓类别”独立成可维护字典 | `categories.html`, `AssetCategoryController.java`, `AssetCategoryService.java` |
| 2 | 删除类别前依赖检查（有持仓则阻止） | Category deletion guard when holdings still reference it | 防止破坏数据完整性 | `AssetCategoryService.deleteCategory()` 返回 `FAILED_HAS_HOLDINGS`；`AssetCategoryController` 返回 `409` + `HAS_HOLDINGS` |
| 3 | 分类去重增强：忽略空格+大小写 | Strong duplicate check: ignore whitespace + case | 解决 `rtf` vs `rt f`、`Real Estate` vs `realestate` | `AssetCategoryService.normalizeCategoryName()`；`AssetCategoryRepository.existsByNormalizedName()` |
| 4 | 结构化错误码（400/409/404）+ 前端识别 | Structured error responses + front-end error parsing | 错误可读、可分流处理，不再全是 500 | `AssetCategoryController.java`, `api.js` 错误解析逻辑 |
| 5 | 错误横幅自动清理机制 | Auto-clear stale error banner | 避免旧错误一直挂在页面 | `api.js` 的 `clearError()`；`categories.html` 在成功加载/新操作前调用 |
| 6 | Dashboard 总览增强（总值、收益、类别卡片、近期持仓） | Enhanced dashboard (total value, return, category cards, recent holdings) | 一屏查看核心指标 | `index.html`, `DashboardResponse.java`, `PortfolioService.getDashboardSummary()` |
| 7 | 资产配置图（Allocation Doughnut） | Asset allocation doughnut chart | 直观看到配置比例 | `index.html` + Chart.js；`GET /api/allocation`；`PortfolioService.getAssetAllocation()` |
| 8 | Performance 时间范围筛选（1M/3M/6M/1Y/MAX） | Performance range selector | 让图表可按时间尺度分析 | `performance.html` range selector；`PerformanceService.getPerformance()` |
| 9 | 细粒度收益曲线（基于 price_history 重算） | Fine-grained curve from `price_history` ticks | 比按天点位更平滑、信息量更高 | `/api/performance/curve`；`PriceHistoryRepository.computePortfolioValueCurve()` |
| 10 | Top Gainer / Top Loser 收益排行 | Top gainer / top loser ranking | 快速定位表现最好/最差资产 | `GET /api/performance/ranking`；`PortfolioService.getPerformanceRanking()` |
| 11 | 持仓价格刷新（调用样例行情API） | Refresh holding prices from sample market API | 一键更新当前价格与估值 | `POST /api/holdings/refresh-prices`；`HoldingService.refreshCurrentPrices()` |
| 12 | 历史价格序列查询接口 | Price series endpoint for a ticker | 为图表/分析功能留扩展接口 | `GET /api/holdings/price-series`；`HoldingService.getPriceSeries()` |
| 13 | 交易面板：买/卖/入金/出金 | Trade panel: buy/sell/deposit/withdraw | 从“静态持仓”升级到“交易驱动持仓变更” | `holdings.html` trade modal；`POST /api/holdings/trade`；`HoldingService.tradeHolding()` |
| 14 | 交易流水表（Recent Trades） | Recent trades ledger | 可审计、可回溯 | `GET /api/holdings/trades`；`TradeRecordWideRepository.java` |
| 15 | 导航与多页面结构（Dashboard/Holdings/Categories/Performance） | Multi-page navigation shell | 提升可用性与模块化 | `navbar.js` + 四个页面HTML |
| 16 | 现金资产自动兜底（USD_CASH） | Auto-ensure cash holding (`USD_CASH`) | 交易逻辑更稳，避免现金记录缺失 | `HoldingService.ensureCashHolding()` |

---

## 4) 你们这版的“小巧思”总结（中文先 / English after Chinese）

1. **先业务再技术的重复定义**：不是只靠数据库原始 `UNIQUE`，而是按“去空白+小写”做业务语义去重。  
   **Business-first duplicate semantics**: not just DB `UNIQUE`, but normalized comparison (strip whitespace + lowercase).

2. **把异常翻译成用户可理解语言**：`HAS_HOLDINGS`、`DUPLICATE_CATEGORY`、`VALIDATION_ERROR`。  
   **Translate exceptions into user-readable errors** via explicit error codes.

3. **防误操作与防连点**：删除确认框 + 删除按钮临时禁用。  
   **Guard against misclicks/double-submit** with confirm dialogs and temporary button disable.

4. **状态自愈式前端**：成功加载或新操作开始时清理旧错误提示。  
   **Self-healing UI state** by clearing stale error banners on new/successful actions.

5. **从“看数据”到“做交易”**：提供 trade API 与流水，形成更完整的投资组合管理闭环。  
   **From read-only holdings to actionable trading** with trade APIs and ledger history.

---

## 5) 可继续增强（可选）/ Optional Next Enhancements

- 增加 API 文档（Swagger/OpenAPI）。  
  Add API docs (Swagger/OpenAPI).
- 为关键服务补齐单元/集成测试（当前 `AssetCategoryServiceTest.java` 为空）。  
  Add unit/integration tests for key services (current `AssetCategoryServiceTest.java` is empty).
- 增加 AI/Quantum 探索型 endpoint 以满足 Appendix E stretch goal。  
  Add exploratory AI/Quantum endpoints for Appendix E stretch goals.
- 前端可加入成功提示与错误自动消失倒计时。  
  Add success toasts and auto-dismiss timers for errors.

