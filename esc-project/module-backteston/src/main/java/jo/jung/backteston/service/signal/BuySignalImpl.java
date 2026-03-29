package jo.jung.backteston.service.signal;

import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.backteston.util.IndicatorUtil;
import jo.jung.common.graph.GraphUtil;
import jo.jung.common.priceutil.PriceUtil;
import jo.jung.domain.graph.Fvg;
import jo.jung.domain.graph.OrderBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple3;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static jo.jung.backteston.util.IndicatorUtil.getFVG;

@Slf4j
@Component
@RequiredArgsConstructor
public class BuySignalImpl implements BuySignal{
    @Override
    public Map<String, Function<List<MinuteCandleEntity>, Long>> getAllSignalFunctions() {
        Map<String, Function<List<MinuteCandleEntity>, Long>> map = new LinkedHashMap<>();
        //??
//        map.put("1.isRedCoverPreBlue", this::isRedCoverPreBlue);
        //0점
//        map.put("2.isRedCoverPreBlueInBollinger", this::isRedCoverPreBlueInBollinger);
        //4점
        //@@map.put("3.isMACDTurnUp", this::isMACDTurnUp);
        //17점 -> 26점
        //@@map.put("4.isBottomBollingerTouch", this::isBottomBollingerTouch);
        //21점 -> 29점
        //@@map.put("5.isRSIUnder30", this::isRSIUnder30);
        //??
//        map.put("6.isThreeCandleRed", this::isThreeCandleRed);
        //12점 -> 8점
        //@@map.put("7.isMAasc", this::isMAasc);
        //13점 -> 7점
//        map.put("8.isCrushLimitLine", this::isCrushLimitLine);
        //11점 -> 28점
        //@@map.put("9.isTradeAmountOver20Avg", this::isTradeAmountOver20Avg);
        //28점 -> 39점
        //@@map.put("10.isThreeCandleBlue", this::isThreeCandleBlue);
        //1점
//        map.put("11.isDivergenceDetected", this::isDivergenceDetected);
        //0점 1분봉,5분봉 0점
//        map.put("12.isDowningAndRedCoverPreTwoCandle", this::isDowningAndRedCoverPreTwoCandle);
        //0점 1분봉,5분봉 0점
//        map.put("13.isDowningAndHammerOrDoge", this::isDowningAndHammerOrDoge);
        //35점
//        map.put("14.isBollingerSpreadWide", this::isBollingerSpreadWide);
        //31점
        //@@map.put("15.isDowningAndBigBlue", this::isDowningAndBigBlue);
        //@@map.put("16.is60EMATouched", this::is60EMATouched);
//        map.put("17.is120EMATouched", this::is120EMATouched);
        //@@map.put("18.isTSG", this::isTSG);
//        map.put("20.FVG", this::isFVGLongSignal);
        map.put("21.isOrderBlockSignal", this::isOrderBlockSignal);
        //@@map.put("22.isOrderBlockAndFvgSignal", this::isOrderBlockAndFvgSignal);
        //@@map.put("23.isFiveMinuteOrderBlockSignal", this::isFiveMinuteOrderBlockSignal);
        //@@map.put("24.isFifteenMinuteOrderBlockSignal", this::isFifteenMinuteOrderBlockSignal);
        //@@map.put("25.isOrderBlockLayerSignal", this::isOrderBlockLayerSignal);
        return map;
    }

    //1. 직전 음봉을 감싸는 상승장악형 양봉출현 탐지
    public boolean isRedCoverPreBlue(List<MinuteCandleEntity> candles){
        int index = candles.size()-1;
        if(index-1 < 0)
            return false;
        MinuteCandleEntity nowCandle = candles.get(index);
        MinuteCandleEntity preCandle = candles.get(index-1);

        long nowStart = nowCandle.getStartPrice();
        long nowEnd = nowCandle.getEndPrice();
        long preStart = preCandle.getStartPrice();
        long preEnd = preCandle.getEndPrice();

        if(preStart<preEnd || nowStart>nowEnd) return false;

        if(preStart < nowEnd){
//            log.info("상승장악형 양봉출현 : "+preStart+" < "+nowEnd);
            return true;
        }
        return false;
    }

