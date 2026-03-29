package com.jung.domain.stock;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StockNowPriceReq {
    private String stockType;
    private String stockShortCode;
}
