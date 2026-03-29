package com.jung.logic.service.filter;

import com.jung.domain.stock.Stock;
import com.jung.kisclient.CommonApiClient;
import com.jung.logic.repo.PersonTradeAmountRepo;
import com.jung.logic.vo.PersonTradeAmountEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockFilter {

    private final int CANDLE_RANGE_DAY = 59;
    //상장일 기준(31일 미만 제외)
    private final int SELLING_DAYS = 30;

    private final CommonApiClient commonApiClient;
    private final PersonTradeAmountRepo personTradeAmountRepo;

    private static JSONParser jsonParser = new JSONParser();

    @Value("${my.minTotalPrice}")
    private long minTotalPrice;

    @Value("${my.maxTotalPrice}")
    private long maxTotalPrice;

    @Value("${my.upPriceRate}")
    private float upPriceRate;





    //시총 1000억~ 5000억 사이 종목 선정(유통주식수도 감안)
    public List<Stock> filterByTotalStockPrice(List<Stock> input) {

        log.info("--- StockFilter filterByTotalStockPrice start!!");
        log.info("--- input size : "+input.size());

        Iterator<Stock> iterator = input.iterator();
        while(iterator.hasNext()) {
            Stock stock = iterator.next();

            ResponseEntity<?> response = commonApiClient.callStockNowPrice(stock);
            if(response.getStatusCode().is5xxServerError()){
                iterator.remove();
                continue;
            }
            LinkedHashMap body = (LinkedHashMap)((JSONObject) response.getBody()).get("output");
            //시가총액 컬럼(억단위임)
            String totalPrice = (String) body.get("hts_avls");
            long totalPriceNum = Long.parseLong(totalPrice);

            if(totalPriceNum < minTotalPrice || totalPriceNum > maxTotalPrice) {
                iterator.remove();
            }
        }

        log.info("--- StockFilter filterByTotalStockPrice end!!");
        log.info("--- input size : "+input.size());

        return input;
    }

    //상장 30일미만 제외
    public List<Stock> filterBySellingDays(List<Stock> input){
        log.info("--- StockFilter filterBySellingDays start!!");
        log.info("--- input size : "+input.size());

        Iterator<Stock> iterator = input.iterator();
        while(iterator.hasNext()) {
            Stock stock = iterator.next();
            ResponseEntity<?> response = commonApiClient.callStockNowPriceOver30(stock);
            ArrayList candles = (ArrayList)((JSONObject) response.getBody()).get("output2");
            //상장일 기준
            if(candles.size() < SELLING_DAYS)
                iterator.remove();
        }


        log.info("--- StockFilter filterBySellingDays end!!");
        log.info("--- input size : "+input.size());

        return input;
    }

    //가격 급상승[10%이상(전일종가~종가기준) and 거래량 급증(5일 평균 거래량보다 2배이상)]
    public List<Stock> filterByPriceUpAndAmount(List<Stock> input) {

        //상장 30일이하 먼저 필터링
        input = filterBySellingDays(input);

        log.info("--- StockFilter filterByPriceUpAndAmount start!!");
        log.info("--- input size : "+input.size());

        Iterator<Stock> iterator = input.iterator();
        while(iterator.hasNext()) {
            Stock stock = iterator.next();
            ResponseEntity<?> response = commonApiClient.callStockNowPriceOver30(stock);
            ArrayList candles = (ArrayList)((JSONObject) response.getBody()).get("output2");

            //가격, 거래량 조건 미충족시 삭제
            if(!isOverTenPercentAndAmountBig(candles)){
                iterator.remove();
            }
        }

        log.info("--- StockFilter filterByPriceUpAndAmount end!!");
        log.info("--- input size : "+input.size());

        return input;
    }
    //다음날 연속으로 장대 양봉 or 장대 음봉 없어야함 and 최근가격이 급상승시 가격보다는 낮아야함
    public List<Stock> filterByLongCandle(List<Stock> input){

        log.info("--- StockFilter filterByLongCandle start!!");
        log.info("--- input size : "+input.size());

        Iterator<Stock> iterator = input.iterator();
        while(iterator.hasNext()) {
            Stock stock = iterator.next();
            ResponseEntity<?> response = commonApiClient.callStockNowPriceOver30(stock);
            ArrayList candles = (ArrayList)((JSONObject) response.getBody()).get("output2");

            //다음날 장대 양봉 or 장대 음봉 있을시 삭제
            if(!isNoLongCandleNextDays(candles)){
//                log.info("장대 양봉 or 음봉 존재 : "+stock.getStockName());
                iterator.remove();
            }
        }

        log.info("--- StockFilter filterByLongCandle end!!");
        log.info("--- input size : "+input.size());
        return input;
    }

    //
    public List<Stock> filterByAmountWhenDown(List<Stock> input){
        log.info("--- StockFilter filterByAmountWhenDown start!!");
        log.info("--- input size : "+input.size());

        Iterator<Stock> iterator = input.iterator();
        while(iterator.hasNext()) {
            Stock stock = iterator.next();
            ResponseEntity<?> response = commonApiClient.callStockNowPriceOver30(stock);
            ArrayList candles = (ArrayList)((JSONObject) response.getBody()).get("output2");

            //떨어지는 추세여도 거래량이 적어야함(급상승시거래량 대비 30% 이하 거래량)
            if(!isAmountSmallWhenDown(candles)){
//                log.info("하락추세 거래량 너무 큼 : "+stock.getStockName());
                iterator.remove();
            }
        }

        log.info("--- StockFilter filterByAmountWhenDown end!!");
        log.info("--- input size : "+input.size());
        return input;
    }

    //가격이 지지선이하로는 안떨어져야함 and
    public List<Stock> filterByBottomPrice(List<Stock> input){
        log.info("--- StockFilter filterByBottomPrice start!!");
        log.info("--- input size : "+input.size());

        Iterator<Stock> iterator = input.iterator();
        while(iterator.hasNext()) {
            Stock stock = iterator.next();
            ResponseEntity<?> response = commonApiClient.callStockNowPriceOver30(stock);
            ArrayList candles = (ArrayList)((JSONObject) response.getBody()).get("output2");

            //가격이 지지선이하로는 안떨어져야함
            if(!isOverBottomPrice(candles)){
//                log.info("지지선 이하로 떨어짐 : "+stock.getStockName());
                iterator.remove();
            }
        }

        log.info("--- StockFilter filterByBottomPrice end!!");
        log.info("--- input size : "+input.size());
        return input;
    }

    //1개월 누적순매수량이 기관이나 외인중 개인보다 커야함
    public List<Stock> filterByBuyPerson(List<Stock> input){
        log.info("--- StockFilter filterByBuyPerson start!!");
        log.info("--- input size : "+input.size());

        Iterator<Stock> iterator = input.iterator();
        while(iterator.hasNext()) {
            Stock stock = iterator.next();
            int type = 0;
            List<PersonTradeAmountEntity> dbCandles = new ArrayList<>();
            try {
                dbCandles = personTradeAmountRepo.findByStockCodeOrderByTimeAtAsc(stock.getStockShortCode());
                if(dbCandles.size() >= 30){
                    log.info("DB에서 투자자별 순매수 정보 호출 보유데이터 "+dbCandles.size()+"일");
                    type=1;
                }
            }
            catch (Exception e){
                log.error("투자자별 순매수 정보 DB 읽기에러"+e);
            }

            if(type==0){
                log.info("DB에서 투자자별 순매수 정보 읽기 불가, API 호출");
                ResponseEntity<?> response = commonApiClient.callStockBuyPerson(stock);
                ArrayList candles = (ArrayList)((JSONObject) response.getBody()).get("output");

                //1개월 누적순매수량이 기관이나 외인중 개인보다 커야함
                if(isNormalPersonBig(candles)){
                    iterator.remove();
                }
            }
            else {
                //1개월 누적순매수량이 기관이나 외인중 개인보다 커야함
                if(isNormalPersonBigDB(dbCandles)){
                    iterator.remove();
                }
            }

        }

        log.info("--- StockFilter filterByBuyPerson end!!");
        log.info("--- input size : "+input.size());
        return input;
    }

    //최근 상승 시점에서부터 이후에 눌림목 연속 3개이상
    public List<Stock> filterByStableShapeDays(List<Stock> input){
        log.info("--- StockFilter filterByStableShapeDays start!!");
        log.info("--- input size : "+input.size());

        Iterator<Stock> iterator = input.iterator();
        while(iterator.hasNext()) {
            Stock stock = iterator.next();

            ResponseEntity<?> response = commonApiClient.callStockNowPriceOver30(stock);
            ArrayList candles = (ArrayList)((JSONObject) response.getBody()).get("output2");

            //
            if(!isLongStableShape(candles)){
//                log.info("눌림목 일수 적음 : "+stock.getStockName());
                iterator.remove();
            }
        }

        log.info("--- StockFilter filterByStableShapeDays end!!");
        log.info("--- input size : "+input.size());
        return input;
    }

    private boolean isLongStableShape(ArrayList candles){
        LinkedHashMap pre = null;
        long topAmount = 0;
        for(int i=0; i<Math.min(CANDLE_RANGE_DAY,candles.size()-1); i++){
            LinkedHashMap json = (LinkedHashMap) candles.get(i);
            pre = (LinkedHashMap) candles.get(i+1);


            long prePrice = Long.parseLong((String) pre.get("stck_clpr"));
            long nowPrice = Long.parseLong((String)json.get("stck_clpr"));

            //전일종가보다 10%이상 상승했을때
            if((prePrice*upPriceRate <= nowPrice) ){
                int cnt = 0;
                boolean isGood = false;
                for(int j=i-1; j>=0; j--){
                    LinkedHashMap tempJson = (LinkedHashMap) candles.get(j);
                    if(isSmallCandle(tempJson, 0))
                        cnt++;
                    else
                        cnt=0;

                    if(cnt>=3) {
                        isGood = true;
                        break;
                    }
                }
                return isGood;
            }

        }
        return false;
    }

    private boolean isSmallCandle(LinkedHashMap c, double avgRange) {
        float range = Long.parseLong((String) c.get("stck_hgpr")) - Long.parseLong((String) c.get("stck_lwpr")); // 종가 - 시가
        float body = Math.abs(Long.parseLong((String) c.get("stck_clpr")) - Long.parseLong((String) c.get("stck_oprc")));// abs(종가 - 시가)

//        boolean smallBody = body / range < 0.3;  // 몸통이 작음
        boolean smallRange = (range / Long.parseLong((String) c.get("stck_clpr"))) <= 0.03; // 변동폭도 작음

        return smallRange;
    }

    //전일 종가 보다 10%이상 상승한적이 있을때 and 그때 거래량 동반할때
    private boolean isOverTenPercentAndAmountBig(ArrayList candles){
        LinkedHashMap pre = null;
        for(int i=0; i<Math.min(CANDLE_RANGE_DAY,candles.size()-1); i++){
            LinkedHashMap json = (LinkedHashMap) candles.get(i);
            pre = (LinkedHashMap) candles.get(i+1);

            long prePrice = Long.parseLong((String) pre.get("stck_clpr"));
            long nextPrice = Long.parseLong((String)json.get("stck_clpr"));

            //전일종가보다 10%이상 상승
            if(prePrice*upPriceRate <= nextPrice) {
                if(i+5>Math.min(CANDLE_RANGE_DAY,candles.size()-1)) return false;

                long amount = Long.parseLong((String) json.get("acml_vol"));
                long fiveDaysAmountSum = 0L;
                for(int j=i+1; j<=i+5; j++){
                    LinkedHashMap tempJson = (LinkedHashMap) candles.get(j);
                    fiveDaysAmountSum += Long.parseLong((String) tempJson.get("acml_vol"));
                }
                long fiveDaysAmountAvg = fiveDaysAmountSum / 5;
                if(fiveDaysAmountAvg*2 <= amount)
                    return true;
                else
                    return false;
            }
        }
        return false;
    }


    //다음날 연속으로 장대 양봉 or 장대 음봉 없어야함(전날 상승률의 50% 이상, 종가기준) and 상승한날 기준 현재가는 낮아야함
    //만족하는거 하나라도 있으면 true
    private boolean isNoLongCandleNextDays(ArrayList candles){
        LinkedHashMap pre = null;
        LinkedHashMap lastJson = (LinkedHashMap) candles.get(0);
        long lastPrice = Long.parseLong((String)lastJson.get("stck_clpr"));
        for(int i=1; i<Math.min(CANDLE_RANGE_DAY,candles.size()-1); i++){
            LinkedHashMap json = (LinkedHashMap) candles.get(i);
            pre = (LinkedHashMap) candles.get(i+1);

            long prePrice = Long.parseLong((String) pre.get("stck_clpr"));
            long nowPrice = Long.parseLong((String)json.get("stck_clpr"));

            //전일종가보다 10%이상 상승했을때 and 최근 가격이 상승했을때보단 낮아야함
            if((prePrice*upPriceRate <= nowPrice) && (nowPrice > lastPrice)){
                long nextPrice = Long.parseLong((String)((LinkedHashMap) candles.get(i-1)).get("stck_clpr"));
                //다음날 연속으로 장대 양봉 or 장대 음봉 없어야함(장대는 전날 상승률의 50%기준)
                if(Math.abs(prePrice-nowPrice)*0.5f > Math.abs(nowPrice-nextPrice))
                    return true;
                else
                    return false;
            }
        }
        return false;
    }

    //떨어지는 추세여도 급상승시거래량 대비 40% 이하 거래량
    //만족하는거 하나라도 있으면 true
    private boolean isAmountSmallWhenDown(ArrayList candles){
        LinkedHashMap pre = null;
        long topAmount = 0;
        for(int i=0; i<Math.min(CANDLE_RANGE_DAY,candles.size()-1); i++){
            LinkedHashMap now = (LinkedHashMap) candles.get(i);
            pre = (LinkedHashMap) candles.get(i+1);
            long prePrice = Long.parseLong((String) pre.get("stck_clpr"));
            long nowPrice = Long.parseLong((String)now.get("stck_clpr"));
            //전일종가보다 10%이상 상승했을때
            if((prePrice*upPriceRate <= nowPrice) ){
                topAmount = Long.parseLong((String)now.get("acml_vol"));
                for(int j=i-1; j>=0; j--){
                    LinkedHashMap json = (LinkedHashMap) candles.get(j);
                    long nowAmount = Long.parseLong((String)json.get("acml_vol"));
                    String nowSign = (String)json.get("prdy_vrss_sign");

                    //고점 거래량이 있을때, 하락추세인 거래량은 고점거래량의 40% 이하여야함
                    if((topAmount !=0) && ("4".equals(nowSign) || "5".equals(nowSign)) && (nowAmount > topAmount*0.4))
                        return false;
                }
                return true;
            }
        }
        return true;
    }

    //가격이 지지선이하로는 안떨어져야함(양봉 1개일때는 양봉 밑꼬리 기준, 양봉 1개 이상일때는 이전 바닥 최저점 기준)
    private boolean isOverBottomPrice(ArrayList candles){
        long bottomPrice = Long.MAX_VALUE;
        long topPrice = 0;
        int startIndex = -1;
        LinkedHashMap pre = null;
        //상승점 중에서 최저가 구하기
        for(int i=Math.min(CANDLE_RANGE_DAY,candles.size()-1); i>=0; i--){
            LinkedHashMap json = (LinkedHashMap) candles.get(i);

            if(pre == null){
                pre = json;
                continue;
            }

            long prePrice = Long.parseLong((String) pre.get("stck_clpr"));
            long nowPrice = Long.parseLong((String)json.get("stck_clpr"));

            //전일종가보다 10%이상 상승했을때
            if((prePrice*upPriceRate <= nowPrice) ){
                long nowLowPrice = Long.parseLong((String)json.get("stck_lwpr"));
                long nowTopPrice = Long.parseLong((String)json.get("stck_hgpr"));
                if(nowLowPrice < bottomPrice){
                    bottomPrice = nowLowPrice;
                    topPrice = nowTopPrice;
                    startIndex = i;
                }

            }

            pre = json;
        }

        for(int i=startIndex-1; i>=0; i--){
            LinkedHashMap json = (LinkedHashMap) candles.get(i);

            long nowPrice = Long.parseLong((String)json.get("stck_clpr"));

            //지지선 이하로 떨어지면
            if(bottomPrice > nowPrice){
                return false;
            }
        }

        //상단의 70% 이상이면 제외
//        LinkedHashMap json = (LinkedHashMap) candles.get(0);
//        long recentPrice = Long.parseLong((String) json.get("stck_clpr"));
//        if(((topPrice-bottomPrice)*0.7+bottomPrice) < recentPrice) {
//            System.out.println((topPrice-bottomPrice)*0.7+bottomPrice+" and "+recentPrice);
//            return false;
//        }

        return true;
    }

    //개인투자자 비중이 클때
    //누구든 개인보다 많은사람 존재할때 필터링에서 제외
    //캔들이 최대 30개 제약사항 있음
    private boolean isNormalPersonBig(ArrayList candles){
        int personSum10 = 0;
        int personSum30 = 0;
        int foreignPersonSum10 = 0;
        int foreignPersonSum30 = 0;
        int agencyPersonSum10 = 0;
        int agencyPersonSum30 = 0;

        ArrayList<Integer> foreignPerson10SumList = new ArrayList<>();
        ArrayList<Integer> foreignPerson30SumList = new ArrayList<>();
        ArrayList<Integer> agencyPerson10SumList = new ArrayList<>();
        ArrayList<Integer> agencyPerson30SumList = new ArrayList<>();



        //10일, 30일치 비교하기
        final int CANDLE_MAX_SIZE = Math.min(29,candles.size()-1);
        for(int i=CANDLE_MAX_SIZE; i>=0; i--){
            LinkedHashMap json = (LinkedHashMap) candles.get(i);
            //당일 데이터는 ""이므로 예외처리
            String str1 = (String)json.get("prsn_ntby_qty");
            String str2 = (String)json.get("frgn_ntby_qty");
            String str3 = (String)json.get("orgn_ntby_qty");
            if(!str1.equals("") && !str2.equals("") && !str3.equals("")) {
                if(i<Math.max(CANDLE_MAX_SIZE-19,10)) {
                    personSum10 += Long.parseLong((String) json.get("prsn_ntby_qty"));
                    foreignPersonSum10 += Long.parseLong((String) json.get("frgn_ntby_qty"));
                    agencyPersonSum10 += Long.parseLong((String) json.get("orgn_ntby_qty"));

                    foreignPerson10SumList.add(foreignPersonSum10);
                    agencyPerson10SumList.add(agencyPersonSum10);
                }
                personSum30 += Long.parseLong((String) json.get("prsn_ntby_qty"));
                foreignPersonSum30 += Long.parseLong((String) json.get("frgn_ntby_qty"));
                agencyPersonSum30 += Long.parseLong((String) json.get("orgn_ntby_qty"));

                foreignPerson30SumList.add(foreignPersonSum30);
                agencyPerson30SumList.add(agencyPersonSum30);
            }
        }

//        double foreignSlope10 = calculateSlope(foreignPerson10SumList.toArray(new Integer[0]));
        double foreignSlope30 = calculateSlope(foreignPerson30SumList.toArray(new Integer[0]));
//        double agencySlope10 = calculateSlope(agencyPerson10SumList.toArray(new Integer[0]));
        double agencySlope30 = calculateSlope(agencyPerson30SumList.toArray(new Integer[0]));

        //기관, 외인중 누적매수량이 0이상이고 개인보다 큰게 존재하면 필터링 제외하기
        if((foreignPersonSum30 > 0 && personSum30 < foreignPersonSum30) && foreignSlope30 >= 0){
            return false;
        }

        if((agencyPersonSum30 > 0 && personSum30 < agencyPersonSum30) && agencySlope30 >= 0) {
            return false;
        }

//        if((foreignPersonSum30 > 0 && agencyPersonSum30 > 0 && personSum30 < foreignPersonSum30 && personSum30 < agencyPersonSum30))
//            return false;


        return true;
    }

    //DB에서 불러올떄 이함수씀
    //개인투자자 비중이 클때
    //누구든 개인보다 많은사람 존재할때 필터링에서 제외
    //캔들이 최대 30개 제약사항 있음
    private boolean isNormalPersonBigDB(List<PersonTradeAmountEntity> dbCandles){
        int personSum60 = 0;
        int foreignPersonSum60 = 0;
        int agencyPersonSum60 = 0;

        ArrayList<Integer> foreignPerson30SumList = new ArrayList<>();
        ArrayList<Integer> agencyPerson30SumList = new ArrayList<>();


        //최대 60일로 변경
        final int CANDLE_MAX_SIZE = Math.min(59,dbCandles.size()-1);
        for(int i=CANDLE_MAX_SIZE; i>=0; i--){
            PersonTradeAmountEntity entity = dbCandles.get(i);

            personSum60 += entity.getPersonBuyAmount();
            foreignPersonSum60 += entity.getForeignBuyAmount();
            agencyPersonSum60 += entity.getAgencyBuyAmount();

            foreignPerson30SumList.add(foreignPersonSum60);
            agencyPerson30SumList.add(agencyPersonSum60);
        }

        double foreignSlope30 = calculateSlope(foreignPerson30SumList.toArray(new Integer[0]));
        double agencySlope30 = calculateSlope(agencyPerson30SumList.toArray(new Integer[0]));

        //기관, 외인중 누적매수량이 0이상이고 개인보다 큰게 존재하면 필터링 제외하기
        if((foreignPersonSum60 > 0 && personSum60 < foreignPersonSum60) && foreignSlope30 >= 0){
            return false;
        }

        if((agencyPersonSum60 > 0 && personSum60 < agencyPersonSum60) && agencySlope30 >= 0) {
            return false;
        }

        return true;
    }

    private double calculateSlope(Integer[] yValues) {
        int n = yValues.length;
        if (n == 0) return 0.0;

        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumX2 = 0;

        for (int i = 0; i < n; i++) {
            int x = i + 1; // x축: 1일부터 시작
            int y = yValues[i];

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double numerator = (n * sumXY) - (sumX * sumY);
        double denominator = (n * sumX2) - (sumX * sumX);

        if (denominator == 0) return 0.0; // 0으로 나눌 수 없으니 예외 처리

        return numerator / denominator;
    }
}