    //2. 2번째 상승장악형 양봉출현 탐지 단, 양봉이 볼린저밴드 안에서 형성되어야함(2번째 바닥이 1번째보다 높든낮든 상관없음)
    public long isRedCoverPreBlueInBollinger(List<MinuteCandleEntity> candles){
        int index = candles.size()-2;
        boolean firstBoolean = isRedCoverPreBlue(candles.subList(0,index+1));
        if(!firstBoolean) return -1;

        Tuple3<Double, Double, Double> bands = IndicatorUtil.getBollingerBands(candles.subList(0, index + 1), 20, 2.0);

        if(candles.get(index-1).getEndPrice() > bands.getT3()){
//            log.info("두번쨰바닥 볼린저내부 안착 : "+candles.get(index-1).getEndPrice()+" > "+bands.getT3());
            return 0;
        }

        return -1;
    }

    //3. MACD 양전
    public long isMACDTurnUp(List<MinuteCandleEntity> candles) {
        int index = candles.size()-2;
        if (candles.size()-1 < 34) {
            return -1; // Signal선 계산을 위한 최소 인덱스는 34
        }

        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (MinuteCandleEntity candle : candles) {
            closes.add((double) candle.getEndPrice());
        }

        // MACD 리스트 구성 (Signal EMA를 위한 9개)
        List<Double> macdList = new ArrayList<>();
        for (int i = index - 8; i <= index; i++) {
            macdList.add(IndicatorUtil.getMACD(closes.subList(0,i+1)));
        }

        double macdPrev = macdList.get(7);   // index - 1
        double macdNow = macdList.get(8);    // index

        double signalPrev = IndicatorUtil.calculateEMA(macdList, 7, 9);  // Signal at index - 1
        double signalNow = IndicatorUtil.calculateEMA(macdList, 8, 9);   // Signal at index

        // MACD가 Signal선을 아래에서 위로 돌파하는 경우 = 턴업
        if(macdPrev < signalPrev && macdNow > signalNow){
//            log.info("macd 양전 탐지 at : "+candles.get(index).getTimeAt());
            return 0;
        }
        return -1;
    }

    //4. 볼린저밴드 하단터치여부 확인
    public long isBottomBollingerTouch(List<MinuteCandleEntity> candles){
        int index = candles.size()-1;
        if(index<=0) return -1;
        Tuple3<Double, Double, Double> bands = IndicatorUtil.getBollingerBands(candles.subList(0, index), 20, 2.0);
        double lower = bands.getT3();
        MinuteCandleEntity candle = candles.get(index);
        if(candle.getBottomPrice() <= lower){
//            log.info("볼린저밴드 하단터치 : "+candle.getEndPrice()+"<="+lower);
            return 0;
        }
        return -1;
    }

    //5. RSI 30이하
    public long isRSIUnder30(List<MinuteCandleEntity> candles){
        int index = candles.size()-1;
        int rsi = IndicatorUtil.getRSI(candles.subList(0,index));
        if(rsi == -1) return -1;
        if(rsi <= 30){
            return 0;
        }
        return -1;
    }

    //6. 최근 3캔들 연속 양봉여부
    public boolean isThreeCandleRed(List<MinuteCandleEntity> candles){
        int index = candles.size()-1;
        if(index-2 < 0)
            return false;
        MinuteCandleEntity candle1 = candles.get(index-2);
        MinuteCandleEntity candle2 = candles.get(index-1);
        MinuteCandleEntity candle3 = candles.get(index);

        return candle1.getStartPrice()<candle1.getEndPrice() &&
                candle2.getStartPrice()<candle2.getEndPrice() &&
                candle3.getStartPrice()<candle3.getEndPrice();
    }


