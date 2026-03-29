package com.jung.selladmin.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class SaveStockStatusReqDTO {
    private String stockShortCode;
    private boolean autoSellActive;
}
