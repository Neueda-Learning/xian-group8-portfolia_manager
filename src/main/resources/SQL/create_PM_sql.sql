create database prot_manager_DB;
use prot_manager_DB;


-- 1. Asset Category Table
CREATE TABLE asset_category (
                                id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Category primary key',
                                category_name VARCHAR(30) NOT NULL UNIQUE COMMENT 'Asset category name',
                                description VARCHAR(200) COMMENT 'Category description'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Asset category dictionary';

-- 2. Holdings Table (core business table, references asset_category)
CREATE TABLE holdings (
                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Holding primary key',
                          symbol VARCHAR(30) NOT NULL COMMENT 'Asset ticker symbol',
                          company_name VARCHAR(100) NOT NULL COMMENT 'Asset / company full name',
                          category_id INT NOT NULL COMMENT 'Foreign key to asset_category.id',
                          shares DECIMAL(18,4) NOT NULL COMMENT 'Number of shares / units held',
                          purchase_price DECIMAL(18,2) NOT NULL COMMENT 'Cost basis per unit',
                          current_price DECIMAL(18,2) NOT NULL COMMENT 'Current market price',
                          purchase_date DATE NOT NULL COMMENT 'Date of purchase',
                          FOREIGN KEY (category_id) REFERENCES asset_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Portfolio holdings detail';

-- 3. Portfolio History Table (for performance line chart, daily granularity)
CREATE TABLE portfolio_history (
                                   id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Record primary key',
                                   record_date DATE NOT NULL UNIQUE COMMENT 'Snapshot date',
                                   portfolio_value DECIMAL(18,2) NOT NULL COMMENT 'Total portfolio market value',
                                   return_rate DECIMAL(10,4) NOT NULL COMMENT 'Cumulative return rate'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Historical portfolio performance';

-- 4. Price History Table (raw intraday tick data, 1:1 with the sample market API response)
-- Source: https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker={SYMBOL}
-- Stores EVERY data point returned by the API (open/high/low/close/volume at each timestamp),
-- so no data from the API is lost. Used to compute a fine-grained "performance curve".
CREATE TABLE price_history (
                               id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Record primary key',
                               symbol VARCHAR(30) NOT NULL COMMENT 'Asset ticker symbol (matches holdings.symbol)',
                               price_time DATETIME NOT NULL COMMENT 'Timestamp of this price tick',
                               open_price DECIMAL(18,4) COMMENT 'Open price',
                               high_price DECIMAL(18,4) COMMENT 'High price',
                               low_price DECIMAL(18,4) COMMENT 'Low price',
                               close_price DECIMAL(18,4) COMMENT 'Close price',
                               volume BIGINT COMMENT 'Traded volume',
                               UNIQUE KEY uq_symbol_time (symbol, price_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Raw intraday price history fetched from the sample market API';


-- Insert asset categories
INSERT INTO asset_category (id, category_name, description) VALUES
                                                                (1, 'Stock', 'Common stock equity assets'),
                                                                (2, 'Bond', 'Fixed income bond assets'),
                                                                (3, 'Cash', 'Cash and money market funds'),
                                                                (4, 'Cryptocurrency', 'Digital crypto assets'),
                                                                (5, 'ETF', 'Exchange traded funds'),
                                                                (6, 'Real Estate', 'Real estate investment assets');

-- Insert sample holdings
-- Data sourced live from the sample price API (README Appendix D):
-- https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker={SYMBOL}
-- Only these 5 tickers are cached by that API: C, AMZN, TSLA, FB, AAPL
-- purchase_price/purchase_date = first valid close in the returned intraday series (2026-07-22)
-- current_price = latest valid close returned by the API as of 2026-07-27/28
INSERT INTO holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) VALUES
                                                                                                                   ('AAPL', 'Apple Inc', 1, 300.0000, 325.07, 333.07, '2026-07-22'),
                                                                                                                   ('TSLA', 'Tesla Inc', 1, 100.0000, 374.32, 313.00, '2026-07-22'),
                                                                                                                   ('AMZN', 'Amazon.com Inc', 1, 150.0000, 248.73, 232.07, '2026-07-22'),
                                                                                                                   ('FB', 'Meta Platforms Inc', 1, 400.0000, 44.70, 44.50, '2026-07-22'),
                                                                                                                   ('C', 'Citigroup Inc', 1, 250.0000, 130.13, 132.21, '2026-07-22');

-- Insert historical performance data
-- Computed from the same live API data: total portfolio value each trading day =
-- sum(shares * end-of-day close) across all 5 holdings above.
-- return_rate is cumulative return relative to the first available trading day (2026-07-22).
INSERT INTO portfolio_history (record_date, portfolio_value, return_rate) VALUES
                                                                              ('2026-07-22', 224337.99, 0.0000),
                                                                              ('2026-07-23', 222807.18, -0.0068),
                                                                              ('2026-07-24', 214241.73, -0.0450),
                                                                              ('2026-07-27', 216887.21, -0.0332);

-- Insert raw intraday price history (all data points from the API for the 5 supported tickers).
-- NOTE: this table is bulk-loaded from src/main/resources/SQL/price_history_seed.csv
-- via LOAD DATA LOCAL INFILE (see the accompanying load-price-history.sql / project docs),
-- because it contains ~1500 rows (5 symbols x ~300 intraday ticks each) which is impractical
-- to keep as literal INSERT statements in this file.
-- To (re)populate it, run (note: CSV uses Windows CRLF line endings):
--   LOAD DATA LOCAL INFILE 'price_history_seed.csv'
--   INTO TABLE price_history
--   FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
--   LINES TERMINATED BY '\r\n'
--   IGNORE 1 ROWS
--   (symbol, price_time, open_price, high_price, low_price, close_price, volume);


select * from asset_category;
select * from holdings;
select * from portfolio_history;
select count(*) as price_history_row_count from price_history;


-- =========================
-- 4. Trade Record Wide Table (NO FK, no join needed)
-- =========================
CREATE TABLE trade_record_wide (
                                   id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Trade record primary key',
                                   trade_no VARCHAR(40) NOT NULL COMMENT 'Business trade number',

    -- Asset redundant fields
                                   asset_symbol VARCHAR(30) NOT NULL COMMENT 'Asset ticker symbol, e.g. AAPL/BTC/USD_CASH',
                                   asset_name VARCHAR(100) NOT NULL COMMENT 'Asset full name',
                                   asset_category_name VARCHAR(30) NOT NULL COMMENT 'Asset category name, e.g. Stock/Cash/Cryptocurrency',
                                   is_cash_asset TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=cash asset, 0=non-cash asset',

    -- Trade type redundant fields
                                   trade_type_code VARCHAR(20) NOT NULL COMMENT 'BUY/SELL/DEPOSIT/WITHDRAW',
                                   trade_type_name VARCHAR(50) NOT NULL COMMENT 'Trade type display name',

    -- Trade core fields
                                   trade_shares DECIMAL(18,4) NOT NULL COMMENT 'Traded shares/units; for USD_CASH, unit is cent (0.01 USD)',
                                   buy_price DECIMAL(18,2) DEFAULT NULL COMMENT 'Buy unit price; for USD_CASH BUY/DEPOSIT use 0.01',
                                   sell_price DECIMAL(18,2) DEFAULT NULL COMMENT 'Sell unit price; for USD_CASH SELL/WITHDRAW use 0.01',
                                   trade_amount DECIMAL(18,2) NOT NULL COMMENT 'Trade amount = shares * unit price',
                                   fee DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT 'Transaction fee',
                                   currency VARCHAR(10) NOT NULL DEFAULT 'USD' COMMENT 'Currency code',

    -- Cash impact fields (direct query friendly)
                                   cash_asset_symbol VARCHAR(30) NOT NULL DEFAULT 'USD_CASH' COMMENT 'Cash asset symbol',
                                   cash_change DECIMAL(18,2) NOT NULL COMMENT 'Cash impact: positive=increase, negative=decrease',

    -- Date and note
                                   trade_date DATE NOT NULL COMMENT 'Trade date',
                                   note VARCHAR(200) DEFAULT NULL COMMENT 'Optional note',
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Wide trade record table without foreign keys';

-- Suggested indexes for common query scenarios
CREATE INDEX idx_trw_asset_symbol ON trade_record_wide(asset_symbol);
CREATE INDEX idx_trw_trade_date ON trade_record_wide(trade_date);
CREATE INDEX idx_trw_trade_type_code ON trade_record_wide(trade_type_code);
CREATE INDEX idx_trw_asset_category_name ON trade_record_wide(asset_category_name);

-- =========================
-- Sample data
-- =========================
INSERT INTO trade_record_wide
(trade_no, asset_symbol, asset_name, asset_category_name, is_cash_asset,
 trade_type_code, trade_type_name, trade_shares, buy_price, sell_price,
 trade_amount, fee, currency, cash_asset_symbol, cash_change, trade_date, note)
VALUES
-- AAPL BUY: cash decreases
('TRX202402010001', 'AAPL', 'Apple Inc', 'Stock', 0,
 'BUY', 'Buy', 50.0000, 130.00, NULL,
 6500.00, 2.50, 'USD', 'USD_CASH', -6502.50, '2024-02-01', 'Add AAPL position'),

-- AAPL SELL: cash increases
('TRX202406150001', 'AAPL', 'Apple Inc', 'Stock', 0,
 'SELL', 'Sell', 20.0000, NULL, 150.00,
 3000.00, 2.50, 'USD', 'USD_CASH', 2997.50, '2024-06-15', 'Partial sell AAPL'),

-- TSLA BUY
('TRX202403050001', 'TSLA', 'Tesla Inc', 'Stock', 0,
 'BUY', 'Buy', 10.0000, 700.00, NULL,
 7000.00, 3.00, 'USD', 'USD_CASH', -7003.00, '2024-03-05', 'Add TSLA'),

-- BTC SELL
('TRX202407010001', 'BTC', 'Bitcoin', 'Cryptocurrency', 0,
 'SELL', 'Sell', 0.0500, NULL, 32000.00,
 1600.00, 5.00, 'USD', 'USD_CASH', 1595.00, '2024-07-01', 'Take BTC profit'),

-- Cash DEPOSIT: no other asset change (cash unit = 0.01 USD, shares are in cents)
('TRX202402200001', 'USD_CASH', 'US Dollar Cash', 'Cash', 1,
 'DEPOSIT', 'Cash Deposit', 200000.0000, 0.01, NULL,
 2000.00, 0.00, 'USD', 'USD_CASH', 2000.00, '2024-02-20', 'Monthly deposit'),

-- Cash WITHDRAW: no other asset change (cash unit = 0.01 USD, shares are in cents)
('TRX202405100001', 'USD_CASH', 'US Dollar Cash', 'Cash', 1,
 'WITHDRAW', 'Cash Withdraw', 50000.0000, NULL, 0.01,
 500.00, 0.00, 'USD', 'USD_CASH', -500.00, '2024-05-10', 'Withdraw for expense');

-- quick check
SELECT * FROM trade_record_wide ORDER BY trade_date, id;


