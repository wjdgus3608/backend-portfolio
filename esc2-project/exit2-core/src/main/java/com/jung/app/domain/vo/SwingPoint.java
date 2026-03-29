package com.jung.app.domain.vo;

import com.jung.app.domain.vo.em.SwingPointType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SwingPoint {

    private final double price;
    private final int index;
    private final SwingPointType type;

    double price() { return price; }
    int index() { return index; }
    SwingPointType type() { return type; }
}
