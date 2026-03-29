package jo.jung.common.graph;

import jo.jung.domain.candle.Candle;
import jo.jung.domain.graph.BollingerBands;
import jo.jung.domain.graph.OrderBlock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GraphUtil {

    /**
     * 주어진 Candle Flux에서 최근 N개의 endPrice로 볼린저밴드를 계산
     */
    public static Mono<BollingerBands> calculateBollingerBands(Flux<Candle> candleFlux, int period, double k) {
        return candleFlux
                .takeLast(period) // 최근 N개만 사용
                .map(candle -> (double) (candle.getEndPrice()+candle.getBottomPrice()+candle.getTopPrice())/3) // long -> double
                .collect(() -> new double[period], (arr, val) -> {
                    for (int i = 0; i < period; i++) {
                        if (arr[i] == 0) {
                            arr[i] = val;
                            break;
                        }
                    }
                })
                .map(values -> {
                    int count = 0;
                    double sum = 0;
                    for (double v : values) {
                        if (v != 0) {
                            sum += v;
                            count++;
                        }
                    }
                    double mean = sum / count;

                    double variance = 0;
                    for (double v : values) {
                        if (v != 0) {
                            variance += Math.pow(v - mean, 2);
                        }
                    }
                    double stddev = Math.sqrt(variance / count);

                    // 반올림 처리
                    double roundedMean = round(mean, 4);
                    double roundedStddev = round(stddev, 4);
                    double upper = roundedMean + (k * roundedStddev);
                    double lower = roundedMean - (k * roundedStddev);

                    return new BollingerBands(upper, mean, lower);
                });
    }

    // 오버로딩: 기본 20개, k = 2
    public static Mono<BollingerBands> calculateBollingerBands(Flux<Candle> candleFlux) {
        return calculateBollingerBands(candleFlux, 20, 2.0);
    }

    // 소수점 n자리 반올림 함수
    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    public static Flux<Integer> getRSIList(Flux<Candle> candleFlux) {
        final int period = 14;

        return candleFlux
                .collectList() // Flux → List<Candle>
                .flatMapMany(candles -> {
                    List<Integer> rsiList = new ArrayList<>();

                    if (candles == null || candles.size() <= period) {
                        // 데이터가 부족하면 빈 Flux 반환
                        return Flux.fromIterable(rsiList);
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

                    // 초기 period 구간은 -1
                    for (int i = 0; i < period; i++) {
                        rsiList.add(-1);
                    }

                    // 첫 번째 RSI 계산
                    rsiList.add(calculateRSI(avgGain, avgLoss));

                    // 이후 RSI 계산 (EMA 방식)
                    for (int i = period + 1; i < candles.size(); i++) {
                        long diff = candles.get(i).getEndPrice() - candles.get(i - 1).getEndPrice();
                        double currentGain = diff > 0 ? diff : 0;
                        double currentLoss = diff < 0 ? Math.abs(diff) : 0;

                        avgGain = (avgGain * (period - 1) + currentGain) / period;
                        avgLoss = (avgLoss * (period - 1) + currentLoss) / period;

                        rsiList.add(calculateRSI(avgGain, avgLoss));
                    }

                    return Flux.fromIterable(rsiList);
                });
    }

    private static int calculateRSI(double avgGain, double avgLoss) {
        if (avgLoss == 0) {
            return 100;
        }
        double rs = avgGain / avgLoss;
        return (int) Math.round(100 - (100 / (1 + rs)));
    }

    public static OrderBlock getOrderBlock(List<Candle> candles) {
        int LENGTH = 60;
        if (candles == null || candles.size() < 2) return null;

        int end = candles.size() - 1;
        int start = Math.max(1, candles.size() - LENGTH);

        for (int i = end; i >= start; i--) {

            Candle prev = candles.get(i - 1);
            Candle cur  = candles.get(i);

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
                Candle next = candles.get(j);
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
