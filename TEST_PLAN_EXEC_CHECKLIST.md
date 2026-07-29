# Portfolio Manager Test Plan (Execution Checklist)

## 1) Test Goal
Validate that the 4-module system works end-to-end after iterative changes, with special focus on `USD_CASH` unit conversion (`1 unit = $0.01`).

## 2) Scope
- Backend APIs (`/api/holdings`, `/api/categories`, `/api/dashboard`, `/api/allocation`, `/api/performance`)
- Frontend pages (`holdings.html`, `categories.html`, `index.html`, `performance.html`)
- DB consistency (`holdings`, `trade_record_wide`, `portfolio_history`, `price_history`)
- Cross-module integration and regressions

## 3) Team and Timebox (4 people / half day)
- Person 1: Holding + Trade
- Person 2: Category + Allocation
- Person 3: Dashboard
- Person 4: Performance + Ranking

Suggested half-day split:
- 00:00-00:20 setup + smoke
- 00:20-02:20 parallel module testing
- 02:20-03:10 cross-module integration
- 03:10-03:40 bug fix retest
- 03:40-04:00 sign-off

## 4) Environment Checklist
- [ ] MySQL running, schema imported from `src/main/resources/SQL/create_PM_sql.sql`
- [ ] `price_history_seed.csv` loaded into `price_history`
- [ ] App starts on port from `src/main/resources/application.properties` (`9166`)
- [ ] Browser cache cleared (or hard refresh)
- [ ] Test evidence folder created (screenshots + SQL logs)

Optional startup commands:
```powershell
cd C:\eight8
mvn clean compile
mvn spring-boot:run
```

## 5) Shared Test Data Baseline
- [ ] Categories exist: Stock, Bond, Cash, Cryptocurrency, ETF, Real Estate
- [ ] At least one stock holding exists (e.g., AAPL)
- [ ] `USD_CASH` holding exists (or created automatically after first trade)
- [ ] `portfolio_history` has rows
- [ ] `price_history` has rows

SQL quick checks:
```sql
select count(*) as holdings_count from holdings;
select count(*) as trade_count from trade_record_wide;
select count(*) as ph_count from portfolio_history;
select count(*) as price_count from price_history;
select * from holdings where upper(symbol)='USD_CASH';
```

---

## 6) Person 1 - Holding + Trade (Owner: ______)
Files: `src/main/java/com/group8/portfolio_manager/service/HoldingService.java`, `src/main/resources/static/holdings.html`

### 6.1 API Functional Tests
- [ ] `GET /api/holdings` returns list with required fields
- [ ] `POST /api/holdings` valid payload creates holding
- [ ] `POST /api/holdings` invalid payload returns `400`
- [ ] `DELETE /api/holdings/{id}` deletes non-cash holding
- [ ] `POST /api/holdings/trade` cash only allows `DEPOSIT/WITHDRAW`
- [ ] `POST /api/holdings/trade` non-cash only allows `BUY/SELL`

### 6.2 Cash 0.01 Core Tests (P0)
- [ ] Deposit $2000 cash
  - Expected DB: `trade_shares=200000`, `buy_price=0.01`, `trade_amount=2000.00`
- [ ] Withdraw $500 cash
  - Expected DB: `trade_shares=50000`, `sell_price=0.01`, `trade_amount=500.00`
- [ ] Cash holding market value equals `shares * 0.01`
- [ ] Cash display in `holdings.html` shows USD (not raw cent units)

### 6.3 Trade Integration Tests
- [ ] Buy stock decreases cash by `(amount + fee)`
- [ ] Sell stock increases cash by `(amount - fee)`
- [ ] Insufficient cash blocks BUY
- [ ] Insufficient stock blocks SELL

### 6.4 Evidence
- [ ] API response screenshots/logs
- [ ] SQL rows from `trade_record_wide` and `holdings`
- [ ] UI screenshot before/after trade

---

## 7) Person 2 - Category + Allocation (Owner: ______)
Files: `src/main/java/com/group8/portfolio_manager/controller/AssetCategoryController.java`, `src/main/java/com/group8/portfolio_manager/service/PortfolioService.java`

### 7.1 Category API Tests
- [ ] `GET /api/categories` returns all categories
- [ ] `POST /api/categories` creates new category (e.g., Commodities)
- [ ] Duplicate category (case/space variants) is rejected
- [ ] Empty name is rejected
- [ ] Delete unused category succeeds
- [ ] Delete category linked to holdings returns conflict/meaningful error

### 7.2 Allocation Tests
- [ ] `GET /api/allocation` returns percentages
- [ ] Percentage sum is about 100% (allow rounding drift)
- [ ] Allocation updates after holding trade or price refresh
- [ ] Empty holdings does not crash

### 7.3 Cash Interaction Checks
- [ ] Cash portion reflects USD value (`shares * 0.01`), not raw shares

### 7.4 Evidence
- [ ] API logs and UI pie chart screenshots
- [ ] Validation error responses

---

