# 需求对照完成度清单 / Requirements vs Implementation Matrix

---

## 一、总体结论

| 维度 | 结论 |
|---|---|
| Readme.md 核心必需功能 | ✅ 全部完成 |
| group8-pm.md 团队自定功能 | ✅ 绝大部分完成，个别表结构被"实时计算"替代 |
| Readme.md Appendix E（AI/Quantum 拓展） | ❌ 未实现 |
| 额外交付（超出两份文档范围） | ✅ 大量交付（交易闭环、错误码体系、数据校验强化等） |

---

## 二、Readme.md 必需需求逐条核查

| # | 需求 | 状态 | 证据 |
|---|---|---|---|
| 1 | 构建 Portfolio Management REST API | ✅ 已完成 | `HoldingController`、`AssetCategoryController`、`DashboardController`、`PerformanceController` |
| 2 | 浏览组合（前端优先级#1） | ✅ 已完成 | `holdings.html` 展示完整持仓表；`index.html` 展示总览 |
| 3 | 图形化查看收益（前端优先级#2） | ✅ 已完成 | `performance.html` + Chart.js 折线图；`/api/performance`、`/api/performance/curve` |
| 4 | 添加持仓（前端优先级#3） | ✅ 已完成 | `holdings.html` Add Holding 表单；`POST /api/holdings` |
| 5 | 删除持仓（前端优先级#4） | ✅ 已完成 | `holdings.html` 删除按钮；`DELETE /api/holdings/{id}` |
| 6 | 使用数据库持久化 | ✅ 已完成 | MySQL，`create_PM_sql.sql` 建表脚本 |
| 7 | Git 版本管理 | ⚠️ 未在本次核查范围内 | 未检查 `.git` 历史/分支策略 |
| 8 | API 文档（Swagger/OpenAPI，可选） | ❌ 未实现 | `pom.xml` 未引入 springdoc/swagger 依赖 |
| 9 | Appendix E：AI 拓展 | ❌ 未实现 | 无 `/predictions`、`/optimize`、`/query` 等端点 |
| 10 | Appendix E：Quantum 拓展 | ❌ 未实现 | 无量子优化相关代码 |

---

## 三、group8-pm.md 团队自定需求逐条核查

### 3.1 Asset Category 模块（Person 2 负责）

| 功能 | 状态 | 证据 |
|---|---|---|
| 查看资产类别 | ✅ 已完成 | `GET /api/categories` |
| 添加类别 | ✅ 已完成 | `POST /api/categories`，含校验 |
| 删除类别 | ✅ 已完成 | `DELETE /api/categories/{id}` |
| Asset Allocation 饼图 | ✅ 已完成 | `GET /api/allocation`，`index.html` 展示 |
| **额外**：去重增强（大小写/空格归一化） | ✅ 额外完成 | `AssetCategoryService` |
| **额外**：删除保护（有持仓时阻止） | ✅ 额外完成 | 返回 `HAS_HOLDINGS` 409 |
| **额外**：结构化错误码（VALIDATION_ERROR/DUPLICATE_CATEGORY） | ✅ 额外完成 | `AssetCategoryController` |

### 3.2 Holding 模块（Person 1 负责）

| 功能 | 状态 | 证据 |
|---|---|---|
| 查看持仓 | ✅ 已完成 | `GET /api/holdings` |
| 添加持仓 | ✅ 已完成 | `POST /api/holdings`，当前价自动从行情API获取 |
| 删除持仓 | ✅ 已完成 | `DELETE /api/holdings/{id}` |
| **额外**：交易闭环 BUY/SELL/DEPOSIT/WITHDRAW | ✅ 额外完成 | `POST /api/holdings/trade` |
| **额外**：交易流水审计 | ✅ 额外完成 | `GET /api/holdings/trades`，`trade_record_wide` 表 |
| **额外**：价格刷新 | ✅ 额外完成 | `POST /api/holdings/refresh-prices` |
| **额外**：价格序列查询 | ✅ 额外完成 | `GET /api/holdings/price-series` |
| **额外**：现金 0.01 单位全链路统一 | ✅ 额外完成 | `HoldingService` CENT_FACTOR 逻辑 |
| **额外**：份额/价格/日期/Symbol 格式强校验 | ✅ 额外完成 | `HoldingService` 正则+范围校验 |

### 3.3 Dashboard 模块（Person 3 负责）

| 功能 | 状态 | 证据 |
|---|---|---|
| Total Portfolio Value | ✅ 已完成 | `GET /api/dashboard` |
| Total Return | ✅ 已完成 | `DashboardResponse` |
| Asset Allocation 联动 | ✅ 已完成 | `GET /api/allocation` |
| 底部资产卡片（Cash/Stock/Bond/Crypto） | ✅ 已完成 | `index.html` |
| Recent Holdings | ✅ 已完成 | `index.html` |
| `portfolio_summary` 独立持久化表 | ❌ 未按原设计实现 | 实际用 `PortfolioService` 服务层实时聚合替代，未建 `portfolio_summary` 表 |

### 3.4 Performance 模块（Person 4 负责）

| 功能 | 状态 | 证据 |
|---|---|---|
| 收益折线图 | ✅ 已完成 | `performance.html` + Chart.js |
| 时间范围筛选（1M/3M/6M/1Y/MAX） | ✅ 已完成 | `GET /api/performance?range=` |
| Top Gainer / Top Loser 排行 | ✅ 已完成 | `GET /api/performance/ranking` |
| `asset_performance` 独立持久化表 | ❌ 未按原设计实现 | 实际用 `PortfolioService.getPerformanceRanking()` 实时计算替代 |
| **额外**：细粒度收益曲线 | ✅ 额外完成 | `GET /api/performance/curve`，基于 `price_history` 逐笔重算 |