    //7. 1분봉의 이평선 정배열(5MA > 20MA)
    public long isMAasc(List<MinuteCandleEntity> candles){

        int size = candles.size()-1;

        // 최소 20개 이상 있어야 20MA 계산 가능
        if (size < 20) {
            //필터는 and조건이므로 20개전에는 참으로 넘겨준다
            return -1;
        }

        // 최근 5, 20, 60개의 종가로 단순이동평균 계산
        double ma5 = candles.subList(size - 5, size).stream()
                .mapToLong(MinuteCandleEntity::getEndPrice)
                .average().orElse(0);

        double ma20 = candles.subList(size - 20, size).stream()
                .mapToLong(MinuteCandleEntity::getEndPrice)
                .average().orElse(0);

        // 정배열 조건 체크: 5MA > 20MA
        if(ma5 > ma20){
//            log.info("5MA > 20MA 만족 "+ma5+">"+ma20);
            return 0;
        }
        return -1;
    }

    //8. 1분봉의 최근 저항선 돌파 + 1봉안착(일단제외)
    public long isCrushLimitLine(List<MinuteCandleEntity> candles){
        int index = candles.size()-1;
//        List<MinuteCandleEntity> fiveMinuteCandles = IndicatorUtil.makeOneMinuteCandleToFiveMinuteCandle(candles.subList(0, index + 1));
//        Tuple3<Long, Long, String> tuple3 = IndicatorUtil.detectResistanceLevels(fiveMinuteCandles, 2, 1);
        Tuple3<Long, Long, String> tuple3 = IndicatorUtil.detectResistanceLevels(candles.subList(0,index), 2, 1);
        if(tuple3 == null) return -1;

        long price = tuple3.getT1();
        long downTimes = tuple3.getT2();
        String appearAt = tuple3.getT3();
        long topPrice = candles.get(index).getTopPrice();
        long lowPrice = candles.get(index).getTopPrice();

        if(topPrice >= price){
            int tick = PriceUtil.getTick(price);
            final long TICK_LIMIT = 5L;
            boolean isUnderNTick = ((price + (tick * TICK_LIMIT)) >= topPrice);
            if(!isUnderNTick) {
                return -1;
            }
            if(lowPrice <= price) {
//                log.info("저항선 돌파 탐지 가격/시간/횟수" + price + " / " + appearAt + " / " + downTimes);
                return price;
            }

            return 0;
        }
        return -1;
    }

    //9. 거래량 > 최근 20봉 평균의 1.5배
    public long isTradeAmountOver20Avg(List<MinuteCandleEntity> candles){
        int index = candles.size()-2;
//        List<MinuteCandleEntity> fiveMinuteCandles = IndicatorUtil.makeOneMinuteCandleToFiveMinuteCandle(candles.subList(0, index + 1));
        if(candles.size()<21) return -1;
        long sum = 0;
        int cnt = 0;
        for(int i=candles.size()-3; i>=0; i--){
            if(cnt==20) break;
            sum+=candles.get(i).getTradeAmount();
            cnt++;
        }
        if(cnt==0) return -1;
        float avg = (float)sum/cnt;
        long nowAmount = candles.get(index).getTradeAmount();
        if(avg*1.5f <= nowAmount)
            return 0;
        return -1;
    }



    //10. 하락장악형 캔들확인(3연속 음봉)
    public long isThreeCandleBlue(List<MinuteCandleEntity> candles){
        int index = candles.size()-1;
        if(index-3 < 0) return -1;


        MinuteCandleEntity candle1 = candles.get(index-3);
        MinuteCandleEntity candle2 = candles.get(index-2);
        MinuteCandleEntity candle3 = candles.get(index-1);

        if(candle1.getStartPrice()>candle1.getEndPrice() &&
                candle2.getStartPrice()>candle2.getEndPrice() &&
                candle3.getStartPrice()>candle3.getEndPrice()){
            return 0;
        }
        return -1;
    }