## 8) Person 3 - Dashboard (Owner: ______)
Files: `src/main/java/com/group8/portfolio_manager/service/PortfolioService.java`, `src/main/resources/static/index.html`

### 8.1 Summary API Tests
- [ ] `GET /api/dashboard` returns `totalValue`, `returnRate`, category values
- [ ] `totalValue = sum(shares * current_price)` verified by SQL/manual calc
- [ ] `returnRate` handles `totalCost = 0` safely

### 8.2 UI Tests (`index.html`)
- [ ] Total Portfolio Value renders as currency
- [ ] Total Return sign/color is correct (+/-)
- [ ] Recent holdings table loads up to 5 items
- [ ] Cash row displays converted amount (USD)

### 8.3 Regression Tests
- [ ] After deposit/withdraw, dashboard values update correctly
- [ ] After stock trade, dashboard and holdings values remain consistent

### 8.4 Evidence
- [ ] Dashboard screenshots with timestamps
- [ ] SQL/value calculation sheet

---

## 9) Person 4 - Performance + Ranking (Owner: ______)
Files: `src/main/java/com/group8/portfolio_manager/controller/PerformanceController.java`, `src/main/java/com/group8/portfolio_manager/repository/PriceHistoryRepository.java`, `src/main/resources/static/performance.html`

### 9.1 Endpoint Tests
- [ ] `GET /api/performance?range=MAX` returns history
- [ ] `GET /api/performance?range=1M/3M/6M/1Y` returns filtered range
- [ ] `GET /api/performance/ranking` returns top gainer/loser
- [ ] `GET /api/performance/curve` returns chart points

### 9.2 Critical Chart Availability Test (P0)
- [ ] `performance.html` chart displays data points
- [ ] If chart empty, capture `/api/performance/curve` response body
- [ ] Verify `price_history` data coverage for held symbols

### 9.3 Known Risk Checks
- [ ] Ensure curve query is not over-filtering because of `USD_CASH`
- [ ] Ranking handles no-holding/one-holding cases without UI break

### 9.4 UI Behavior
- [ ] Range buttons switch active state correctly
- [ ] Tooltip currency format is correct
- [ ] Top gainer/loser values show symbol + percent

### 9.5 Evidence
- [ ] Network tab captures (`/api/performance/*`)
- [ ] Chart screenshots per range

---

## 10) Cross-Module Integration (All)

### 10.1 E2E Scenario A (Happy Path)
- [ ] Add cash via DEPOSIT $2000
- [ ] Buy AAPL with fee
- [ ] Verify holdings, trade records, dashboard, allocation, performance page all consistent

### 10.2 E2E Scenario B (Reverse)
- [ ] Sell part of AAPL
- [ ] Withdraw cash $500
- [ ] Verify same consistency across all pages/APIs

### 10.3 Data Consistency Checks
- [ ] `holdings` cash shares and `trade_record_wide` cash trades reconcile
- [ ] Dashboard total equals holdings aggregate
- [ ] Allocation aligns with dashboard category values

---

## 11) SQL Validation Checklist

- [ ] Latest cash trades:
```sql
select id, trade_no, trade_type_code, trade_shares, buy_price, sell_price, trade_amount, cash_change, trade_date
from trade_record_wide
where upper(asset_symbol)='USD_CASH'
order by id desc
limit 10;
```

- [ ] Cash holding state:
```sql
select id, symbol, shares, purchase_price, current_price, purchase_date
from holdings
where upper(symbol)='USD_CASH';
```

- [ ] Portfolio history availability:
```sql
select * from portfolio_history order by record_date desc limit 10;
```

- [ ] Price history coverage:
```sql
select symbol, count(*) as ticks
from price_history
group by symbol
order by ticks desc;
```

---

## 12) Defect Logging Template

For every failed checkbox, log:
- [ ] Module Owner
- [ ] Test ID (e.g., P1-6.2-Deposit)
- [ ] Steps to reproduce
- [ ] Expected vs Actual
- [ ] Screenshot / API payload / SQL proof
- [ ] Severity (`Critical`, `High`, `Medium`, `Low`)
- [ ] Fix commit / retest status

---

## 13) Final Sign-Off

### Person 1 Sign-Off
- [ ] All P0 passed
- [ ] Open defects reviewed
- Signature: ______  Time: ______

### Person 2 Sign-Off
- [ ] All P0 passed
- [ ] Open defects reviewed
- Signature: ______  Time: ______

### Person 3 Sign-Off
- [ ] All P0 passed
- [ ] Open defects reviewed
- Signature: ______  Time: ______

### Person 4 Sign-Off
- [ ] All P0 passed
- [ ] Open defects reviewed
- Signature: ______  Time: ______

### Team Lead Release Decision
- [ ] Go
- [ ] No-Go
- Notes: __________________________________________

---

## 14) Quick Retest Pack (if time is short)
Run these first:
- [ ] Cash deposit/withdraw P0
- [ ] Dashboard total and cash card consistency
- [ ] Allocation sum and cash ratio
- [ ] Performance chart renders non-empty
- [ ] End-to-end buy/sell with fee

