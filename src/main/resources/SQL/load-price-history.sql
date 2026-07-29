

USE prot_manager_DB;

-- Avoid duplicate rows if this script is re-run (safe to run multiple times).
DELETE FROM price_history;

LOAD DATA LOCAL INFILE 'price_history_seed.csv'--absolute pathname: /Users/username/IdeaProjects/portfolio-manager/src/main/resources/SQL/price_history_seed.csv
INTO TABLE price_history
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
LINES TERMINATED BY '\r\n'
IGNORE 1 ROWS
(symbol, price_time, open_price, high_price, low_price, close_price, volume);

-- Quick check
SELECT COUNT(*) AS price_history_row_count FROM price_history;
SELECT symbol, COUNT(*) AS ticks FROM price_history GROUP BY symbol ORDER BY symbol;
