package jo.jung.backteston.service.signal;

import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.backteston.util.IndicatorUtil;
import jo.jung.common.priceutil.PriceUtil;
import jo.jung.domain.candle.Candle;
import jo.jung.domain.graph.Fvg;
import jo.jung.domain.graph.OrderBlock;
import jo.jung.domain.trade.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static jo.jung.backteston.util.IndicatorUtil.getFVG;

@Slf4j
@Component
@RequiredArgsConstructor
public class SellSignalImpl implements SellSignal{
    @Override
    public Map<String, Function<CustomParam, Long>> getAllSignalFunctions() {
        Map<String, Function<CustomParam, Long>> map = new LinkedHashMap<>();
        //@@map.put("1.isOverNPercent", this::isOverNPercent);
//        map.put("2.isOverNPercent15And05", this::isOverNPercent15And05);
        //@@map.put("3.isTopBollingTouch", this::isTopBollingTouch);
//        map.put("4.isBollingDiffTouch", this::isBollingDiffTouch);
        map.put("5.isRsiTurnDownOrDoubleBlueCandle", this::isRsiTurnDownOrDoubleBlueCandle);
//        map.put("6.isDownCompareTopWithRsiUnder", this::isDownCompareTopWithRsiUnder);
//        map.put("7.isCloseUnderEmaWithDoubleBlueCandle", this::isCloseUnderEmaWithDoubleBlueCandle);
        //@@map.put("8.isTSG", this::isTSG);
        //@@map.put("9.isFvgSell", this::isFvgSell);
        map.put("10.isOrderBlockSell", this::isOrderBlockSell);
        //@@map.put("11.isOrderBlockAndFvgSell", this::isOrderBlockAndFvgSell);
//        map.put("12.isFiveOrderBlockSell", this::isFiveOrderBlockSell);
//        map.put("13.isFifteenOrderBlockSell", this::isFifteenOrderBlockSell);
        return map;
    }


    //1.isOverNPercent
    public long isOverNPercent(CustomParam param){
        Trade myStock = param.getMyStock();
        List<MinuteCandleEntity> candles = param.getCandles();
        float n = param.getN();
        float n2 = param.getPainN();

        long myPrice = myStock.getMyPrice();
        int candleIdx = candles.size()-1;
        MinuteCandleEntity candle = candles.get(candleIdx);
        long topPrice = candle.getTopPrice();
        long bottomPrice = candle.getBottomPrice();

        //N% 익절
        if(myPrice*(1+n) <= topPrice){
//            log.info("N% 익절조건 만족");
            return ((Float)(myPrice*(1+n))).longValue();
        }

        //N% 손절
        if(myPrice*(1-n2) >= bottomPrice){
//            log.info("N% 손절조건 만족");
            return ((Float)(myPrice*(1-n2))).longValue();
        }
        return 0;
    }

    //2.isOverNPercent15And05
    public long isOverNPercent15And05(CustomParam param){
        Trade myStock = param.getMyStock();
        List<MinuteCandleEntity> candles = param.getCandles();
        float n = 0.015f;
        float n2 = 0.005f;

        long myPrice = myStock.getMyPrice();
        int candleIdx = candles.size()-1;
        MinuteCandleEntity candle = candles.get(candleIdx);
        long topPrice = candle.getTopPrice();
        long bottomPrice = candle.getBottomPrice();

        //N% 익절
        if(myPrice*(1+n) <= topPrice){
//            log.info("N% 익절조건 만족");
            return ((Float)(myPrice*(1+n))).longValue();
        }

        //N% 손절
        if(myPrice*(1-n2) >= bottomPrice){
//            log.info("N% 손절조건 만족");
            return ((Float)(myPrice*(1-n2))).longValue();
        }
        return 0;
    }