    //11. 상승다이버전스 탐지
    public long isDivergenceDetected(List<MinuteCandleEntity> candles){
        List<MinuteCandleEntity> preCandles = candles.subList(0,candles.size()-1);
        List<Integer> rsiList = IndicatorUtil.getRSIList(preCandles);
        if(rsiList.size()<=0) return -1;
        //그래프 바닥점 3개 찾기
        List<Integer> rsiBottoms = IndicatorUtil.findRecentBottoms(rsiList, 40);
        if (rsiBottoms.size()<=2) return -1;

        List<Integer> bottomPriceList = preCandles.stream()
                .map(c -> (int) c.getBottomPrice())
                .collect(Collectors.toList());
        List<Integer> priceBottoms = IndicatorUtil.findRecentBottoms(bottomPriceList, -1);
        if(priceBottoms.size()<=2) return -1;

        //상승세인지 하락세인지 판단
        int MIN_RISE_VALUE = 3;
        int MAX_MISS_VALUE = 2;
        boolean isRisingRsi = (rsiBottoms.getFirst()>rsiBottoms.getLast()+MIN_RISE_VALUE)
                && (rsiBottoms.getFirst() >= rsiBottoms.get(1) - MAX_MISS_VALUE);

        float MIN_RISE_PRICE = priceBottoms.getFirst()*0.002f;
        float MAX_MISS_PRICE = priceBottoms.getFirst()*0.001f;
        boolean isDowningPrice = (priceBottoms.getFirst()<priceBottoms.getLast()-MIN_RISE_PRICE)
                && (priceBottoms.getFirst() <= priceBottoms.get(1) + MAX_MISS_PRICE);

        return (isRisingRsi && isDowningPrice) ? 0 : -1;
    }

    //12. 하락추세중 이전2개캔들 덮는 상승장악형 캔들 탐지
    public long isDowningAndRedCoverPreTwoCandle(List<MinuteCandleEntity> candles){
        int index = candles.size()-2;
        if(index-20 < 0)
            return -1;

        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (int i=0; i<=index; i++) {
            closes.add((double)candles.get(i).getEndPrice());
        }

        double v2 = IndicatorUtil.calculateEMA(closes, index, 10);
        double v1 = IndicatorUtil.calculateEMA(closes, index-10, 10);

        if(v2>=v1)
            return -1;


        MinuteCandleEntity nowCandle = candles.get(index);
        MinuteCandleEntity preCandle = candles.get(index-1);
        MinuteCandleEntity pre2Candle = candles.get(index-2);

        long nowStart = nowCandle.getStartPrice();
        long nowEnd = nowCandle.getEndPrice();

        if((nowStart>=nowEnd) || ((nowEnd-nowStart) < nowEnd*0.01f))
            return -1;

        long preBottom = Math.min(preCandle.getBottomPrice(),pre2Candle.getBottomPrice());
        long preTop = Math.max(preCandle.getTopPrice(), pre2Candle.getTopPrice());

        if(preBottom<nowCandle.getBottomPrice() || preTop>nowCandle.getTopPrice()) return -1;

        return 0;
    }

    //13. 하락추세중 아래꼬리긴해머형 or 일반 도지후 상승장악형 캔들 탐지
    public long isDowningAndHammerOrDoge(List<MinuteCandleEntity> candles){
        int index = candles.size()-2;
        if(index-20 < 0)
            return -1;

        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (int i=0; i<=index; i++) {
            closes.add((double)candles.get(i).getEndPrice());
        }

        double v2 = IndicatorUtil.calculateEMA(closes, index, 10);
        double v1 = IndicatorUtil.calculateEMA(closes, index-10, 10);

        if(v2>=v1)
            return -1;


        MinuteCandleEntity nowCandle = candles.get(index);
        MinuteCandleEntity preCandle = candles.get(index-1);

        long nowStart = nowCandle.getStartPrice();
        long nowEnd = nowCandle.getEndPrice();

        if((nowStart>=nowEnd) || ((nowEnd-nowStart) < nowEnd*0.01f))
            return -1;

        if(!IndicatorUtil.isDogeOrHammer(preCandle))
            return -1;

        return 0;
    }

