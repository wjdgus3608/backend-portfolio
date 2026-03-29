package com.jung.backtest.domain.vo;

import lombok.Data;

@Data
public class Account {
    String stockName;
    float stockPrice;
    float stockAmount;
}