---

## 四、前端页面核查

| 页面 | 对应模块 | 状态 |
|---|---|---|
| `index.html` | Dashboard + Allocation | ✅ 已完成 |
| `holdings.html` | Holding + Trade | ✅ 已完成（含交易面板、数据校验、下拉选择） |
| `categories.html` | Category | ✅ 已完成 |
| `performance.html` | Performance | ✅ 已完成 |
| React 组件化架构（`pages/`、`components/`目录结构） | 原设计草案 | ❌ 未采用，当前为静态 HTML + 原生 JS |

---

## 五、数据库表核查（对照 `create_PM_sql.sql`）

| 设计表 | 状态 | 说明 |
|---|---|---|
| `asset_category` | ✅ 已建 | 与设计一致 |
| `holdings` | ✅ 已建 | 与设计一致 |
| `portfolio_history` | ✅ 已建 | 与设计一致（用于 `/api/performance`） |
| `price_history` | ✅ 已建（额外） | 设计文档未明确提及，属于额外增强，支撑细粒度曲线 |
| `trade_record_wide` | ✅ 已建（额外） | 设计文档未提及，属于额外增强，支撑交易闭环审计 |
| `portfolio_summary` | ❌ 未建 | 原计划表，实际由服务层实时计算替代 |
| `asset_performance` | ❌ 未建 | 原计划表，实际由服务层实时计算替代 |

---

## 六、测试覆盖核查

| 项 | 状态 | 说明 |
|---|---|---|
| `PortfolioManagerApplicationTests.java` | ✅ 存在 | 基础上下文加载测试 |
| `service/` 下单元测试 | ⚠️ 部分/不完整 | 需要补充针对 `HoldingService`、`AssetCategoryService` 的单元测试 |
| 交易闭环回归测试 | ❌ 未发现 | 建议补充 BUY/SELL/DEPOSIT/WITHDRAW 断言用例 |

---

## 七、汇总：已完成 / 未完成 / 额外完成

### ✅ 已完成（对照 Readme.md + group8-pm.md 基础需求）
1. Portfolio Management REST API（Holding/Category/Dashboard/Performance 全部端点）
2. 浏览组合、图形化收益、增删持仓（Readme 前端优先级 1-4 全部满足）
3. 数据库持久化（MySQL，5 张核心表）
4. 4 人模块分工全部落地，页面与接口一一对应
5. Asset Allocation 饼图、Dashboard 汇总卡片、Performance 折线图 + 排行

### ❌ 未完成 / 偏离原设计
1. Swagger/OpenAPI 接口文档（Readme 建议项，未实现）
2. Appendix E：AI 拓展功能（预测、自然语言查询等）—— 完全未实现
3. Appendix E：Quantum 拓展功能（组合优化算法等）—— 完全未实现
4. React 组件化前端架构（`pages/components/services` 目录结构）—— 当前是静态 HTML
5. `portfolio_summary` 独立持久化表 —— 用实时计算替代
6. `asset_performance` 独立持久化表 —— 用实时计算替代
7. 单元/集成测试覆盖不完整，尤其是交易闭环缺少自动化回归测试

### 🌟 额外完成（超出两份需求文档范围）
1. 完整交易生命周期：BUY / SELL / DEPOSIT / WITHDRAW（状态约束、现金与股票分离规则）
2. 交易流水审计表 `trade_record_wide`（可追溯每笔交易）
3. 现金 0.01 单位全链路统一（存储用分、展示用美元，前后端 SQL 三层一致）
4. 结构化错误码体系（VALIDATION_ERROR / DUPLICATE_CATEGORY / HAS_HOLDINGS 等）
5. 前后端双重数据校验强化（份额/价格/费用为正数、日期不可为未来、Symbol 格式正则校验、SELL 时校验持仓余量）
6. 细粒度收益曲线接口 `/api/performance/curve`（基于逐笔行情数据重算，比日级别更平滑）
7. 持仓价格一键刷新 `/api/holdings/refresh-prices`
8. 单 ticker 历史价格序列查询 `/api/holdings/price-series`
9. Stock 类别下拉选择 + 当前价格实时回显（前端体验优化）
10. Add Holding / Trade 面板前端表单校验（正数、非未来日期、持仓充足性检查、默认日期回填）

---

## 八、建议下一步优先级（按性价比排序）

1. **P0**：为 `HoldingService.tradeHolding()` 补充单元测试（BUY/SELL/DEPOSIT/WITHDRAW 四条主干路径 + 边界失败路径）
2. **P1**：引入 springdoc-openapi，生成 Swagger UI，满足 Readme 文档建议项
3. **P1**：为 Performance curve 空数据场景补充 fallback 逻辑（已在汇报材料中提出，代码层面待确认是否落地）
4. **P2**：视时间情况评估是否需要将 `portfolio_summary`/`asset_performance` 落地为真实表（当前实时计算已能满足功能，非强制）
5. **P3（可选加分）**：Appendix E AI/Quantum 拓展，可选取一个小切口做 POC（如简单规则库的"再平衡建议"）

---

## 附：中英文状态图例

| 图标 | 中文 | English |
|---|---|---|
| ✅ | 已完成 | Completed |
| ❌ | 未完成 | Not Completed |
| ⚠️ | 部分完成/待确认 | Partial / Needs Verification |
| 🌟 | 额外完成 | Extra Delivered |

