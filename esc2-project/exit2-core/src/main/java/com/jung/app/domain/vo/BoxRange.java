package com.jung.app.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BoxRange {
    private double top;
    private double middle;
    private double bottom;
    private double size;
    private double atr;
    private String startTimeAt;
    private String endTimeAt;
}