    //3.isTopBollingTouch
    public long isTopBollingTouch(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        Trade myStock = param.getMyStock();

        int index = candles.size()-1;
        if(index<=0) return 0;
        Tuple3<Double, Double, Double> bands = IndicatorUtil.getBollingerBands(candles.subList(0, index), 20, 2.0);
        long topValue = (bands.getT1()).longValue();
        long price = candles.get(index).getTopPrice();
        long lowPrice = candles.get(index).getBottomPrice();
        if(topValue <= price && PriceUtil.getEarnMoney(myStock.getMyPrice(), topValue, myStock.getAmount()) > 0){
//            log.info("볼린저 상단터치");
            return topValue;
        }
        else if((myStock.getMyPrice()*(1-0.005f))>=lowPrice){
            return ((Float)(myStock.getMyPrice()*(1-0.005f))).longValue();
        }
        return 0;
    }

    //4.isBollingDiffTouch
    public long isBollingDiffTouch(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        Trade myStock = param.getMyStock();

        int index = candles.size()-1;
        if(index<=0) return 0;
        Tuple3<Double, Double, Double> bands = IndicatorUtil.getBollingerBands(candles.subList(0, index), 20, 2.0);
        long topValue = (bands.getT1()).longValue();
        long midValue = (bands.getT2()).longValue();
        long diffValue = topValue - midValue;
        long price = candles.get(index).getTopPrice();
        long lowPrice = candles.get(index).getBottomPrice();
        if((myStock.getMyPrice()+diffValue*2) <= price && PriceUtil.getEarnMoney(myStock.getMyPrice(), (myStock.getMyPrice()+diffValue*2), myStock.getAmount()) > 0){
//            log.info("볼린저 상단터치");
            return (myStock.getMyPrice()+diffValue*2);
        }
        else if((myStock.getMyPrice()-diffValue)>=lowPrice){
            return (myStock.getMyPrice()-diffValue);
        }
        return 0;
    }



    public long isMidBollingTouch(List<MinuteCandleEntity> candles){
        int index = candles.size()-1;
        if(index<=0) return 0;
        Tuple3<Double, Double, Double> bands = IndicatorUtil.getBollingerBands(candles.subList(0, index + 1), 20, 2.0);
        long midValue = (bands.getT2()).longValue();
        long price = candles.get(index).getTopPrice();
        if(midValue <= price){
            log.info("볼린저 중앙터치");
            return midValue;
        }
        return 0;
    }

    //RSI 고점탐색은 20봉, 가격고점은 15봉 범위로
    //RSI 하락전환 or 2연속 음봉

    //5.isRsiTurnDownOrDoubleBlueCandle
    public long isRsiTurnDownOrDoubleBlueCandle(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        Trade myStock = param.getMyStock();
        List<Integer> rsiList = IndicatorUtil.getRSIList(candles.subList(0, candles.size() - 1));
        MinuteCandleEntity lastCandle = candles.getLast();
        long startPrice = lastCandle.getStartPrice();

        boolean isRsiBelowBy2Ticks = isRsiBelowBy2Ticks(rsiList);
        if(isRsiBelowBy2Ticks){
            if(!myStock.getTimeAt().equals(lastCandle.getTimeAt()))
                return startPrice;
            else
                return lastCandle.getEndPrice();
        }

        int index = candles.size()-1;
        if(index-2 < 0) return 0;

        MinuteCandleEntity candle1 = candles.get(index-2);
        MinuteCandleEntity candle2 = candles.get(index-1);

        if(candle1.getStartPrice()>candle1.getEndPrice() &&
                candle2.getStartPrice()>candle2.getEndPrice()){
            if(!myStock.getTimeAt().equals(lastCandle.getTimeAt()))
                return startPrice;
            else
                return lastCandle.getEndPrice();
        }
        return 0;
    }

