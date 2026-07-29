-- =========================================================================
-- Enrich holdings table with a more diverse sample portfolio
-- =========================================================================
-- Purpose: the original create_PM_sql.sql only seeds 5 holdings, all of
-- category "Stock" (AAPL/TSLA/AMZN/FB/C). This script ADDS additional
-- holdings covering every asset_category (Bond, Cash, Cryptocurrency, ETF,
-- Real Estate) plus a few more stocks, so the portfolio looks realistic
-- and the UI/charts have varied data to render.
--
-- Safe to re-run: it first deletes any existing rows with the same
-- symbols (only the ones this script manages) before re-inserting, so
-- running it multiple times will NOT create duplicate rows. It does NOT
-- touch the original 5 holdings (AAPL/TSLA/AMZN/FB/C) seeded by
-- create_PM_sql.sql, since price_history_seed.csv data is keyed to those.
--
-- How to run:
--   mysql -u root -p prot_manager_DB < src\main\resources\SQL\holdings_enrich_seed.sql
-- =========================================================================

USE prot_manager_DB;

-- Remove previous run of this script's rows (idempotent), without
-- touching the original 5 seed holdings.
DELETE FROM holdings WHERE symbol IN (
    'MSFT', 'GOOGL', 'NVDA',
    'US10Y', 'CORPAA', 'MUNI5Y',
    'USD_CASH', 'EUR_CASH',
    'BTC', 'ETH', 'SOL',
    'SPY', 'QQQ', 'VTI',
    'VNQ', 'RESI01', 'PLD'
);

-- ---- Category 1: Stock (additional names beyond the original 5) ----
INSERT INTO holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) VALUES
    ('MSFT',  'Microsoft Corporation', 1, 80.0000,  340.00, 415.00, '2025-04-15'),
    ('GOOGL', 'Alphabet Inc',          1, 60.0000,  130.00, 165.00, '2025-05-20'),
    ('NVDA',  'NVIDIA Corporation',   1, 40.0000,  450.00, 900.00, '2025-02-10');

-- ---- Category 2: Bond ----
INSERT INTO holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) VALUES
    ('US10Y',  'US 10-Year Treasury Note',    2, 500.0000, 98.50,  99.20, '2025-01-15'),
    ('CORPAA', 'ABC Corp AA Rated Bond',      2, 300.0000, 101.25, 100.80, '2025-03-10'),
    ('MUNI5Y', 'Municipal Bond 5-Year',       2, 200.0000, 100.00, 101.10, '2025-05-01');

-- ---- Category 3: Cash ----
INSERT INTO holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) VALUES
    ('USD_CASH', 'US Dollar Cash', 3, 15000.0000, 1.00, 1.00, '2026-01-01'),
    ('EUR_CASH', 'Euro Cash',      3, 5000.0000,  1.00, 1.08, '2026-02-01');

-- ---- Category 4: Cryptocurrency ----
INSERT INTO holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) VALUES
    ('BTC', 'Bitcoin',  4, 0.7500,  42000.00, 61000.00, '2025-11-20'),
    ('ETH', 'Ethereum', 4, 10.0000, 2200.00,  3400.00,  '2025-12-05'),
    ('SOL', 'Solana',   4, 150.0000, 95.00,   145.00,   '2026-01-10');

-- ---- Category 5: ETF ----
INSERT INTO holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) VALUES
    ('SPY', 'SPDR S&P 500 ETF Trust',           5, 80.0000,  480.00, 560.00, '2025-06-15'),
    ('QQQ', 'Invesco QQQ Trust',                5, 60.0000,  400.00, 470.00, '2025-07-01'),
    ('VTI', 'Vanguard Total Stock Market ETF',  5, 120.0000, 230.00, 265.00, '2025-09-10');

-- ---- Category 6: Real Estate ----
INSERT INTO holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) VALUES
    ('VNQ',    'Vanguard Real Estate ETF',           6, 200.0000, 85.00,     92.00,     '2025-08-01'),
    ('RESI01', 'Rental Property - Downtown Condo',   6, 1.0000,   350000.00, 385000.00, '2024-05-01'),
    ('PLD',    'Prologis Inc REIT',                  6, 100.0000, 110.00,    128.00,    '2025-10-01');

-- Quick check
SELECT h.symbol, h.company_name, ac.category_name, h.shares, h.purchase_price, h.current_price, h.purchase_date
FROM holdings h
JOIN asset_category ac ON ac.id = h.category_id
ORDER BY h.category_id, h.symbol;
