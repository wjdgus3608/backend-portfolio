package jo.jung.backteston.util;

import jo.jung.backteston.entity.MinuteCandleEntity;
import jo.jung.common.priceutil.PriceUtil;
import jo.jung.domain.graph.Fvg;
import jo.jung.domain.graph.OrderBlock;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class IndicatorUtil {
    //MACD 계산함수
    public static double getMACD(List<Double> closes) {
        int index = closes.size()-1;
        if (index < 26) return 0.0; // EMA26까지는 계산 불가

        double ema12 = calculateEMA(closes, index, 12);
        double ema26 = calculateEMA(closes, index, 26);
        return ema12 - ema26;
    }

    // 🔹 SMA 계산 함수 (Simple Moving Average)
    public static double calculateSMA(List<Double> values, int index, int period) {
        if (index < period - 1) return 0.0;

        double sum = 0.0;
        for (int i = index - period + 1; i <= index; i++) {
            sum += values.get(i);
        }

        return sum / period;
    }

    // 🔹 EMA 계산 함수 (Exponential Moving Average)
    public static double calculateEMA(List<Double> values, int index, int period) {
        if (index < period - 1) return 0.0;

        double k = 2.0 / (period + 1);
        // ✅ 초기값을 SMA 함수로 대체
        double ema = calculateSMA(values, index, period);

        // 지수이동평균 계산
        for (int i = index - period + 1; i <= index; i++) {
            ema = values.get(i) * k + ema * (1 - k);
        }

        return ema;
    }

    // 🔹 2. 가중이동평균 (WMA)
    public static double calculateWMA(List<Double> values, int index, int period) {
        if (index < period - 1) return 0.0;

        double weightedSum = 0.0;
        double weightTotal = 0.0;

        int weight = 1;
        for (int i = index - period + 1; i <= index; i++) {
            weightedSum += values.get(i) * weight;
            weightTotal += weight;
            weight++;
        }

        return weightedSum / weightTotal;
    }

    // 4️⃣ HMA 전체 배열 생성
    public static List<Double> calculateHMA(List<Double> values, int period) {
        int n = values.size();
        List<Double> hmaList = new ArrayList<>();

        int halfPeriod = period / 2;
        int sqrtPeriod = (int) Math.sqrt(period);

        // WMA(halfPeriod) 시계열
        List<Double> wmaHalfList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            wmaHalfList.add(calculateWMA(values, i, halfPeriod));
        }

        // WMA(fullPeriod) 시계열
        List<Double> wmaFullList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            wmaFullList.add(calculateWMA(values, i, period));
        }

        // 2*WMA(half) - WMA(full)
        List<Double> diffList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            diffList.add(2 * wmaHalfList.get(i) - wmaFullList.get(i));
        }

        // HMA = WMA(diff, sqrt(n))
        for (int i = 0; i < n; i++) {
            hmaList.add(calculateWMA(diffList, i, sqrtPeriod));
        }

        return hmaList;
    }

    // 5️⃣ HSMA 전체 배열 생성 = HMA - SMA
    public static List<Double> calculateHSMA(List<Double> values, int period) {
        int n = values.size();
        List<Double> hmaList = calculateHMA(values, period);
        List<Double> hsmaList = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            double sma = calculateSMA(values, i, period);
            hsmaList.add(hmaList.get(i) - sma);
        }

        return hsmaList;
    }

    //볼린저밴드 계산함수
    public static Tuple3<Double, Double, Double> getBollingerBands(List<MinuteCandleEntity> candles, int period, double k) {
        // 최근 period 개의 데이터만 사용
        List<Double> values = candles.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> list.subList(Math.max(0, list.size() - period), list.size())
                ))
                .stream()
                .map(candle -> (double) (candle.getEndPrice() + candle.getBottomPrice() + candle.getTopPrice()) / 3)
                .collect(Collectors.toList());

        int count = values.size();
        if (count == 0) {
            throw new IllegalArgumentException("볼린저밴드 만드는 데이터 부족");
        }

        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / count;

        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum() / count;

        double stddev = Math.sqrt(variance);

        // 반올림 처리
        double roundedMean = round(mean, 4);
        double roundedStddev = round(stddev, 4);
        double upper = roundedMean + (k * roundedStddev);
        double lower = roundedMean - (k * roundedStddev);

        return Tuples.of(upper, mean, lower);
    }

    // 소수점 반올림 함수
    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    public static int getRSI(List<MinuteCandleEntity> candles){
        final int period = 14;

        if (candles == null || candles.size() <= period) {
            return -1; // 데이터 부족
        }

        double gain = 0.0;
        double loss = 0.0;

        // 초기 SMA 기반 평균 구하기 (1~period까지)
        for (int i = 1; i <= period; i++) {
            long diff = candles.get(i).getEndPrice() - candles.get(i - 1).getEndPrice();
            if (diff > 0) {
                gain += diff;
            } else {
                loss += Math.abs(diff);
            }
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        // EMA 방식으로 period+1부터 마지막까지 평균 갱신
        for (int i = period + 1; i < candles.size(); i++) {
            long diff = candles.get(i).getEndPrice() - candles.get(i - 1).getEndPrice();
            double currentGain = diff > 0 ? diff : 0;
            double currentLoss = diff < 0 ? Math.abs(diff) : 0;

            avgGain = (avgGain * (period - 1) + currentGain) / period;
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period;
        }

        return calculateRSI(avgGain, avgLoss);
    }

    public static List<Integer> getRSIList(List<MinuteCandleEntity> candles) {
        final int period = 14;
        List<Integer> rsiList = new ArrayList<>();

        if (candles == null || candles.size() <= period) {
            return rsiList;
        }

        double gain = 0.0;
        double loss = 0.0;

        // 초기 SMA 기반 avgGain, avgLoss 계산
        for (int i = 1; i <= period; i++) {
            long diff = candles.get(i).getEndPrice() - candles.get(i - 1).getEndPrice();
            if (diff > 0) {
                gain += diff;
            } else {
                loss += Math.abs(diff);
            }
        }

        double avgGain = gain / period;
        double avgLoss = loss / period;

        // 초기 period 이전은 -1
        for (int i = 0; i < period; i++) {
            rsiList.add(-1);
        }

        // 첫 번째 RSI 계산
        rsiList.add(calculateRSI(avgGain, avgLoss));

        // EMA 방식으로 이후 RSI 계산
        for (int i = period + 1; i < candles.size(); i++) {
            long diff = candles.get(i).getEndPrice() - candles.get(i - 1).getEndPrice();
            double currentGain = diff > 0 ? diff : 0;
            double currentLoss = diff < 0 ? Math.abs(diff) : 0;

            avgGain = (avgGain * (period - 1) + currentGain) / period;
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period;

            rsiList.add(calculateRSI(avgGain, avgLoss));
        }

        return rsiList;
    }

    private static int calculateRSI(double avgGain, double avgLoss) {
        if (avgLoss == 0) {
            return 100;
        }
        double rs = avgGain / avgLoss;
        double rsi = 100 - (100 / (1 + rs));
        return (int) Math.round(rsi);
    }


    public static List<MinuteCandleEntity> convertMinuteToMultiMinute(List<MinuteCandleEntity> minuteCandles, int unitMinutes) {

        List<MinuteCandleEntity> result = new ArrayList<>();

        if (minuteCandles == null || minuteCandles.isEmpty()) {
            return result;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // 1) 시간 기준 정렬
        minuteCandles.sort(Comparator.comparing(
                c -> LocalDateTime.parse(c.getTimeAt(), formatter)
        ));

        // 2) N분 단위 그룹핑
        Map<LocalDateTime, List<MinuteCandleEntity>> grouped = new LinkedHashMap<>();

        for (MinuteCandleEntity candle : minuteCandles) {
            LocalDateTime time = LocalDateTime.parse(candle.getTimeAt(), formatter);

            // floor(N)
            int floored = (time.getMinute() / unitMinutes) * unitMinutes;

            LocalDateTime groupKey = time.withMinute(floored)
                    .withSecond(0)
                    .withNano(0);

            grouped.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(candle);
        }

        // 3) 그룹별 N분봉 생성
        for (Map.Entry<LocalDateTime, List<MinuteCandleEntity>> entry : grouped.entrySet()) {
            List<MinuteCandleEntity> group = entry.getValue();

            // 기본: 정확히 N개 이상이어야 생성
            if (group.size() < unitMinutes) {
                continue;
            }

            MinuteCandleEntity first = group.get(0);
            MinuteCandleEntity last = group.get(group.size() - 1);

            long topPrice = group.stream().mapToLong(MinuteCandleEntity::getTopPrice).max().orElse(0);
            long bottomPrice = group.stream().mapToLong(MinuteCandleEntity::getBottomPrice).min().orElse(0);
            long totalAmount = group.stream().mapToLong(MinuteCandleEntity::getTradeAmount).sum();
            long totalMoney = group.stream().mapToLong(MinuteCandleEntity::getTradeMoney).sum();

            String groupTimeStr = entry.getKey().format(formatter);

            MinuteCandleEntity newCandle = MinuteCandleEntity.builder()
                    .stockCode(first.getStockCode())
                    .stockName(first.getStockName())
                    .startPrice(first.getStartPrice())
                    .endPrice(last.getEndPrice())
                    .topPrice(topPrice)
                    .bottomPrice(bottomPrice)
                    .tradeAmount(totalAmount)
                    .tradeMoney(totalMoney)
                    .timeAt(groupTimeStr)
                    .build();

            result.add(newCandle);
        }

        return result;
    }

    //저항선 구하는함수
    public static Tuple3<Long, Long, String> detectResistanceLevels(
            List<MinuteCandleEntity> candles,
            int minRejectionCount,
            float minVolumeMultiplier
    ) {
        Map<Long, List<String>> rejectionMap = new HashMap<>();

        for (int i = 1; i<candles.size() ; i++) {
            MinuteCandleEntity current = candles.get(i);
            MinuteCandleEntity prev = candles.get(i - 1);

            boolean priceDropped = current.getEndPrice() < prev.getEndPrice();
            boolean volumeIncreased = current.getTradeAmount() > prev.getTradeAmount() * minVolumeMultiplier;

            if (priceDropped) {
                long price = prev.getEndPrice();
                String timeAt = prev.getTimeAt();

                rejectionMap.computeIfAbsent(price, k -> new ArrayList<>()).add(timeAt);
            }
        }

        // 필터링: 최소 반락 횟수 이상
        return rejectionMap.entrySet().stream()
                .map(entry -> {
                    Long price = entry.getKey();
                    List<String> times = entry.getValue();

                    String latestTime = times.stream()
                            .max(Comparator.naturalOrder())
                            .orElse("00000000000000");

                    return Tuples.of(price, (long) times.size(), latestTime);
                })
                .filter(t -> t.getT2() >= minRejectionCount)
                .sorted((a, b) -> b.getT3().compareTo(a.getT3())) // 최신 시간순 내림차순
                .findFirst()
                .orElse(null);
    }

    public static List<Integer> findRecentBottoms(List<Integer> rsiList, int maxValue) {
        final int window = 10; // 양쪽 5개씩 = 총 11개
        final int minDistance = 10;
        final int rsiThreshold = (maxValue==-1 ? Integer.MAX_VALUE : maxValue);

        List<Integer> bottoms = new ArrayList<>();

        if (rsiList == null || rsiList.size() < window + 1) {
            return bottoms; // 데이터 부족
        }

        for (int i = rsiList.size() - 1; i >= window / 2; i--) {
            if (rsiList.get(i) == -1 || (bottoms.size()==2 && rsiList.get(i) > rsiThreshold)) {
                continue; // 조건 1 위반: 30 초과거나 계산불가
            }

            boolean isLocalMin = true;

            // 윈도우 범위 내에서 자신이 최저인지 확인
            for (int j = i - window / 2; j <= i + window / 2 && j < rsiList.size(); j++) {
                if (j < 0 || j == i || rsiList.get(j) == -1) continue;
                if (rsiList.get(j) <= rsiList.get(i)) {
                    isLocalMin = false;
                    break;
                }
            }

            if (!isLocalMin) continue;

            // 이전 바닥점과 최소 10 인덱스 이상 떨어져 있는지 확인
            boolean isFarEnough = true;
            for (int idx : bottoms) {
                if (Math.abs(idx - i) < minDistance) {
                    isFarEnough = false;
                    break;
                }
            }

            if (isFarEnough) {
                bottoms.add(rsiList.get(i));
                if (bottoms.size() >= 3) break; // 최대 3개 찾으면 종료
            }
        }

        return bottoms;
    }

    public static boolean isDogeOrHammer(MinuteCandleEntity c) {
        double open = c.getStartPrice();
        double close = c.getEndPrice();
        double high = c.getTopPrice();
        double low = c.getBottomPrice();

        double body = Math.abs(close - open);
        double upper = high - Math.max(open, close);
        double lower = Math.min(open, close) - low;
        double total = high - low;

        // 예외 처리: 데이터 이상
        if (total == 0) return false;

        double bodyRatio = body / total;
        double lowerRatio = lower / total;
        double upperRatio = upper / total;

        // ===== ① 도지형 (Doji) =====
        if (bodyRatio <= 0.15) {
            return true;
        }

        // ===== ② 해머형 (아래꼬리 긴 캔들) =====
        boolean cond1 = lower >= 2 * body;     // 아래꼬리 길이 충분
        boolean cond2 = upper <= 0.3 * body;   // 윗꼬리 짧음
        boolean cond3 = lowerRatio > 0.6;      // 몸통이 전체 중 하단부에 위치

        if (cond1 && cond2 && cond3) {
            return true;
        }

        return false;
    }


    public static Fvg getFVG(List<MinuteCandleEntity> candles, int preRange) {

        if (candles == null || candles.size() < 3) return null;

        int lastIndex = candles.size() - 1;

        // 탐색 시작 지점 (최신 → preRange 만큼 뒤로)
        int startIndex = Math.max(2, lastIndex - preRange);

        // 최신 → 과거 방향으로 탐색
        for (int i = lastIndex; i >= startIndex; i--) {

            MinuteCandleEntity A = candles.get(i - 2);
            MinuteCandleEntity B = candles.get(i - 1); // B는 조건에는 필요 없지만 FVG 구조 확인용
            MinuteCandleEntity C = candles.get(i);

            long A_high = A.getTopPrice();
            long C_low  = C.getBottomPrice();

            // ==================================================
            // Bullish FVG 조건
            // 1. A_high < C_low : 빠른 상승으로 생긴 틈
            // ==================================================
            if (A_high < C_low) {

                // FVG 범위
                long fvgTop = C_low;    // 위쪽
                long fvgBottom = A_high; // 아래쪽
                long fvgSize = fvgTop - fvgBottom;

                Fvg fvg = new Fvg(fvgTop, fvgBottom, i, false);

                // ==================================================
                // Filled 여부 판단
                // 불균형 구간을 이후 봉이 다시 채웠는지 확인
                // ==================================================
                for (int j = i; j < candles.size(); j++) {
                    long high = candles.get(j).getTopPrice();
                    long low  = candles.get(j).getBottomPrice();

                    if (high >= fvgTop && low <= fvgBottom) {
                        fvg.filled = true;
                        break;
                    }
                }

                // 이미 filled 되었으면 skip
                if (fvg.filled) continue;

                return fvg;
            }
        }

        return null;
    }


    public static OrderBlock getOrderBlock(List<MinuteCandleEntity> candles) {
        int LENGTH = 60;
        if (candles == null || candles.size() < 2) return null;

        int end = candles.size() - 1;
        int start = Math.max(1, candles.size() - LENGTH);

        for (int i = end; i >= start; i--) {

            MinuteCandleEntity prev = candles.get(i - 1);
            MinuteCandleEntity cur  = candles.get(i);

            long prevOpen = prev.getStartPrice();
            long prevClose = prev.getEndPrice();
            long curOpen  = cur.getStartPrice();
            long curClose = cur.getEndPrice();

            // 1. 이전 캔들 음봉
            if (prevClose >= prevOpen) continue;

            // 2. 현재 캔들 양봉
            if (curClose <= curOpen) continue;

            // (NEW) 3. 현재 종가가 이전 시가보다 0.5% 이상 상승했는지
            double riseFromPrevOpen = ((double)(curClose - prevOpen) / prevOpen) * 100.0;
            if (riseFromPrevOpen < 0.5) continue;

            // 3. Engulfing 체크
            long prevBodyLow  = Math.min(prevOpen, prevClose);
            long prevBodyHigh = Math.max(prevOpen, prevClose);
            long curBodyLow  = Math.min(curOpen, curClose);
            long curBodyHigh = Math.max(curOpen, curClose);

            if (!(curBodyLow <= prevBodyLow && curBodyHigh >= prevBodyHigh)) continue;

//            // 4. 상승률 1% 이상
//            double change = ((double)(curClose - prevClose) / prevClose) * 100.0;
//            if (change < 1.0) continue;

            // 5. OB 터치 여부 체크
            boolean touched = false;
            long obHigh = prevBodyHigh;
            long obLow  = prevBodyLow;

            for (int j = i + 1; j < candles.size(); j++) {
                MinuteCandleEntity next = candles.get(j);
                long high = next.getTopPrice();
                long low  = next.getBottomPrice();

                if (high >= obLow && low <= obHigh) {
                    touched = true;
                    break;
                }
            }

            if (!touched) {
                long prevCandleLow = prev.getBottomPrice();

                return new OrderBlock(obHigh, obLow, i, false, prevCandleLow);
            }
        }

        return null;
    }

}