    private boolean isRsiBelowBy2Ticks(List<Integer> rsiList) {
        if (rsiList == null || rsiList.size() < 2) {
            return false; // 데이터가 부족하면 판단 불가
        }

        // 최근 20개 데이터만 추출
        int size = rsiList.size();
        int startIndex = Math.max(0, size - 20);
        List<Integer> recent20 = rsiList.subList(startIndex, size);

        // 최근 20개 중 최고 RSI 구하기
        int maxRsi = recent20.stream()
                .max(Integer::compareTo)
                .orElse(rsiList.get(size - 1));

        // 현재 RSI (리스트 마지막 값)
        int currentRsi = rsiList.get(size - 1);

        // 차이 계산
        int diff = maxRsi - currentRsi;

        // 2틱(2포인트) 이상 떨어졌는지 확인
        return diff >= 2;
    }

    //최고가 대비 -0.5%이상 하락 and RSI < 65
    //6.isDownCompareTopWithRsiUnder
    public long isDownCompareTopWithRsiUnder(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        int rsi = IndicatorUtil.getRSI(candles.subList(0, candles.size() - 1));

        if(isLastCandleDownByHalfPercent(candles.subList(0, candles.size()-1)) && rsi <65){
            return candles.getLast().getStartPrice();
        }

        return 0;
    }

    private boolean isLastCandleDownByHalfPercent(List<MinuteCandleEntity> candles) {
        if (candles == null || candles.size() < 2) {
            return false; // 데이터 부족
        }

        int size = candles.size();
        int startIndex = Math.max(0, size - 15);
        List<MinuteCandleEntity> recent15 = candles.subList(startIndex, size);

        // 최근 15개 봉 중 최고가 구하기
        double maxHigh = recent15.stream()
                .mapToDouble(MinuteCandleEntity::getTopPrice)
                .max()
                .orElse(Double.NaN);

        // 마지막 봉 종가
        double lastClose = candles.get(size - 1).getEndPrice();

        // 하락률 계산
        double dropRate = (maxHigh - lastClose) / maxHigh;

        // 0.5% (0.005) 이상 하락했는지
        return dropRate >= 0.005;
    }

    //종가<ema(5) and 2연속 음봉
    //7.isCloseUnderEmaWithDoubleBlueCandle
    public long isCloseUnderEmaWithDoubleBlueCandle(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        long startPrice = candles.getLast().getStartPrice();

        long preEndPrice = candles.get(candles.size() - 2).getEndPrice();
        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (int i=0; i<candles.size()-1; i++) {
            MinuteCandleEntity candle = candles.get(i);
            closes.add((double) candle.getEndPrice());
        }
        double ema = IndicatorUtil.calculateEMA(closes, candles.size() - 2, 5);

        if(preEndPrice>=ema)
            return 0;

        int index = candles.size()-1;
        if(index-2 < 0) return 0;

        MinuteCandleEntity candle1 = candles.get(index-2);
        MinuteCandleEntity candle2 = candles.get(index-1);

        if(candle1.getStartPrice()>candle1.getEndPrice() &&
                candle2.getStartPrice()>candle2.getEndPrice()){
            return startPrice;
        }
        return 0;
    }

    //8.isTSG
    public long isTSG(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (int i=0; i<candles.size()-1; i++) {
            MinuteCandleEntity candle = candles.get(i);
            closes.add((double)candle.getEndPrice());
        }

        List<Double> HSMAList = IndicatorUtil.calculateHSMA(closes, 20);
        boolean isCrossNow = isHSMAZeroCrossNow(HSMAList);
        if(isCrossNow){
            return candles.getLast().getStartPrice();
        }
        return 0;
    }

    // HSMA 음->양 전환 여부 확인 (현재 시점 기준)
    private boolean isHSMAZeroCrossNow(List<Double> hsmaList) {
        int n = hsmaList.size();
        if (n < 2) return false; // 데이터가 부족하면 false

        double prev = hsmaList.get(n - 2);
        double curr = hsmaList.get(n - 1);

        return prev >= 0 && curr < 0; // 양->음 전환 시 true
    }

