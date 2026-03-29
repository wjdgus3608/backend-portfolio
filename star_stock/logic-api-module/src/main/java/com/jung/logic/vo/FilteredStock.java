package com.jung.logic.vo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class FilteredStock {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String searchTime;
    private String stockNormalCode;
    private String stockShortCode;
    private String stockName;
}
