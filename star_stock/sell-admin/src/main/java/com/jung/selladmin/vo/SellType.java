package com.jung.selladmin.vo;
/*
    첫째자리(눌림목 높이)
    A - 장대 상단의 0이하% 30%이상
    B - 장대 상단의 30% 미만 70% 이상
    C - 장대 상단의 70% 미만 지지선 이상

    둘째자리(이전 캔들높이)
    A - 저항선 근처
    B - 중간지점
    C - 지지선 근처
 */
public enum SellType {
    AA("익절은 5% or 10%(힘), 손절은 지지선과 -5%중 MIN값"),
    AB("익절은 5% or 10%(힘), 손절은 지지선과 -5%중 MIN값"),
    AC("익절은 5% or 10%(힘), 손절은 -3.5%"),
    BA("익절은 5%, 손절은 지지선과 -2.5%중 MIN값"),
    BB("익절은 저항선과 5%중 MIN값, 손절은 지지선과 -5%중 MIN값"),
    BC("익절은 저항선과 5%중 MIN값, 손절은 -5%"),
    CA("익절은 5%, 손절은 지지선과 -2.5%중 MIN값"),
    CB("익절은 저항선과 5%중 MIN값, 손절은 지지선과 -5%중 MIN값"),
    CC("익절은 저항선과 5%중 MIN값, 손절은 -2.5%");

    private final String desc;

    SellType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