    //9.isFvgMiddleTouch
    public long isFvgSell(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        Trade myStock = param.getMyStock();
        Fvg fvg = IndicatorUtil.getFVG(candles.subList(0, candles.size() - 1), 30);
        if(fvg==null)
            return 0;

        int size = candles.size()-1;
        int startIndex = Math.max(0, size - 15);
        List<MinuteCandleEntity> recent15 = candles.subList(startIndex, size);

        // 최근 15개 봉 중 최고가 구하기
        double maxHigh = recent15.stream()
                .mapToDouble(MinuteCandleEntity::getTopPrice)
                .max()
                .orElse(Double.NaN);

        long sellPrice = ((Double)maxHigh).longValue();
        MinuteCandleEntity fvgPreCandle = candles.get(fvg.getCreatedIndex()-2);

        if(sellPrice <=candles.getLast().getTopPrice() && (PriceUtil.getEarnMoney(myStock.getMyPrice(), sellPrice, myStock.getAmount())>0)){
            return sellPrice;
        }
        else if(fvgPreCandle.getBottomPrice() >= candles.getLast().getBottomPrice()){
            return fvgPreCandle.getBottomPrice();
        }

        return 0;
    }

    //10.isOrderBlockSell
    public long isOrderBlockSell(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        MinuteCandleEntity lastCandle = candles.getLast();
        String timeStr = param.getMyStock().getTimeAt();
        List<MinuteCandleEntity> copyCandles = new ArrayList<>();
        for(int i=0; i<candles.size()-1; i++){
            MinuteCandleEntity candle = candles.get(i);
            if(candle.getTimeAt().equals(timeStr)) break;

            copyCandles.add(candle);
        }
        OrderBlock orderBlock = IndicatorUtil.getOrderBlock(copyCandles);
        if(orderBlock==null) return 0;

        int size = candles.size()-1;
        int startIndex = Math.max(0, size - 15);
        List<MinuteCandleEntity> recent15 = candles.subList(startIndex, size);

        // 최근 15개 봉 중 최고가 구하기
        double maxHigh = recent15.stream()
                .mapToDouble(MinuteCandleEntity::getTopPrice)
                .max()
                .orElse(Double.NaN);
        if(lastCandle.getStartPrice() <= lastCandle.getEndPrice()){
            if(lastCandle.getTopPrice() >= maxHigh && PriceUtil.getEarnMoney(param.getMyStock().getMyPrice(),((Double)maxHigh).longValue(), param.getMyStock().getAmount()) > 0){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("maxHigh : "+maxHigh);
                return ((Double)maxHigh).longValue();
            }
            else if(lastCandle.getBottomPrice() <= orderBlock.getPrevCandleLow()){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("block PrevCandleLow : "+orderBlock.getPrevCandleLow());
                return orderBlock.getPrevCandleLow();
            }
        }
        else{
            if(lastCandle.getBottomPrice() <= orderBlock.getPrevCandleLow()){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("block PrevCandleLow : "+orderBlock.getPrevCandleLow());
                return orderBlock.getPrevCandleLow();
            }
            else if(lastCandle.getTopPrice() >= maxHigh && PriceUtil.getEarnMoney(param.getMyStock().getMyPrice(),((Double)maxHigh).longValue(), param.getMyStock().getAmount()) > 0){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("maxHigh : "+maxHigh);
                return ((Double)maxHigh).longValue();
            }
        }


        return 0;
    }

