按照这个最终效果反推：

> **用户打开系统 → 看到资产总览 → 查看资产配置 → 查看收益趋势 → 查看持仓 → 添加/删除资产 → 查看详细信息**

我先给你完整设计：

1. 最终需要有哪些功能
2. 数据库怎么设计
3. 后端怎么设计
4. 前端怎么设计
5. 四个人怎么分工

------

# 一、最终实现功能总览

## 1. Asset（资产类别）管理

注意：

这里的 Asset 指：

```
Stock
Bond
Cash
Crypto
```

不是 AAPL、TSLA。

------

功能：

### 查看资产类别

例如：

```
Stock

Bond

Crypto

Cash
```

------

### 添加 Asset

按钮：

```
+ Add Asset
```

例如：

新增：

```
Real Estate
```

------

### 删除 Asset

删除某个资产类别。

------

# 2. Holding（具体持仓）管理

Holding 是用户真正拥有的东西：

例如：

```
AAPL
TSLA
AMZN
BTC
```

------

功能：

## 查看持仓

页面：

```
Your Holdings
```

展示：

| Symbol | Company | Shares | Price | Value | P/L   |
| ------ | ------- | ------ | ----- | ----- | ----- |
| AAPL   | Apple   | 35     | 145   | 5086  | +886  |
| TSLA   | Tesla   | 20     | 720   | 14403 | +1402 |

------

## Add Holding

添加：

例如：

```
Symbol:
AAPL

Company:
Apple

Shares:
35

Purchase Price:
120
```

------

## Delete Holding

删除：

例如：

```
TSLA
```

------

# 3. Dashboard 首页

对应截图：

------

## Total Portfolio Value

显示：

```
Total Portfolio Value

$152,430.25
```

计算：

所有 Holding 当前价值之和。

公式：

```
数量 × 当前价格
```

------

## Total Return

显示：

```
Total Return

+12.8%
```

计算：

```
(当前价值 - 成本)
/
成本
```

------

## Asset Allocation

资产配置饼图：

例如：

```
Stocks 40%

Bonds 20%

Crypto 15%

Cash 25%
```

来源：

根据 Holding 分类统计。

例如：

Holding：

```
AAPL
TSLA
BTC
Cash
```

统计：

```
Stock Value

Bond Value

Crypto Value

Cash Balance
```

然后计算比例。

------

## 底部资产卡片

展示：

```
Cash Balance

$5200
Stocks Value

$110900
Bonds Value

$25330
Crypto Value

$11000
```

------

# 4. Performance 收益趋势

对应截图：

```
Portfolio Performance
```

------

功能：

## 收益折线图

展示：

过去时间：

```
Apr

May

Jun

Jul
```

资产变化：

例如：

```
120000

130000

140000

150000
```

------

## 时间范围筛选

增加：

```
1M

3M

6M

1Y

MAX
```

其中：

MAX：

表示查看全部历史数据。

------

## 收益分析小功能

增加：

### Top Gainer

最高收益资产：

例如：

```
AAPL

+25%
```

------

### Top Loser

最低收益资产：

例如：

```
TSLA

-8%
```

------

# 二、数据库设计总结

总共主要表：

------

# 1. asset_category

保存资产类别。

例如：

```
Stock

Bond

Crypto

Cash
```

字段：

| 字段          | 说明     |
| ------------- | -------- |
| id            | 主键     |
| category_name | 类别名称 |
| description   | 描述     |

------

# 2. holdings

保存用户持仓。

例如：

```
AAPL
TSLA
BTC
```

字段：

| 字段           | 说明     |
| -------------- | -------- |
| id             | 主键     |
| symbol         | 代码     |
| company_name   | 公司     |
| category_id    | 资产类别 |
| shares         | 数量     |
| purchase_price | 买入价格 |
| current_price  | 当前价格 |
| purchase_date  | 购买日期 |

------

# 3. portfolio_summary

保存首页统计数据。

字段：

| 字段         | 说明         |
| ------------ | ------------ |
| id           | 主键         |
| total_value  | 总资产       |
| total_return | 收益率       |
| cash_value   | 现金         |
| stock_value  | 股票价值     |
| bond_value   | 债券价值     |
| crypto_value | 加密货币价值 |

------

# 4. portfolio_history

保存历史收益。

字段：

| 字段            | 说明     |
| --------------- | -------- |
| id              | 主键     |
| date            | 日期     |
| portfolio_value | 组合价值 |
| return_rate     | 收益率   |

------

# 5. asset_performance

保存收益排行。

字段：

| 字段        | 说明     |
| ----------- | -------- |
| id          | 主键     |
| symbol      | 资产代码 |
| profit_rate | 收益率   |

------

# 三、后端实现（Spring Boot）

结构：

```
backend

├── controller
├── service
├── repository
├── entity
└── dto
```

