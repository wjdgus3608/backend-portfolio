
CREATE TABLE IF NOT EXISTS stocks (
                stock_code VARCHAR(100) PRIMARY KEY,
                stock_name VARCHAR(100) NOT NULL
);

 CREATE TABLE IF NOT EXISTS trade_amount (
                stock_code VARCHAR(100) NOT NULL,
                stock_name VARCHAR(100) NOT NULL,
                trade_amount BIGINT NOT NULL,
                time_at VARCHAR(100) NOT NULL,
                PRIMARY KEY (stock_code, time_at)
            );
