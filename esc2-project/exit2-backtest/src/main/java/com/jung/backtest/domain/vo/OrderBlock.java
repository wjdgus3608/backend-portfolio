package com.jung.backtest.domain.vo;

import lombok.*;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class OrderBlock {
    public double top;           // 몸통 상단
    public double bottom;        // 몸통 하단
    public int createdIndex;   // 해당 캔들이 생성된 인덱스
    public boolean used;       // 사용 여부 (필요 시 마크용)
    public double prevCandleLow;
}
