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

-- 3. Portfolio History Table (for performance line chart)
CREATE TABLE portfolio_history (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Record primary key',
    record_date DATE NOT NULL UNIQUE COMMENT 'Snapshot date',
    portfolio_value DECIMAL(18,2) NOT NULL COMMENT 'Total portfolio market value',
    return_rate DECIMAL(10,4) NOT NULL COMMENT 'Cumulative return rate'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Historical portfolio performance';


-- Insert asset categories
INSERT INTO asset_category (id, category_name, description) VALUES
(1, 'Stock', 'Common stock equity assets'),
(2, 'Bond', 'Fixed income bond assets'),
(3, 'Cash', 'Cash and money market funds'),
(4, 'Cryptocurrency', 'Digital crypto assets'),
(5, 'ETF', 'Exchange traded funds'),
(6, 'Real Estate', 'Real estate investment assets');

-- Insert sample holdings
INSERT INTO holdings (symbol, company_name, category_id, shares, purchase_price, current_price, purchase_date) VALUES
('AAPL', 'Apple Inc', 1, 300.0000, 120.00, 145.20, '2024-01-15'),
('TSLA', 'Tesla Inc', 1, 100.0000, 650.00, 720.50, '2024-02-10'),
('TLT', '20+ Year Treasury Bond ETF', 2, 200.0000, 120.00, 126.65, '2024-01-20'),
('USD_CASH', 'US Dollar Cash', 3, 5200.0000, 1.00, 1.00, '2024-01-01'),
('BTC', 'Bitcoin', 4, 0.3500, 28000.00, 31428.57, '2024-03-01');

-- Insert historical performance data (4 months)
INSERT INTO portfolio_history (record_date, portfolio_value, return_rate) VALUES
('2024-04-30', 120000.00, 0.0520),
('2024-05-31', 128500.00, 0.0835),
('2024-06-30', 139200.00, 0.1068),
('2024-07-31', 152430.25, 0.1280);


select * from asset_category;
select * from holdings;
select * from portfolio_history;


