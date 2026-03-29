
CREATE TABLE IF NOT EXISTS minute_candles (
                stock_code VARCHAR(100) NOT NULL,
                stock_name VARCHAR(100) NOT NULL,
                top_price BIGINT NOT NULL,
                bottom_price BIGINT NOT NULL,
                start_price BIGINT NOT NULL,
                end_price BIGINT NOT NULL,
                trade_amount BIGINT NOT NULL,
                trade_money BIGINT NOT NULL,
                time_at VARCHAR(100) NOT NULL,
                PRIMARY KEY (stock_code, time_at)
);

 CREATE TABLE IF NOT EXISTS trade_amount (
                stock_code VARCHAR(100) NOT NULL,
                stock_name VARCHAR(100) NOT NULL,
                trade_amount BIGINT NOT NULL,
                time_at VARCHAR(100) NOT NULL,
                PRIMARY KEY (stock_code, time_at)
            );
