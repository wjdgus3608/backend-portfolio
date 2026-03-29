package com.jung.selladmin.vo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
public class SellHistory {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String stockShortCode;
    private String stockName;
    private String sellTimeAt;
    private String sellType;
    private String sellResult;
    private float resultRate;
    private String betterTimeAt;
    private String betterResult;
}
