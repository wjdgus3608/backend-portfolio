package com.jung.feedbot.feedbot.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class StockKey implements Serializable {
    private String stockName;
    private String date;
}
