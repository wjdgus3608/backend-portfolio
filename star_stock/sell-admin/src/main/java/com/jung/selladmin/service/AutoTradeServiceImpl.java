package com.jung.selladmin.service;

import com.jung.common.util.TimeUtil;
import com.jung.domain.order.OrderStockReqDTO;
import com.jung.domain.order.OrderType;
import com.jung.domain.stock.Stock;
import com.jung.kisclient.CommonApiClient;
import com.jung.selladmin.dto.RetrieveStockRtnDTO;
import com.jung.selladmin.dto.SaveStockStatusReqDTO;
import com.jung.selladmin.repo.AutoTradeStockRepo;
import com.jung.selladmin.repo.SellHistoryRepo;
import com.jung.selladmin.vo.AutoTradeStock;
import com.jung.selladmin.vo.SellType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoTradeServiceImpl implements AutoTradeService{

    private final AutoTradeStockRepo autoTradeStockRepo;
    private final SellHistoryRepo sellHistoryRepo;
    private final CommonApiClient commonApiClient;

    private static String opndYn = "N";

    @Value("${my.acno}")
    private String acno;

    @Value("${my.upPriceRate}")
    private float upPriceRate;

    private final int CANDEL_RANGE_DAY = 60;


    // 최초 1회 실행
    @PostConstruct
    public void runOnStartup() {
        updateMarketOpened();
    }

    @Scheduled(fixedDelay = 5000)
    public void runScheduledAutoTrade() {
        try {
            runAutoTrade();
        } catch (Exception e) {
            log.error("runAutoTrade 예외 발생", e);
        }
    }

    @Override
    @Transactional
    public void runAutoTrade() {
        if(opndYn.equals("Y") && isBetween9And3half()){
            List<AutoTradeStock> stockList = callMyBox();
            updateToDB(stockList);
            monitorSellPlan();
        }
    }

    @Override
    @Transactional
    public List<RetrieveStockRtnDTO> retrieveAccount() {
        List<AutoTradeStock> stockList = callMyBox();
        updateToDB(stockList);
        return getAccountFromDB();
    }

    @Override
    @Transactional
    public void saveAccountStatus(List<SaveStockStatusReqDTO> input) {
        saveStatusToDB(input);
    }

    private List<AutoTradeStock> callMyBox(){
        List<AutoTradeStock> stockList = new ArrayList<>();

        ResponseEntity<?> response = commonApiClient.callMyBox(acno);
        ArrayList<LinkedHashMap> list = ((ArrayList)((JSONObject) response.getBody()).get("output1"));

        for(LinkedHashMap map : list){
            //당일 매도한거 API에 남아있으므로 제거
            long remainAmount = Long.parseLong((String)map.get("hldg_qty"));
            if(remainAmount == 0) continue;

            String code = (String)map.get("pdno");
            String name = (String)map.get("prdt_name");
            float price = Float.parseFloat((String)map.get("pchs_avg_pric"));
            AutoTradeStock autoTradeStock = AutoTradeStock.builder()
                    .stockShortCode(code)
                    .stockName(name)
                    .myPrice(Math.round(price))
                    .build();

            stockList.add(autoTradeStock);
        }
        return stockList;
    }

    public void updateToDB(List<AutoTradeStock> list){
        //신규 매수종목 업데이트
        for(AutoTradeStock stock : list){
            if(!autoTradeStockRepo.existsById(stock.getStockShortCode())){
                stock.setBuyTimeAt(TimeUtil.getToday()+TimeUtil.getCurrentTimeHHmmss());
                makeSellPlan(stock);
                autoTradeStockRepo.save(stock);
            }
            //매수평균가 달라지면 업데이트
            else if(autoTradeStockRepo.findById(stock.getStockShortCode()).get().getMyPrice() != stock.getMyPrice()){
                stock.setBuyTimeAt(TimeUtil.getToday()+TimeUtil.getCurrentTimeHHmmss());
                makeSellPlan(stock);
                autoTradeStockRepo.save(stock);
            }
        }

        //이미 매도종목 삭제하기
        List<AutoTradeStock> dbStockList = autoTradeStockRepo.findAll();
        for(AutoTradeStock stock : dbStockList){
            if(!list.contains(stock)){
                autoTradeStockRepo.delete(stock);
            }
        }
    }

    private List<RetrieveStockRtnDTO> getAccountFromDB(){
        List<AutoTradeStock> dbStockList = autoTradeStockRepo.findAll();
        List<RetrieveStockRtnDTO> resultList = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("#,###");
        for(AutoTradeStock stock : dbStockList){

            RetrieveStockRtnDTO dto = RetrieveStockRtnDTO.builder()
                    .stockShortCode(stock.getStockShortCode())
                    .stockName(stock.getStockName())
                    .autoSellActive(stock.isAutoSellActive())
                    .myPrice(df.format(Math.round(stock.getMyPrice()))+"원")
                    .buyTimeAt(stock.getBuyTimeAt())
                    .sellType(stock.getSellType().name())
                    .sellDesc(stock.getSellDesc())
                    .futureGainPrice(df.format(Math.round(stock.getFutureGainPrice()))+"원")
                    .futurePainPrice(df.format(Math.round(stock.getFuturePainPrice()))+"원")
                    .futureGainRate(stock.getFutureGainRate())
                    .futurePainRate(stock.getFuturePainRate())
                    .build();

            resultList.add(dto);
        }

        return resultList;
    }


    public void saveStatusToDB(List<SaveStockStatusReqDTO> input){
        for (SaveStockStatusReqDTO req : input) {
            autoTradeStockRepo.findById(req.getStockShortCode())
                    .ifPresent(stock -> stock.setAutoSellActive(req.isAutoSellActive()));
        }
    }

    private void makeSellPlan(AutoTradeStock stock){
        Stock input = new Stock();
        input.setStockShortCode(stock.getStockShortCode());
        ResponseEntity<?> response = commonApiClient.callStockNowPriceOver30(input);
        ArrayList candles = (ArrayList)((JSONObject) response.getBody()).get("output2");

        int topLine = getTopLine(candles, stock);
        int bottomLine = getBottomLine(candles, stock);

        String type1 = getFirstType(candles, stock);
        String type2 = getSecondType(stock, topLine, bottomLine);
        SellType sellType = SellType.valueOf(type1+type2);

        stock.setSellType(sellType);
        stock.setSellDesc(sellType.getDesc());

        float x = 0.05f;
        float gainSellPrice;
        float painSellPrice;
        switch (sellType){
            case AA:
            case AB:
                stock.setFutureGainPrice(stock.getMyPrice()*(1+x));
                stock.setFutureGainRate(x);

                if(stock.getMyPrice()*(1-x-0.01f) > bottomLine)
                    painSellPrice = stock.getMyPrice()*(1-x);
                else
                    painSellPrice = Math.min(stock.getMyPrice()*(1-x), bottomLine);
                stock.setFuturePainPrice(painSellPrice);
                stock.setFuturePainRate(-(1-painSellPrice/stock.getMyPrice()));
                break;
            case AC:
                stock.setFutureGainPrice(stock.getMyPrice()*(1+x));
                stock.setFutureGainRate(x);

                stock.setFuturePainPrice(stock.getMyPrice()*(1-x/2-0.01f));
                stock.setFuturePainRate(-(x/2+0.01f));
                break;
            case BA:
            case CA:
                stock.setFutureGainPrice(stock.getMyPrice()*(1+x));
                stock.setFutureGainRate(x);

                if(stock.getMyPrice()*(1-x/2-0.01f) > bottomLine)
                    painSellPrice = stock.getMyPrice()*(1-x/2);
                else
                    painSellPrice = Math.min(stock.getMyPrice()*(1-x/2), bottomLine);
                stock.setFuturePainPrice(painSellPrice);
                stock.setFuturePainRate(-(1-painSellPrice/stock.getMyPrice()));
                break;
            case BB:
            case CB:
                gainSellPrice = Math.min(stock.getMyPrice()*(1+x), topLine);
                stock.setFutureGainPrice(gainSellPrice);
                stock.setFutureGainRate((gainSellPrice/stock.getMyPrice()-1));

                if(stock.getMyPrice()*(1-x-0.01f) > bottomLine)
                    painSellPrice = stock.getMyPrice()*(1-x);
                else
                    painSellPrice = Math.min(stock.getMyPrice()*(1-x), bottomLine);
                stock.setFuturePainPrice(painSellPrice);
                stock.setFuturePainRate(-(1-painSellPrice/stock.getMyPrice()));
                break;
            case BC:
                gainSellPrice = Math.min(stock.getMyPrice()*(1+x), topLine);
                stock.setFutureGainPrice(gainSellPrice);
                stock.setFutureGainRate((gainSellPrice/stock.getMyPrice()-1));

                stock.setFuturePainPrice(stock.getMyPrice()*(1-x));
                stock.setFuturePainRate(-x);
                break;
            case CC:
                gainSellPrice = Math.min(stock.getMyPrice()*(1+x), topLine);
                stock.setFutureGainPrice(gainSellPrice);
                stock.setFutureGainRate((gainSellPrice/stock.getMyPrice()-1));

                stock.setFuturePainPrice(stock.getMyPrice()*(1-x/2));
                stock.setFuturePainRate(-x/2);
                break;

        }

    }

    private String getFirstType(ArrayList candles, AutoTradeStock stock){
        int baseIndex = findFirstBaseRedCandle(candles);

        if(baseIndex == -1)
            return "C";

        LinkedHashMap baseCandle = (LinkedHashMap) candles.get(baseIndex);
        long endPrice = Long.parseLong((String) baseCandle.get("stck_clpr"));
        long lowPrice = Long.parseLong((String) baseCandle.get("stck_lwpr"));

        float locateValue =  (float)(stock.getMyPrice() - lowPrice) / (endPrice - lowPrice);

        if(locateValue >= 0.7f)
            return "A";
        else if (locateValue >= 0.3f)
            return "B";
        else
            return "C";
    }

    private int findFirstBaseRedCandle(ArrayList candles){
        LinkedHashMap json;
        LinkedHashMap pre;
        for(int i=0; i<Math.min(candles.size()-1, CANDEL_RANGE_DAY-1); i++){
            json = (LinkedHashMap) candles.get(i);
            pre = (LinkedHashMap) candles.get(i+1);

            long prePrice = Long.parseLong((String) pre.get("stck_clpr"));
            long nextPrice = Long.parseLong((String)json.get("stck_clpr"));

            if(prePrice*upPriceRate <= nextPrice) {
                return i;
            }
        }

        return -1;
    }

    private String getSecondType(AutoTradeStock stock, int topLine, int bottomLine){
        log.info("top line : "+topLine);
        log.info("bottom line : "+bottomLine);
        float locateValue =  (float)(stock.getMyPrice() - bottomLine) / (topLine - bottomLine);
        if(locateValue >= 0.7f)
            return "A";
        else if (locateValue >= 0.3f)
            return "B";
        else
            return "C";
    }

    private int getTopLine(ArrayList candles, AutoTradeStock stock){
        int dayCnt = 0;
        int topPrice = 0;
        LinkedHashMap json;
        for(int i=0; i<Math.min(candles.size(), CANDEL_RANGE_DAY); i++){
            json = (LinkedHashMap) candles.get(i);
            String timeStr = (String) json.get("stck_bsop_date");
            if(TimeUtil.isAfterOrEqual2(timeStr, stock.getBuyTimeAt().substring(0,8))) continue;
            int price = Integer.parseInt((String) json.get("stck_hgpr"));
            topPrice = Math.max(topPrice, price);
            dayCnt++;
            if(dayCnt==5){
                if(topPrice>=(stock.getMyPrice()*1.02f))
                    return topPrice;
            }
            else if(dayCnt==10){
                return topPrice;
            }
        }
        return topPrice;
    }

    private int getBottomLine(ArrayList candles, AutoTradeStock stock){
        int dayCnt = 0;
        int bottomPrice = Integer.MAX_VALUE;
        LinkedHashMap json;
        for(int i=0; i<Math.min(candles.size(), CANDEL_RANGE_DAY); i++){
            json = (LinkedHashMap) candles.get(i);
            String timeStr = (String) json.get("stck_bsop_date");
            if(TimeUtil.isAfterOrEqual2(timeStr, stock.getBuyTimeAt().substring(0,8))) continue;
            int price = Integer.parseInt((String) json.get("stck_lwpr"));
            bottomPrice = Math.min(bottomPrice, price);
            dayCnt++;
            if(dayCnt==10){
                if(bottomPrice<=(stock.getMyPrice()*0.98f))
                    return bottomPrice;
            }
            else if(dayCnt==15){
                return bottomPrice;
            }
        }
        return bottomPrice;
    }


    @Transactional
    public void monitorSellPlan(){
        List<AutoTradeStock> dbStockList = autoTradeStockRepo.findAll();
        for(AutoTradeStock autoTradeStock : dbStockList){
            if(!autoTradeStock.isAutoSellActive()) continue;

            if(isSellPlanTrue(autoTradeStock)){
                Stock input = new Stock();
                input.setStockShortCode(autoTradeStock.getStockShortCode());
                if(autoTradeStock.getFutureGainRate() != 0.1f && isTradeAmountBig(input)) {
                    sellStock(input, com.jung.domain.order.SellType.HALF);
                    autoTradeStock.setFutureGainPrice(autoTradeStock.getMyPrice()*1.1f);
                    autoTradeStock.setFutureGainRate(0.1f);
                }
                else
                    sellStock(input, com.jung.domain.order.SellType.ALL);
            }
        }
    }

    private boolean isTradeAmountBig(Stock input){

        ResponseEntity<?> response = commonApiClient.callStockDailyPrice(input);
        ArrayList<LinkedHashMap> list = ((ArrayList)((JSONObject) response.getBody()).get("output"));
        if(list == null || list.size()<6) return false;

        long nowAmount;
        String nowVol = (String)((LinkedHashMap) list.get(0)).get("acml_vol");
        if(nowVol == null || nowVol.equals("")) nowAmount = 0;
        else nowAmount = Long.parseLong(nowVol);
        long sum = 0L;
        for (int i=1; i<=5; i++){
            LinkedHashMap map = (LinkedHashMap) list.get(i);
            String vol = (String)map.get("acml_vol");
            long amount;
            if(vol == null || vol.equals("")) amount = 0;
            else
                amount = Long.parseLong(vol);
            sum+= amount;
        }

        return (nowAmount/hoursSince9AM() * 6 * 0.8f) >= (sum/5*2);
    }

    public static int hoursSince9AM() {
        // 현재 시간 가져오기
        LocalTime now = LocalTime.now();

        // 기준 시간: 오전 9시
        LocalTime nineAM = LocalTime.of(9, 0);

        // 9시 이후면 시간 차이 계산
        if (now.isAfter(nineAM)) {
            int hours = (int) java.time.Duration.between(nineAM, now).toHours();
            return Math.max(hours, 1); // 0이면 1로
        }

        // 9시 이전이면 1 반환
        return 1;
    }

    private boolean isSellPlanTrue(AutoTradeStock autoTradeStock){
        Stock input = new Stock();
        input.setStockShortCode(autoTradeStock.getStockShortCode());
        ResponseEntity<?> response = commonApiClient.callStockNowPrice(input);
        LinkedHashMap body = (LinkedHashMap)((JSONObject) response.getBody()).get("output");
        long nowPrice = Long.parseLong((String) body.get("stck_prpr"));

        if(autoTradeStock.getFutureGainPrice() <= nowPrice) {
            log.info("nowPrice over FutureGainPrice : "+autoTradeStock.getFutureGainPrice()+" <= "+nowPrice);
            return true;
        }
        if(autoTradeStock.getFuturePainPrice() >= nowPrice) {
            log.info("nowPrice under FuturePainPrice : "+autoTradeStock.getFuturePainPrice()+" >= "+nowPrice);
            return true;
        }

        return false;
    }

    private void sellStock(Stock input, com.jung.domain.order.SellType sellType){
        ResponseEntity<?> sellPossibleResponse = commonApiClient.callSellPossibleStockAmount(acno, input.getStockShortCode());
        LinkedHashMap responseMap = ((LinkedHashMap)((JSONObject) sellPossibleResponse.getBody()).get("output"));

        String qty = ((String)responseMap.get("ord_psbl_qty"));
        if(qty == null || qty.equals("0")) return;

        String amount;
        if(sellType.equals(com.jung.domain.order.SellType.HALF)) {
            int temp = (Integer.parseInt(qty) / 2);
            amount = (temp == 0 ? "1" : Integer.toString(temp));
        }
        else{
            amount = qty;
        }


        OrderStockReqDTO reqDTO = OrderStockReqDTO.builder()
                .acno(acno)
                .stockShortCode(input.getStockShortCode())
                .amount(amount)
                .orderType(OrderType.SELL)
                .build();

        commonApiClient.callOrderStock(reqDTO);
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void updateMarketOpened(){
        ResponseEntity<?> response = commonApiClient.callMarketDayInfo();
        String todayStr = TimeUtil.getToday();
        ArrayList<LinkedHashMap> list = ((ArrayList)((JSONObject) response.getBody()).get("output"));
        for(LinkedHashMap map : list){
            if(((String)map.get("bass_dt")).equals(todayStr)){
                opndYn = (String)map.get("opnd_yn");
                break;
            }
        }
        if(opndYn.equals("Y")){
            log.info("주식시장 오픈날입니다.");
        }
        else{
            log.info("주식시장 휴장날입니다.");
        }
    }

    public boolean isBetween9And3half() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(9, 0);  // 오전 9시
        LocalTime end = LocalTime.of(15, 30);   // 오후 3:30
//        if((!now.isBefore(start) && !now.isAfter(end))==false) log.info("시간 안맞음");
        return !now.isBefore(start) && !now.isAfter(end);
    }

}