------

# Controller接口

## Asset

```
GET /api/categories

POST /api/categories

DELETE /api/categories/{id}
```

------

## Holding

查询：

```
GET /api/holdings
```

添加：

```
POST /api/holdings
```

删除：

```
DELETE /api/holdings/{id}
```

------

## Dashboard

首页数据：

```
GET /api/dashboard
```

返回：

```
{
totalValue:152430,

returnRate:12.8,

stock:110900,

bond:25330,

crypto:11000,

cash:5200
}
```

------

## Allocation

```
GET /api/allocation
```

返回：

```
{
stock:40,

bond:20,

crypto:15,

cash:25
}
```

------

## Performance

```
GET /api/performance
```

支持：

```
?range=max
```

------

收益排行：

```
GET /api/performance/ranking
```

------

# 四、前端实现（React）

结构：

```
src

├── pages

├── components

├── services

└── charts
```

------

# 页面1：首页 Dashboard

包含：

- Total Portfolio Value
- Total Return
- Asset Allocation 饼图
- Holdings表格
- 四个资产卡片

------

# 页面2：Holdings

功能：

- 查看持仓
- Add Holding
- Delete Holding

------

# 页面3：Asset Category

功能：

- 查看 Stock/Bond/Crypto/Cash
- Add Asset
- Delete Asset

------

# 页面4：Performance

功能：

- 收益折线图
- 时间范围选择
- Top Gainer
- Top Loser

------

# 五、四个人任务分配

- ## 1. Asset（资产类别）

  Asset 是**大类分类**：

  例如：

  ```
  Asset
  
  ├── Stock（股票）
  ├── Bond（债券）
  ├── Cash（现金）
  └── Crypto（加密货币）
  ```

  它回答：

  > 钱属于哪一种资产类别？

  ------

  ## 2. Holding（具体持仓）

  Holding 才是用户实际拥有的东西：

  例如：

  ```
  Holdings
  
  AAPL
  TSLA
  AMZN
  BTC
  ```

  它回答：

  > 用户具体买了什么？

  例如：

  | Holding     | Asset Type |
  | ----------- | ---------- |
  | AAPL        | Stock      |
  | TSLA        | Stock      |
  | BTC         | Crypto     |
  | US Treasury | Bond       |

  关系：

  ```
  Asset Category
          |
          |
          ↓
      Holdings
  
  Stock
   |
   ├── AAPL
   ├── TSLA
   └── AMZN
  
  
  Crypto
   |
   └── BTC
  ```

  ------

  # 3. Asset Allocation

  这个也重新明确：

  截图：

  ```
  Asset Allocation
  
  Stocks 40%
  Bonds 20%
  Crypto 15%
  Cash 25%
  ```

  它展示的是：

  **Asset 类别占比**

  所以数据来源应该是：

  ```
  Holdings
      |
      |
  按照 Asset Type 分类统计
      |
      ↓
  Asset Allocation
  ```

  比如：

  用户持仓：

  ```
  AAPL $50,000
  TSLA $30,000
  BTC $20,000
  Cash $10,000
  ```

  统计：

  ```
  Stock:
  80000
  
  Crypto:
  20000
  
  Cash:
  10000
  ```

  显示：

  ```
  Stock 72%
  Crypto 18%
  Cash 10%
  ```

  ------

  # 所以最终正确结构应该是：

  ```
                  Asset Category
                      |
         -----------------------------
         |            |             |
       Stock        Bond          Crypto
  
  
                      ↑
                      |
                  Holdings
  
            AAPL
            TSLA
            BTC
            Bond ETF
  
  
                      |
                      ↓
  
            Dashboard
  
            Total Portfolio Value
  
            Total Return
  
            Asset Allocation
  ```

# 👤 Person 1：Holding Management（持仓管理）

## 负责目标

管理用户具体拥有的资产。

例如：

```
AAPL
TSLA
AMZN
BTC
```

对应截图：

```
Your Holdings
```

------

# 数据库设计

负责表：

## holdings

| 字段           | 说明     |
| -------------- | -------- |
| id             | 主键     |
| symbol         | 股票代码 |
| company_name   | 公司名称 |
| asset_type     | 资产类别 |
| shares         | 持有数量 |
| purchase_price | 买入价格 |
| current_price  | 当前价格 |
| purchase_date  | 购买日期 |

示例：

| symbol | type   | shares |
| ------ | ------ | ------ |
| AAPL   | Stock  | 35     |
| BTC    | Crypto | 2      |

------

# 后端

负责 API：

### 查询持仓

```
GET /api/holdings
```

返回所有持仓。

### 添加持仓

```
POST /api/holdings
```

例如：

添加：

```
AAPL
35 shares
```

### 删除持仓

```
DELETE /api/holdings/{id}
```

------

# 前端