    //14. 볼린저밴드 너무 작은거 제외해보기
    public long isBollingerSpreadWide(List<MinuteCandleEntity> candles){
        if(candles.size()<=1) return -1;
        Tuple3<Double, Double, Double> bands = IndicatorUtil.getBollingerBands(candles.subList(0,candles.size()-1), 20, 2.0);

        if(Math.abs(bands.getT1()- bands.getT3()) < candles.get(candles.size()-2).getEndPrice()*0.01f)
            return -1;

        return 0;
    }

    //15. 하락추세중 장대음봉 캔들 탐지
    public long isDowningAndBigBlue(List<MinuteCandleEntity> candles){
        int index = candles.size()-2;//이전캔들까지
        if(index-20 < 0)
            return -1;

        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (MinuteCandleEntity candle : candles) {
            closes.add((double)candle.getEndPrice());
        }

        double v2 = IndicatorUtil.calculateEMA(closes, index, 10);
        double v1 = IndicatorUtil.calculateEMA(closes, index-10, 10);

        if(v2>=v1)
            return -1;


        MinuteCandleEntity preCandle = candles.get(index);

        long nowStart = preCandle.getStartPrice();
        long nowEnd = preCandle.getEndPrice();

        if((nowStart<=nowEnd) || ((nowStart-nowEnd) < nowEnd*0.01f))
            return -1;

        return 0;
    }

    //16. 60분 이평선 터치시 반등 노리기
    public long is60EMATouched(List<MinuteCandleEntity> candles){
        int index = candles.size()-2;//이전캔들까지
        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (MinuteCandleEntity candle : candles) {
            closes.add((double)candle.getEndPrice());
        }
        double ema = IndicatorUtil.calculateEMA(closes, index, 60);

        if(candles.getLast().getBottomPrice()<= ema)
            return 0;

        return -1;
    }
    //17. 120분 이평선 터치시 반등 노리기
    public long is120EMATouched(List<MinuteCandleEntity> candles){
        int index = candles.size()-2;//이전캔들까지
        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (MinuteCandleEntity candle : candles) {
            closes.add((double)candle.getEndPrice());
        }
        double ema = IndicatorUtil.calculateEMA(closes, index, 120);

        if(candles.getLast().getBottomPrice()<= ema)
            return 0;

        return -1;
    }
    //18. Trend Strength Gauge
    public long isTSG(List<MinuteCandleEntity> candles) {
        // 종가 추출
        List<Double> closes = new ArrayList<>();
        for (int i=0; i<candles.size()-1; i++) {
            MinuteCandleEntity candle = candles.get(i);
            closes.add((double)candle.getEndPrice());
        }

        List<Double> HSMAList = IndicatorUtil.calculateHSMA(closes, 20);
        boolean isCrossNow = isHSMAZeroCrossNow(HSMAList);
        if(isCrossNow){
            return 0;
        }
        return -1;
    }

    // HSMA 음->양 전환 여부 확인 (현재 시점 기준)
    private boolean isHSMAZeroCrossNow(List<Double> hsmaList) {
        int n = hsmaList.size();
        if (n < 2) return false; // 데이터가 부족하면 false

        double prev = hsmaList.get(n - 2);
        double curr = hsmaList.get(n - 1);

        return prev <= 0 && curr > 0; // 음->양 전환 시 true
    }


    //19. Dynamic Swing Anchored VWAP

    //20. FVG
    private long isFVGLongSignal(List<MinuteCandleEntity> candles) {

        // 1. 최신 FVG 탐지 + Filled 여부 체크
        Fvg lastFVG = getFVG(candles.subList(0,candles.size()-1), 30);
        MinuteCandleEntity lastCandle = candles.getLast();

        if(lastFVG==null) return -1;

        Long middleFvg = (lastFVG.getBottom()+lastFVG.getTop())/2;
        if(middleFvg <= lastCandle.getTopPrice() && middleFvg >= lastCandle.getBottomPrice())
            return middleFvg;

//        log.info("FVG "+lastFVG.getBottom()+"~"+lastFVG.getTop()+" time at : "+candles.get(lastFVG.getCreatedIndex()).getTimeAt());
        return -1;
    }

