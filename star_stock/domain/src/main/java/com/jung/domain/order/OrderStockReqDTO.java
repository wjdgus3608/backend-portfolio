package com.jung.domain.order;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderStockReqDTO {
    private String acno;
    private String stockShortCode;
    private OrderType orderType;
    private String amount;
}