## 页面（Page）

### `Holdings.jsx`

路径：

```
src/pages/Holdings.jsx
```

功能：

展示用户所有持仓。

页面效果：

```
---------------------------------

Your Holdings

+ Add Holding


Symbol | Company | Shares | Price | Value | P/L

AAPL     Apple     35      145    5086   +886

TSLA     Tesla     20      720   14403  +1402


Delete

---------------------------------
```

------

# 组件（Components）

## 1. `HoldingsTable.jsx`

路径：

```
src/components/HoldingsTable.jsx
```

负责：

展示表格：

字段：

```
Symbol

Company

Shares

Current Price

Value

Profit/Loss
```

数据来源：

调用：

```
GET /api/holdings
```

------

## 2. `AddHoldingForm.jsx`

负责添加持仓。

页面：

点击：

```
+ Add Holding
```

弹出：

```
Symbol:

AAPL


Company:

Apple Inc


Category:

Stock


Shares:

35


Purchase Price:

120
```

提交：

调用：

```
POST /api/holdings
```

------

## 3. `DeleteHoldingButton.jsx`

负责删除。

调用：

```
DELETE /api/holdings/{id}
```

------

## 前端文件：

最终：

```
pages

└── Holdings.jsx


components

├── HoldingsTable.jsx

├── AddHoldingForm.jsx

└── DeleteHoldingButton.jsx
```

|      |      |      |      |      |
| ---- | ---- | ---- | ---- | ---- |
|      |      |      |      |      |

------

# 👤 Person 2：Asset Category + Asset Allocation（资产类别与配置）

## 负责目标

管理资产分类：

```
Stock

Bond

Crypto

Cash
```

并完成：

```
Asset Allocation
```

饼图。

------

# 数据库设计

负责表：

## asset_category

| 字段          | 说明     |
| ------------- | -------- |
| id            | 主键     |
| category_name | 类别名称 |
| description   | 描述     |

数据：

```
1 Stock

2 Bond

3 Crypto

4 Cash
```

------

# 后端

负责：

### 获取资产类别

```
GET /api/categories
```

### 添加类别

```
POST /api/categories
```

### 删除类别

```
DELETE /api/categories/{id}
```

------

### Asset Allocation接口

根据类别金额计算：

例如：

```
Stocks Value
80000

Bonds Value
20000

Crypto Value
10000

Cash
5000
```

转换：

```
Stock 70%

Bond 20%

Crypto 8%

Cash 2%
```

接口：

```
GET /api/allocation
```

------

# 前端

- # 页面1：

  ## `AssetCategory.jsx`

  路径：

  ```
  src/pages/AssetCategory.jsx
  ```

  功能：

  管理资产类别。

  显示：

  ```
  Asset Categories
  
  
  Stock
  
  Bond
  
  Crypto
  
  Cash
  
  
  + Add Asset
  
  Delete
  ```

  ------

  # 组件

  ## 1. `CategoryList.jsx`

  显示：

  ```
  Stock
  
  Bond
  
  Crypto
  
  Cash
  ```

  调用：

  ```
  GET /api/categories
  ```

  ------

  ## 2. `AddCategoryForm.jsx`

  添加资产类别。

  表单：

  ```
  Category Name:
  
  Stock
  ```

  调用：

  ```
  POST /api/categories
  ```

  ------

  ## 3. `AssetAllocationChart.jsx`

  重点组件。

  路径：

  ```
  components/AssetAllocationChart.jsx
  ```

  负责：

  饼图：

  ```
  Stock 40%
  
  Bond 20%
  
  Crypto 15%
  
  Cash 25%
  ```

  数据：

  调用：

  ```
  GET /api/allocation
  ```

  技术：

  Chart.js

  ------

  ## 前端文件：

  ```
  pages
  
  └── AssetCategory.jsx
  
  
  components
  
  ├── CategoryList.jsx
  
  ├── AddCategoryForm.jsx
  
  └── AssetAllocationChart.jsx
  ```

------

# 👤 Person 3：Dashboard Overview（首页总览）

## 负责目标

完全实现截图首页核心。

包括：

顶部：

```
Total Portfolio Value

Total Return
```

底部：

```
Cash Balance

Stocks Value

Bonds Value

Crypto Value
```

------

# 数据库设计

负责表：

## portfolio_summary

| 字段         | 说明     |
| ------------ | -------- |
| id           | 主键     |
| total_value  | 总资产   |
| total_return | 收益率   |
| cash_value   | 现金     |
| stock_value  | 股票     |
| bond_value   | 债券     |
| crypto_value | 加密货币 |
| update_time  | 更新时间 |

------

# 后端

负责：

Dashboard API：

```
GET /api/dashboard
```

返回：

```
{
"totalValue":152430,
"returnRate":12.8,
"cash":5200,
"stocks":110900,
"bonds":25330,
"crypto":11000
}
```