    //11.isOrderBlockAndFvgSell
    public long isOrderBlockAndFvgSell(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        Trade myStock = param.getMyStock();

        List<MinuteCandleEntity> ableCandles = candles.subList(0,candles.size()-1);
        List<MinuteCandleEntity> fiveMinuteCandles = IndicatorUtil.convertMinuteToMultiMinute(ableCandles, 5);
        List<MinuteCandleEntity> fifteenMinuteCandles = IndicatorUtil.convertMinuteToMultiMinute(ableCandles, 15);

        OrderBlock oneBlock = IndicatorUtil.getOrderBlock(ableCandles);
        OrderBlock fiveBlock = IndicatorUtil.getOrderBlock(fiveMinuteCandles);
        OrderBlock fifteenBlock = IndicatorUtil.getOrderBlock(fifteenMinuteCandles);

        Fvg oneFVG = getFVG(ableCandles, 30);
        Fvg fiveFVG = getFVG(fiveMinuteCandles, 30);
        Fvg fifteenFVG = getFVG(fifteenMinuteCandles, 30);

        MinuteCandleEntity candle = candles.getLast();

        Long lowestValueInOverlapped = getLowestValueInOverlapped(new Fvg[]{oneFVG,fiveFVG, fifteenFVG}, oneBlock,fiveBlock, fifteenBlock);

        if(lowestValueInOverlapped == null)
            return 0;

        int size = candles.size()-1;
        int startIndex = Math.max(0, size - 15);
        List<MinuteCandleEntity> recent15 = candles.subList(startIndex, size);

        // 최근 15개 봉 중 최고가 구하기
        double maxHigh = recent15.stream()
                .mapToDouble(MinuteCandleEntity::getTopPrice)
                .max()
                .orElse(Double.NaN);

        long sellPrice = ((Double)maxHigh).longValue();

        if(sellPrice <=candle.getTopPrice() && (PriceUtil.getEarnMoney(myStock.getMyPrice(), sellPrice, myStock.getAmount())>0)){
            return sellPrice;
        }
        else if(lowestValueInOverlapped >= candle.getBottomPrice()){
            return lowestValueInOverlapped;
        }

        return 0;
    }

    public static Long getLowestValueInOverlapped(Fvg[] fvgList, OrderBlock... obList) {

        // 1) null 제거하고 범위 수집
        List<long[]> ranges = new ArrayList<>();
        List<Long> allValues = new ArrayList<>(); // bottom/top 값 저장

        if (fvgList != null) {
            for (Fvg fvg : fvgList) {
                if (fvg != null) {
                    ranges.add(new long[]{fvg.bottom, fvg.top});
                    allValues.add(fvg.bottom);
                    allValues.add(fvg.top);
                }
            }
        }

        if (obList != null) {
            for (OrderBlock ob : obList) {
                if (ob != null) {
                    ranges.add(new long[]{ob.bottom, ob.top});
                    allValues.add(ob.bottom);
                    allValues.add(ob.top);
                }
            }
        }

        // 최소 2개 이상 있어야 겹침 판단 가능
        if (ranges.size() < 2) return null;

        // 2) 교집합 범위 계산
        long maxBottom = ranges.stream().mapToLong(r -> r[0]).max().orElse(Long.MIN_VALUE);
        long minTop    = ranges.stream().mapToLong(r -> r[1]).min().orElse(Long.MAX_VALUE);

        // 3) 겹침 여부 확인
        if (maxBottom > minTop) return null;

        // 4) 겹치는 범위 안에 속한 값들 중 최소값 찾기
        long minValueInOverlap = allValues.stream()
                .filter(v -> v >= maxBottom && v <= minTop) // 겹치는 범위 안
                .min(Long::compare)
                .orElse(Long.MIN_VALUE);

        return minValueInOverlap;
    }

