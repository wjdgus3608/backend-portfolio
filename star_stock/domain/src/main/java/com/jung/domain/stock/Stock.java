package com.jung.domain.stock;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class Stock {
    private String stockNormalCode;
    private String stockShortCode;
    private String stockName;
}
