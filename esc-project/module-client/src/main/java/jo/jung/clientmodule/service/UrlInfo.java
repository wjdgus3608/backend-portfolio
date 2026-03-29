package jo.jung.clientmodule.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public enum UrlInfo {

    QUOTATIONS_INQUIRE_TIME_DAILYCHARTPRICE(
            "/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice",
            "KOSPI" // 예시로 KOSPI 코드 사용
    ),

    HOLIDAY_CHECK("/uapi/domestic-stock/v1/quotations/chk-holiday", "CTCA0903R"),
    NOW_PRICE("/uapi/domestic-stock/v1/quotations/inquire-price", "FHKST01010100"),
    //일봉 최대 30개 기간설정 불가
    DAILY_PRICE_URL("/uapi/domestic-stock/v1/quotations/inquire-daily-price", "FHKST01010400"),
    BUY_PERSON_URL("/uapi/domestic-stock/v1/quotations/inquire-investor", "FHKST01010900"),

    //주식 일별 분봉조회
    DAILY_MINUTE_CANDLE_URL("/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice", "FHKST03010230"),
    //주식 당일 분봉조회
    TODAY_MINUTE_CANDLE_URL("/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice", "FHKST03010200"),
    //주식 현재가 호가/예상체결
    TODAY_HO_PRICE_URL("/uapi/domestic-stock/v1/quotations/inquire-asking-price-exp-ccn", "FHKST01010200"),
    //주식 현재가 체결
    TODAY_TRADE_AMOUNT_URL("/uapi/domestic-stock/v1/quotations/inquire-ccnl", "FHKST01010300"),
    //주식 현재가 당일시간대별 체결
    TODAY_TIME_TRADE_AMOUNT_URL("/uapi/domestic-stock/v1/quotations/inquire-time-itemconclusion", "FHPST01060000"),
    //주식 잔고조회
    GET_MY_BOX("/uapi/domestic-stock/v1/trading/inquire-balance", "TTTC8434R"),
    //주식 매수가능/잔액조회
    GET_MY_MONEY("/uapi/domestic-stock/v1/trading/inquire-psbl-order", "TTTC8908R"),
    //주식 기본조회
    GET_STOCK_INFO("/uapi/domestic-stock/v1/quotations/search-stock-info", "CTPF1002R"),
    //거래량 순위
    GET_STOCK_TRADE_RANK_INFO("/uapi/domestic-stock/v1/quotations/volume-rank", "FHPST01710000"),
    //상승률 순위
    GET_STOCK_UPDOWN_RANK_INFO("/uapi/domestic-stock/v1/ranking/fluctuation", "FHPST01700000"),
    //국내주식기간별시세(일/주/월/년) - 일봉최대 100개 기간설정 가능
    GET_DAILY_CANDLE("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice", "FHKST03010100"),

    MAKE_BUY_ORDER("/uapi/domestic-stock/v1/trading/order-cash","TTTC0012U"),
    MAKE_SELL_ORDER("/uapi/domestic-stock/v1/trading/order-cash","TTTC0011U");

    private final String baseUrl = "https://openapi.koreainvestment.com:9443";
    private final String endpoint; // 엔드포인트
    private final String trId; // 거래 코드

}