    //10.isOrderBlockSell
    public long isFiveOrderBlockSell(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        MinuteCandleEntity lastCandle = candles.getLast();
        String timeStr = param.getMyStock().getTimeAt();
        List<MinuteCandleEntity> copyCandles = new ArrayList<>();
        for(int i=0; i<candles.size()-1; i++){
            MinuteCandleEntity candle = candles.get(i);
            if(candle.getTimeAt().equals(timeStr)) break;

            copyCandles.add(candle);
        }
        copyCandles = IndicatorUtil.convertMinuteToMultiMinute(copyCandles,5);
        OrderBlock orderBlock = IndicatorUtil.getOrderBlock(copyCandles);
        if(orderBlock==null) return 0;

        int size = candles.size()-1;
        int startIndex = Math.max(0, size - 15);
        List<MinuteCandleEntity> recent15 = candles.subList(startIndex, size);

        // 최근 15개 봉 중 최고가 구하기
        double maxHigh = recent15.stream()
                .mapToDouble(MinuteCandleEntity::getTopPrice)
                .max()
                .orElse(Double.NaN);
        if(lastCandle.getStartPrice() <= lastCandle.getEndPrice()){
            if(lastCandle.getTopPrice() >= maxHigh && PriceUtil.getEarnMoney(param.getMyStock().getMyPrice(),((Double)maxHigh).longValue(), param.getMyStock().getAmount()) > 0){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("maxHigh : "+maxHigh);
                return ((Double)maxHigh).longValue();
            }
            else if(lastCandle.getBottomPrice() <= orderBlock.getPrevCandleLow()){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("block PrevCandleLow : "+orderBlock.getPrevCandleLow());
                return orderBlock.getPrevCandleLow();
            }
        }
        else{
            if(lastCandle.getBottomPrice() <= orderBlock.getPrevCandleLow()){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("block PrevCandleLow : "+orderBlock.getPrevCandleLow());
                return orderBlock.getPrevCandleLow();
            }
            else if(lastCandle.getTopPrice() >= maxHigh && PriceUtil.getEarnMoney(param.getMyStock().getMyPrice(),((Double)maxHigh).longValue(), param.getMyStock().getAmount()) > 0){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("maxHigh : "+maxHigh);
                return ((Double)maxHigh).longValue();
            }
        }


        return 0;
    }

    //10.isOrderBlockSell
    public long isFifteenOrderBlockSell(CustomParam param){
        List<MinuteCandleEntity> candles = param.getCandles();
        MinuteCandleEntity lastCandle = candles.getLast();
        String timeStr = param.getMyStock().getTimeAt();
        List<MinuteCandleEntity> copyCandles = new ArrayList<>();
        for(int i=0; i<candles.size()-1; i++){
            MinuteCandleEntity candle = candles.get(i);
            if(candle.getTimeAt().equals(timeStr)) break;

            copyCandles.add(candle);
        }
        copyCandles = IndicatorUtil.convertMinuteToMultiMinute(copyCandles,15);
        OrderBlock orderBlock = IndicatorUtil.getOrderBlock(copyCandles);
        if(orderBlock==null) return 0;

        int size = candles.size()-1;
        int startIndex = Math.max(0, size - 15);
        List<MinuteCandleEntity> recent15 = candles.subList(startIndex, size);

        // 최근 15개 봉 중 최고가 구하기
        double maxHigh = recent15.stream()
                .mapToDouble(MinuteCandleEntity::getTopPrice)
                .max()
                .orElse(Double.NaN);
        if(lastCandle.getStartPrice() <= lastCandle.getEndPrice()){
            if(lastCandle.getTopPrice() >= maxHigh && PriceUtil.getEarnMoney(param.getMyStock().getMyPrice(),((Double)maxHigh).longValue(), param.getMyStock().getAmount()) > 0){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("maxHigh : "+maxHigh);
                return ((Double)maxHigh).longValue();
            }
            else if(lastCandle.getBottomPrice() <= orderBlock.getPrevCandleLow()){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("block PrevCandleLow : "+orderBlock.getPrevCandleLow());
                return orderBlock.getPrevCandleLow();
            }
        }
        else{
            if(lastCandle.getBottomPrice() <= orderBlock.getPrevCandleLow()){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("block PrevCandleLow : "+orderBlock.getPrevCandleLow());
                return orderBlock.getPrevCandleLow();
            }
            else if(lastCandle.getTopPrice() >= maxHigh && PriceUtil.getEarnMoney(param.getMyStock().getMyPrice(),((Double)maxHigh).longValue(), param.getMyStock().getAmount()) > 0){
//                log.info("order block at : "+orderBlock.getPrevCandleLow());
//                log.info("maxHigh : "+maxHigh);
                return ((Double)maxHigh).longValue();
            }
        }


        return 0;
    }


}
