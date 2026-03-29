package com.jung.selladmin.dto;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RetrieveStockRtnDTO {
    private String stockShortCode;
    private String stockName;
    private boolean autoSellActive = false;
    private String myPrice;
    private String buyTimeAt;
    private String sellType;
    private String sellDesc;
    private String futureGainPrice;
    private String futurePainPrice;
    private float futureGainRate;
    private float futurePainRate;
}