------

# 前端

这是截图首页。

------

# 页面：

## `Dashboard.jsx`

路径：

```
src/pages/Dashboard.jsx
```

包含：

```
Total Portfolio Value

Total Return

Asset Allocation

Holdings

Bottom Cards
```

------

# 组件

## 1. `PortfolioSummaryCard.jsx`

显示：

顶部：

```
Total Portfolio Value

$152,430
```

以及：

```
Total Return

+12.8%
```

调用：

```
GET /api/dashboard
```

------

## 2. `AssetValueCards.jsx`

底部四个卡片：

拆成：

```
CashCard.jsx

StockCard.jsx

BondCard.jsx

CryptoCard.jsx
```

显示：

```
Cash Balance

$5200
```

------

## 3. `DashboardAllocation.jsx`

注意：

这个不是自己写饼图。

调用 Person 2 的组件或者接口。

显示：

```
Asset Allocation
```

数据：

```
GET /api/allocation
```

------

## 4. `RecentHoldings.jsx`

首页下面显示：

```
AAPL

TSLA

BTC
```

调用：

```
GET /api/holdings
```

------

## 前端文件：

```
pages

└── Dashboard.jsx


components

├── PortfolioSummaryCard.jsx

├── AssetValueCards.jsx

├── DashboardAllocation.jsx

└── RecentHoldings.jsx
```



------

# 👤 Person 4：Performance + 小功能扩展

## 负责目标

收益趋势模块。

对应截图：

```
Portfolio Performance
```

------

# 数据库设计

负责表：

## portfolio_history

| 字段            | 说明     |
| --------------- | -------- |
| id              | 主键     |
| date            | 日期     |
| portfolio_value | 资产价值 |
| return_rate     | 收益率   |

例如：

| Month | Value  |
| ----- | ------ |
| Apr   | 120000 |
| May   | 125000 |
| Jun   | 135000 |
| Jul   | 145000 |

------

# 后端

负责：

### 查询历史收益

```
GET /api/performance
```

支持：

时间范围：

```
1M

3M

6M

1Y

MAX
```

例如：

```
GET /api/performance?range=max
```

------

# 前端

负责：

## Performance 页面

实现：

折线图：

```
Portfolio Performance
```

------

并加入：

时间筛选：

```
页面：
Performance.jsx

路径：

src/pages/Performance.jsx

页面：

Portfolio Performance


        折线图


1M 3M 6M 1Y MAX


Top Gainer

AAPL +25%


Top Loser

TSLA -8%
组件
1. PerformanceChart.jsx

核心组件。

负责：

折线图：

Apr

May

Jun

Jul

数据：

调用：

GET /api/performance
2. TimeRangeSelector.jsx

负责：

时间按钮：

1M

3M

6M

1Y

MAX

点击：

例如：

MAX

请求：

GET /api/performance?range=max
3. PerformanceRanking.jsx

第4个人的小功能。

显示：

Top Gainer

AAPL +25%


Top Loser

TSLA -8%

调用：

GET /api/performance/ranking
前端文件：
pages

└── Performance.jsx


components

├── PerformanceChart.jsx

├── TimeRangeSelector.jsx

└── PerformanceRanking.jsx
```

# 第4个人额外小功能（可选）：

## Best / Worst Performing Asset（收益排行）

展示：

例如：

```
Top Gainer

AAPL

+25%


Top Loser

TSLA

-8%
```

新增数据库：

## asset_performance

| 字段        | 说明     |
| ----------- | -------- |
| id          | 主键     |
| symbol      | 资产代码 |
| profit_rate | 收益率   |

API：

```
GET /api/performance/ranking
```

前端：

增加：

```
Performance Summary Card
```

------

# 最终任务表

| 成员 | 模块                 | 数据库              | 后端          | 前端          |
| ---- | -------------------- | ------------------- | ------------- | ------------- |
| 1    | Holding管理          | holdings            | CRUD API      | Holding页面   |
| 2    | Asset类别+Allocation | asset_category      | 类别+配置API  | 分类页+饼图   |
| 3    | Dashboard首页        | portfolio_summary   | Dashboard API | 首页全部展示  |
| 4    | Performance+收益排行 | history+performance | 趋势+排行API  | 折线图+分析卡 |

# 最终 React 项目结构

大概：

```
src

├── pages
│
├── Dashboard.jsx
├── Holdings.jsx
├── AssetCategory.jsx
└── Performance.jsx


├── components

├── PortfolioSummaryCard.jsx
├── HoldingsTable.jsx
├── AssetAllocationChart.jsx
├── PerformanceChart.jsx
├── AddHoldingForm.jsx
├── CategoryList.jsx
└── RankingCard.jsx


├── services

└── api.js


└── App.jsx
```