    //21. 오더블럭
    private long isOrderBlockSignal(List<MinuteCandleEntity> candles){
        List<MinuteCandleEntity> ableCandles = candles.subList(0,candles.size()-1);

        OrderBlock oneBlock = IndicatorUtil.getOrderBlock(ableCandles);

        MinuteCandleEntity candle = candles.getLast();

        if(oneBlock == null)
            return -1;

        long overlappedMiddle = oneBlock.getTop();

        if(overlappedMiddle <= candle.getTopPrice() && overlappedMiddle >= candle.getBottomPrice()) {
//            log.info("블록 at : "+candles.get(oneBlock.getCreatedIndex()).getTimeAt()+" block top : "+overlappedMiddle);
            return overlappedMiddle;
        }

        return -1;
    }




    //22. FVG+오더블럭 2개이상 겹치는곳
    private long isOrderBlockAndFvgSignal(List<MinuteCandleEntity> candles){
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

        Long overlappedTop = getOverlappedTop(new Fvg[]{oneFVG,fiveFVG, fifteenFVG}, oneBlock, fiveBlock, fifteenBlock);

        if(overlappedTop == null)
            return -1;

        if(overlappedTop <= candle.getTopPrice() && overlappedTop >= candle.getBottomPrice())
            return overlappedTop;

        return -1;
    }

    //23. 오더블럭
    private long isFiveMinuteOrderBlockSignal(List<MinuteCandleEntity> candles){
        List<MinuteCandleEntity> ableCandles = candles.subList(0,candles.size()-1);
        List<MinuteCandleEntity> fiveMinuteCandles = IndicatorUtil.convertMinuteToMultiMinute(ableCandles, 5);

        OrderBlock fiveBlock = IndicatorUtil.getOrderBlock(fiveMinuteCandles);

        MinuteCandleEntity candle = candles.getLast();

        if(fiveBlock == null)
            return -1;

        long topValue = fiveBlock.getTop();

        if(topValue <= candle.getTopPrice() && topValue >= candle.getBottomPrice())
            return topValue;

        return -1;
    }

    //24. 오더블럭
    private long isFifteenMinuteOrderBlockSignal(List<MinuteCandleEntity> candles){
        List<MinuteCandleEntity> ableCandles = candles.subList(0,candles.size()-1);
        List<MinuteCandleEntity> fifteenMinuteCandles = IndicatorUtil.convertMinuteToMultiMinute(ableCandles, 15);

        OrderBlock fifteenBlock = IndicatorUtil.getOrderBlock(fifteenMinuteCandles);

        MinuteCandleEntity candle = candles.getLast();

        if(fifteenBlock == null)
            return -1;

        long topValue = fifteenBlock.getTop();

        if(topValue <= candle.getTopPrice() && topValue >= candle.getBottomPrice())
            return topValue;

        return -1;
    }

    //25. 오더블럭만 2개이상 겹치는곳
    private long isOrderBlockLayerSignal(List<MinuteCandleEntity> candles){
        List<MinuteCandleEntity> ableCandles = candles.subList(0,candles.size()-1);
        List<MinuteCandleEntity> fiveMinuteCandles = IndicatorUtil.convertMinuteToMultiMinute(ableCandles, 5);
        List<MinuteCandleEntity> fifteenMinuteCandles = IndicatorUtil.convertMinuteToMultiMinute(ableCandles, 15);

        OrderBlock oneBlock = IndicatorUtil.getOrderBlock(ableCandles);
        OrderBlock fiveBlock = IndicatorUtil.getOrderBlock(fiveMinuteCandles);
        OrderBlock fifteenBlock = IndicatorUtil.getOrderBlock(fifteenMinuteCandles);


        MinuteCandleEntity candle = candles.getLast();

        Long overlappedTop = getOverlappedTop(oneBlock, fiveBlock, fifteenBlock);

        if(overlappedTop == null)
            return -1;

        if(overlappedTop <= candle.getTopPrice() && overlappedTop >= candle.getBottomPrice())
            return overlappedTop;

        return -1;
    }

    private Long getOverlappedMiddle(OrderBlock... blocks) {

        // 1) null 제거
        List<OrderBlock> list = Arrays.stream(blocks)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (list.size() < 2) {
            return null; // 2개 미만이면 교집합 불가능
        }

        // 2) 교집합 범위 계산
        Long maxBottom = list.stream()
                .map(b -> b.bottom)
                .max(Long::compare)
                .orElse(null);

        Long minTop = list.stream()
                .map(b -> b.top)
                .min(Long::compare)
                .orElse(null);

        // 3) 교집합 없으면 null
        if (maxBottom == null || minTop == null || maxBottom > minTop) {
            return null;
        }

        // 4) 중앙값(middle) 계산
        return (maxBottom + minTop) / 2;
    }
    public static Long getOverlappedMiddle(Fvg[] fvgList, OrderBlock... obList) {

        // 1) null 제거하고 범위 수집
        List<long[]> ranges = new ArrayList<>();

        if (fvgList != null) {
            for (Fvg fvg : fvgList) {
                if (fvg != null) ranges.add(new long[]{fvg.bottom, fvg.top});
            }
        }

        if (obList != null) {
            for (OrderBlock ob : obList) {
                if (ob != null) ranges.add(new long[]{ob.bottom, ob.top});
            }
        }

        // 최소 2개 이상 있어야 겹침 판단 가능
        if (ranges.size() < 2) return null;

        // 2) 교집합 범위 계산
        long maxBottom = ranges.stream().mapToLong(r -> r[0]).max().orElse(Long.MIN_VALUE);
        long minTop    = ranges.stream().mapToLong(r -> r[1]).min().orElse(Long.MAX_VALUE);

        // 3) 겹침 여부 확인
        if (maxBottom > minTop) return null;

        // 4) middle 값 계산
        return (maxBottom + minTop) / 2;
    }

    private Long getOverlappedTop(OrderBlock... blocks) {

        // 1) null 제거
        List<OrderBlock> list = Arrays.stream(blocks)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (list.size() < 2) {
            return null; // 2개 미만이면 교집합 불가능
        }

        // 2) 교집합 범위 계산
        Long maxBottom = list.stream()
                .map(b -> b.bottom)
                .max(Long::compare)
                .orElse(null);

        Long minTop = list.stream()
                .map(b -> b.top)
                .min(Long::compare)
                .orElse(null);

        // 3) 교집합 없으면 null
        if (maxBottom == null || minTop == null || maxBottom > minTop) {
            return null;
        }


        return minTop;
    }
    public static Long getOverlappedTop(Fvg[] fvgList, OrderBlock... obList) {

        // 1) null 제거하고 범위 수집
        List<long[]> ranges = new ArrayList<>();

        if (fvgList != null) {
            for (Fvg fvg : fvgList) {
                if (fvg != null) ranges.add(new long[]{fvg.bottom, fvg.top});
            }
        }

        if (obList != null) {
            for (OrderBlock ob : obList) {
                if (ob != null) ranges.add(new long[]{ob.bottom, ob.top});
            }
        }

        // 최소 2개 이상 있어야 겹침 판단 가능
        if (ranges.size() < 2) return null;

        // 2) 교집합 범위 계산
        long maxBottom = ranges.stream().mapToLong(r -> r[0]).max().orElse(Long.MIN_VALUE);
        long minTop    = ranges.stream().mapToLong(r -> r[1]).min().orElse(Long.MAX_VALUE);

        // 3) 겹침 여부 확인
        if (maxBottom > minTop) return null;


        return minTop;
    }

}
