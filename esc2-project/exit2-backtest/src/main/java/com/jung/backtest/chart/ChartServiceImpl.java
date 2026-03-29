package com.jung.backtest.chart;

import com.jung.backtest.common.PriceUtil;
import com.jung.backtest.domain.em.LongShortType;
import com.jung.backtest.domain.em.SwingPointType;
import com.jung.backtest.domain.vo.BoxRange;
import com.jung.backtest.domain.vo.Candle;
import com.jung.backtest.domain.vo.OrderBlock;
import com.jung.backtest.domain.vo.SwingPoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class ChartServiceImpl implements ChartService{

    private double k = 0.1;
    private int boxSize = 200;

    /*
    박스 점수표
    * 횟수별
    2회 -> 2점
    3회 -> 5점
    4회 -> 8점
    5회이상 -> 12점

    * 최신별
    10캔들 이내 -> 6점
    20캔들 이내 -> 4점
    40캔들 이내 -> 2점
    80캔들 이내 -> 0점
    80캔들 초과 -> -3점

    *총점 기준
    총점 9점 이상 -> 실전박스
    6~8점 -> 보조 박스
    6점 미만 -> 폐기
     */


    @Override
    public BoxRange getBoxRange(List<Candle> candles) {
        if (candles == null || candles.size() < boxSize) return null;

        List<Candle> recent = candles.subList(candles.size() - boxSize, candles.size());

        double atr = calculateATR(recent, 14);
        if (atr <= 0) return null;

        double tolerance = atr * k;

        List<SwingPoint> swings = findSwingPoints(recent);

        List<SwingPoint> highs = swings.stream()
                .filter(p -> p.getType() == SwingPointType.HIGH)
                .sorted(Comparator.comparingDouble(SwingPoint::getPrice))
                .toList();

        List<SwingPoint> lows = swings.stream()
                .filter(p -> p.getType() == SwingPointType.LOW)
                .sorted(Comparator.comparingDouble(SwingPoint::getPrice))
                .toList();

        List<List<SwingPoint>> highClusters =
                filterValidClusters(clusterSwingPoints(highs, tolerance), atr);

        List<List<SwingPoint>> lowClusters =
                filterValidClusters(clusterSwingPoints(lows, tolerance), atr);

        if (highClusters.isEmpty() || lowClusters.isEmpty()) return null;

        BoxRange bestBox = null;
        int bestScore = Integer.MIN_VALUE;

        for (List<SwingPoint> highCluster : highClusters) {
            for (List<SwingPoint> lowCluster : lowClusters) {

                double top = highCluster.stream().mapToDouble(SwingPoint::getPrice).max().orElse(0);
                double bottom = lowCluster.stream().mapToDouble(SwingPoint::getPrice).min().orElse(0);

                if (top <= bottom) continue;

                // 수익성 필터 (기존 유지)
                if (PriceUtil.getEarnMoney(
                        bottom,
                        (top + bottom) / 2,
                        2000 / bottom,
                        LongShortType.LONG
                ) < 3) continue;

                int score = calculateBoxScore(highCluster, lowCluster, recent.size());

                if (score >= 9 && score > bestScore) {
                    bestScore = score;
                    bestBox = BoxRange.builder()
                            .top(top)
                            .bottom(bottom)
                            .middle((top + bottom) / 2)
                            .size(top - bottom)
                            .atr(atr)
                            .startTimeAt(recent.get(0).getTimeAt())
                            .endTimeAt(recent.get(recent.size() - 1).getTimeAt())
                            .build();
                }
            }
        }

        return bestBox; // 실전박스 없으면 null
    }


    /*
        [거래량 + 꼬리 기반 박스 점수표]

        * 거래량 조건
          - 현재 거래량 > 최근 20캔들 평균 거래량 * 1.8

        * 캔들 형태 조건
          - 하단 (망치형)
            · 아래꼬리 ≥ 몸통 * 2
            · 위꼬리 ≤ 몸통 * 0.5

          - 상단 (역망치형)
            · 위꼬리 ≥ 몸통 * 2
            · 아래꼬리 ≤ 몸통 * 0.5

        * 가격 근접 조건
          - 고가(상단) 또는 저가(하단)가
            박스 상/하단 가격과 ATR * 0.5 이내

        * 발생 횟수별 점수 (상단 / 하단 각각 계산)
          1회  → 2점
          2회  → 4점
          3회  → 7점
          4회 이상 → 10점

        * 최근성 보너스
          - 최근 20캔들 이내 발생 → +3점
          - 최근 40캔들 이내 발생 → +1점

        * 총점 기준
          총점 9점 이상  → 실전 박스
          6 ~ 8점       → 보조 박스
          6점 미만      → 폐기
        */

    @Override
    public BoxRange getBoxRange2(List<Candle> candles) {
        if (candles == null || candles.size() < boxSize) return null;

        List<Candle> recent = candles.subList(candles.size() - boxSize, candles.size());

        double atr = calculateATR(recent, 14);
        if (atr <= 0) return null;

        double tolerance = atr * k;

        List<SwingPoint> swings = findSwingPoints(recent);

        List<SwingPoint> highs = swings.stream()
                .filter(p -> p.getType() == SwingPointType.HIGH)
                .sorted(Comparator.comparingDouble(SwingPoint::getPrice))
                .toList();

        List<SwingPoint> lows = swings.stream()
                .filter(p -> p.getType() == SwingPointType.LOW)
                .sorted(Comparator.comparingDouble(SwingPoint::getPrice))
                .toList();

        List<List<SwingPoint>> highClusters =
                filterValidClusters(clusterSwingPoints(highs, tolerance), atr);

        List<List<SwingPoint>> lowClusters =
                filterValidClusters(clusterSwingPoints(lows, tolerance), atr);

        if (highClusters.isEmpty() || lowClusters.isEmpty()) return null;

        BoxRange bestBox = null;
        int bestScore = Integer.MIN_VALUE;

        for (List<SwingPoint> highCluster : highClusters) {
            for (List<SwingPoint> lowCluster : lowClusters) {

                double top = highCluster.stream().mapToDouble(SwingPoint::getPrice).max().orElse(0);
                double bottom = lowCluster.stream().mapToDouble(SwingPoint::getPrice).min().orElse(0);

                if (top <= bottom) continue;

                // 수익성 필터 유지
                if (PriceUtil.getEarnMoney(
                        bottom,
                        (top + bottom) / 2,
                        2000 / bottom,
                        LongShortType.LONG
                ) < 3) continue;

                int score = calculateVolumeWickScore(recent, top, bottom, atr);

                if (score >= 6 && score > bestScore) {
                    bestScore = score;
                    bestBox = BoxRange.builder()
                            .top(top)
                            .bottom(bottom)
                            .middle((top + bottom) / 2)
                            .size(top - bottom)
                            .atr(atr)
                            .startTimeAt(recent.get(0).getTimeAt())
                            .endTimeAt(recent.get(recent.size() - 1).getTimeAt())
                            .build();
                }
            }
        }

        return bestBox;
    }

    private int calculateRecencyScore(
            int lastSignalIndex,
            int candleSize
    ) {
        int diff = candleSize - 1 - lastSignalIndex;

        if (diff <= 10) return 6;
        if (diff <= 20) return 4;
        if (diff <= 40) return 2;
        if (diff <= 80) return 0;
        return -3;
    }

    /**
     * 거래량 급증 + 망치형/역망치형 기반 박스 점수 계산
     *
     * - 상단/하단 각각 점수 계산
     * - 해당 캔들 기준 최근 20캔들 평균 거래량과 비교하여 거래량 급증 체크
     * - 횟수 점수: 2회→2점, 3회→5점, 4회→8점, 5회 이상→12점
     * - 최근성 점수: 기존 점수표 그대로 적용 (10/20/40/80 캔들)
     * - 총점 = 상단 점수 + 하단 점수 + 최근성 점수
     *
     * @param candles  박스 후보 캔들 리스트 (boxSize 길이)
     * @param top      박스 상단 가격
     * @param bottom   박스 하단 가격
     * @param atr      최근 ATR 값
     * @return 총점 (횟수 + 최근성 점수)
     */
    private int calculateVolumeWickScore(
            List<Candle> candles,
            double top,
            double bottom,
            double atr
    ) {
        int highCount = 0;           // 상단 시그널 발생 횟수
        int lowCount = 0;            // 하단 시그널 발생 횟수
        int lastHighIndex = -1;      // 가장 최근 상단 시그널 위치
        int lastLowIndex = -1;       // 가장 최근 하단 시그널 위치

        for (int i = 0; i < candles.size(); i++) {
            Candle c = candles.get(i);

            double body = Math.abs(c.getEndPrice() - c.getStartPrice());
            if (body == 0) continue; // 몸통 없는 캔들 무시

            double upperWick = c.getTopPrice() - Math.max(c.getEndPrice(), c.getStartPrice());
            double lowerWick = Math.min(c.getEndPrice(), c.getStartPrice()) - c.getBottomPrice();

            // 하단: 망치형
            boolean isHammer = lowerWick >= body * 2 && upperWick <= body * 0.5;

            // 상단: 역망치형
            boolean isInvertedHammer = upperWick >= body * 2 && lowerWick <= body * 0.5;

            // 해당 캔들 이전 20캔들 평균 거래량 계산
            int startIdx = Math.max(0, i - 20);
            double avgVolume = candles.subList(startIdx, i).stream()
                    .mapToDouble(Candle::getTradeAmount)
                    .average()
                    .orElse(0);

            boolean volumeSpike = c.getTradeAmount() > avgVolume * 1.8;

            // 상단 신호
            if (Math.abs(c.getTopPrice() - top) <= atr * 0.5 && isInvertedHammer && volumeSpike) {
                highCount++;
                lastHighIndex = i;
            }

            // 하단 신호
            if (Math.abs(c.getBottomPrice() - bottom) <= atr * 0.5 && isHammer && volumeSpike) {
                lowCount++;
                lastLowIndex = i;
            }
        }

        int score = 0;

        // 횟수 점수 계산 (기존 점수표 그대로)
        score += convertCountToScore(highCount);
        score += convertCountToScore(lowCount);

        // 최근성 점수 계산 (기존 로직과 동일)
        if (lastHighIndex >= 0) {
            score += calculateRecencyScore(lastHighIndex, candles.size());
        }
        if (lastLowIndex >= 0) {
            score += calculateRecencyScore(lastLowIndex, candles.size());
        }

        return score;
    }

    private int convertCountToScore(int count) {
        if (count >= 5) return 12;
        if (count == 4) return 8;
        if (count == 3) return 5;
        if (count == 2) return 2;
        return 0;
    }


    private int calculateBoxScore(
            List<SwingPoint> highCluster,
            List<SwingPoint> lowCluster,
            int totalCandleSize
    ) {
        int score = 0;

        // 1️⃣ 횟수 점수 (high + low 터치 합산)
        int touchCount = highCluster.size() + lowCluster.size();
        if (touchCount == 2) score += 2;
        else if (touchCount == 3) score += 5;
        else if (touchCount == 4) score += 8;
        else if (touchCount >= 5) score += 12;

        // 2️⃣ 최신성 점수 (가장 최근 터치 기준)
        int lastTouchIndex = Stream.concat(highCluster.stream(), lowCluster.stream())
                .mapToInt(SwingPoint::getIndex)   // 🔴 SwingPoint에 index 필요
                .max()
                .orElse(0);

        int candlesAgo = totalCandleSize - lastTouchIndex - 1;

        if (candlesAgo <= 10) score += 6;
        else if (candlesAgo <= 20) score += 4;
        else if (candlesAgo <= 40) score += 2;
        else if (candlesAgo <= 80) score += 0;
        else score -= 3;

        return score;
    }



    private List<List<SwingPoint>> filterValidClusters(
            List<List<SwingPoint>> clusters, double atr) {

        return clusters.stream()
                .filter(c -> c.size() >= 2)
                .filter(c -> {
                    double max = c.stream().mapToDouble(SwingPoint::getPrice).max().orElse(0);
                    double min = c.stream().mapToDouble(SwingPoint::getPrice).min().orElse(0);
                    return (max - min) <= atr * k;
                })
                .toList();
    }

    private List<SwingPoint> selectMostRecent(List<List<SwingPoint>> clusters) {
        return clusters.stream()
                .max(Comparator.comparingInt(
                        c -> c.stream().mapToInt(SwingPoint::getIndex).max().orElse(0)
                ))
                .orElse(null);
    }


    @Override
    public List<SwingPoint> findSwingPoints(List<Candle> candles) {
        List<SwingPoint> points = new ArrayList<>();

        for (int i = 2; i < candles.size() - 2; i++) {
            Candle c = candles.get(i);

            boolean swingHigh =
                    c.getTopPrice() > candles.get(i - 1).getTopPrice() &&
                            c.getTopPrice() > candles.get(i - 2).getTopPrice() &&
                            c.getTopPrice() > candles.get(i + 1).getTopPrice() &&
                            c.getTopPrice() > candles.get(i + 2).getTopPrice();

            boolean swingLow =
                    c.getBottomPrice() < candles.get(i - 1).getBottomPrice() &&
                            c.getBottomPrice() < candles.get(i - 2).getBottomPrice() &&
                            c.getBottomPrice() < candles.get(i + 1).getBottomPrice() &&
                            c.getBottomPrice() < candles.get(i + 2).getBottomPrice();

            if (swingHigh) {
                points.add(new SwingPoint(c.getTopPrice(), i, SwingPointType.HIGH));
            }
            if (swingLow) {
                points.add(new SwingPoint(c.getBottomPrice(), i, SwingPointType.LOW));
            }
        }

        return points;
    }

    private List<List<SwingPoint>> clusterSwingPoints(List<SwingPoint> points, double tolerance) {
        List<List<SwingPoint>> clusters = new ArrayList<>();
        List<SwingPoint> current = new ArrayList<>();

        for (SwingPoint p : points) {
            if (current.isEmpty()) {
                current.add(p);
                continue;
            }

            double lastPrice = current.get(current.size() - 1).getPrice();
            if (Math.abs(p.getPrice() - lastPrice) <= tolerance) {
                current.add(p);
            } else {
                clusters.add(new ArrayList<>(current));
                current.clear();
                current.add(p);
            }
        }

        if (!current.isEmpty()) {
            clusters.add(current);
        }

        return clusters;
    }

    private double calculateATR(List<Candle> candles, int period) {
        if (candles.size() <= period) return 0;

        List<Double> trs = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            Candle curr = candles.get(i);
            Candle prev = candles.get(i - 1);

            double tr = Math.max(
                    curr.getTopPrice() - curr.getBottomPrice(),
                    Math.max(
                            Math.abs(curr.getTopPrice() - prev.getEndPrice()),
                            Math.abs(curr.getBottomPrice() - prev.getEndPrice())
                    )
            );
            trs.add(tr);
        }

        return trs.stream()
                .skip(trs.size() - period)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }


    @Override
    public OrderBlock getUpOrderBlock(List<Candle> candles) {
        int LENGTH = 60;
        if (candles == null || candles.size() < 2) return null;

        int end = candles.size() - 1;
        int start = Math.max(1, candles.size() - LENGTH);

        for (int i = end; i >= start; i--) {

            Candle prev = candles.get(i - 1);
            Candle cur  = candles.get(i);

            double prevOpen = prev.getStartPrice();
            double prevClose = prev.getEndPrice();
            double curOpen  = cur.getStartPrice();
            double curClose = cur.getEndPrice();

            // 1. 이전 캔들 음봉
            if (prevClose >= prevOpen) continue;

            // 2. 현재 캔들 양봉
            if (curClose <= curOpen) continue;

            // (NEW) 3. 현재 종가가 이전 시가보다 0.5% 이상 상승했는지
            double riseFromPrevOpen = ((double)(curClose - prevOpen) / prevOpen) * 100.0;
            if (riseFromPrevOpen < 0.5) continue;

            // 3. Engulfing 체크
            double prevBodyLow  = Math.min(prevOpen, prevClose);
            double prevBodyHigh = Math.max(prevOpen, prevClose);
            double curBodyLow  = Math.min(curOpen, curClose);
            double curBodyHigh = Math.max(curOpen, curClose);

            if (!(curBodyLow <= prevBodyLow && curBodyHigh >= prevBodyHigh)) continue;

//            // 4. 상승률 1% 이상
//            double change = ((double)(curClose - prevClose) / prevClose) * 100.0;
//            if (change < 1.0) continue;

            // 5. OB 터치 여부 체크
            boolean touched = false;
            double obHigh = prevBodyHigh;
            double obLow  = prevBodyLow;

            for (int j = i + 1; j < candles.size(); j++) {
                Candle next = candles.get(j);
                double high = next.getTopPrice();
                double low  = next.getBottomPrice();

                if (high >= obLow && low <= obHigh) {
                    touched = true;
                    break;
                }
            }

            if (!touched) {
                double prevCandleLow = prev.getBottomPrice();

                List<SwingPoint> swingPoints = findSwingPoints(candles.subList(Math.max(0, candles.size() - 30), candles.size() - 1));
                Optional<SwingPoint> latestHigh = swingPoints.stream()
                        .filter(sp -> sp.getType() == SwingPointType.HIGH)
                        .max(Comparator.comparingInt(SwingPoint::getIndex));
                if(latestHigh.isEmpty()) return null;

                if (PriceUtil.getEarnMoney(
                        obHigh,
                        latestHigh.get().getPrice(),
                        2000 / obHigh,
                        LongShortType.LONG
                ) < 3) return null;

                return new OrderBlock(obHigh, obLow, i, false, prevCandleLow);
            }
        }

        return null;
    }

    @Override
    public OrderBlock getDownOrderBlock(List<Candle> candles) {
        int LENGTH = 60;
        if (candles == null || candles.size() < 2) return null;

        int end = candles.size() - 1;
        int start = Math.max(1, candles.size() - LENGTH);

        for (int i = end; i >= start; i--) {

            Candle prev = candles.get(i - 1);
            Candle cur  = candles.get(i);

            double prevOpen = prev.getStartPrice();
            double prevClose = prev.getEndPrice();
            double curOpen  = cur.getStartPrice();
            double curClose = cur.getEndPrice();

            // 1. 이전 캔들 양봉
            if (prevClose <= prevOpen) continue;

            // 2. 현재 캔들 음봉
            if (curClose >= curOpen) continue;

            // 3. 현재 종가가 이전 시가보다 0.5% 이상 하락했는지
            double dropFromPrevOpen = ((prevOpen - curClose) / prevOpen) * 100.0;
            if (dropFromPrevOpen < 0.5) continue;

            // 4. Bearish Engulfing 체크
            double prevBodyLow  = Math.min(prevOpen, prevClose);
            double prevBodyHigh = Math.max(prevOpen, prevClose);
            double curBodyLow   = Math.min(curOpen, curClose);
            double curBodyHigh  = Math.max(curOpen, curClose);

            if (!(curBodyHigh >= prevBodyHigh && curBodyLow <= prevBodyLow)) continue;

            // 5. OB 터치 여부 체크
            boolean touched = false;
            double obHigh = prevBodyHigh;
            double obLow  = prevBodyLow;

            for (int j = i + 1; j < candles.size(); j++) {
                Candle next = candles.get(j);
                double high = next.getTopPrice();
                double low  = next.getBottomPrice();

                if (high >= obLow && low <= obHigh) {
                    touched = true;
                    break;
                }
            }

            if (!touched) {
                double prevCandleHigh = prev.getTopPrice();

                List<SwingPoint> swingPoints = findSwingPoints(
                        candles.subList(Math.max(0, candles.size() - 30), candles.size() - 1)
                );

                Optional<SwingPoint> latestLow = swingPoints.stream()
                        .filter(sp -> sp.getType() == SwingPointType.LOW)
                        .max(Comparator.comparingInt(SwingPoint::getIndex));
                if (latestLow.isEmpty()) return null;

                if (PriceUtil.getEarnMoney(
                        obLow,
                        latestLow.get().getPrice(),
                        2000 / obLow,
                        LongShortType.SHORT
                ) < 3) return null;

                return new OrderBlock(obHigh, obLow, i, true, prevCandleHigh);
            }
        }

        return null;
    }

    @Override
    public List<OrderBlock> getRecentUpOrderBlocks(List<Candle> candles, int maxCount) {
        int LENGTH = 60;
        List<OrderBlock> foundOBs = new ArrayList<>();
        if (candles == null || candles.size() < 2) return foundOBs;

        int end = candles.size() - 1;
        int start = Math.max(1, candles.size() - LENGTH);

        for (int i = end; i >= start && foundOBs.size() < maxCount; i--) {

            Candle prev = candles.get(i - 1);
            Candle cur  = candles.get(i);

            double prevOpen = prev.getStartPrice();
            double prevClose = prev.getEndPrice();
            double curOpen  = cur.getStartPrice();
            double curClose = cur.getEndPrice();

            // 1. 이전 캔들 음봉
            if (prevClose >= prevOpen) continue;

            // 2. 현재 캔들 양봉
            if (curClose <= curOpen) continue;

            // 3. 현재 종가가 이전 시가보다 0.5% 이상 상승
            double riseFromPrevOpen = ((curClose - prevOpen) / prevOpen) * 100.0;
            if (riseFromPrevOpen < 0.5) continue;

            // Engulfing 체크
            double prevBodyLow  = Math.min(prevOpen, prevClose);
            double prevBodyHigh = Math.max(prevOpen, prevClose);
            double curBodyLow  = Math.min(curOpen, curClose);
            double curBodyHigh = Math.max(curOpen, curClose);

            if (!(curBodyLow <= prevBodyLow && curBodyHigh >= prevBodyHigh)) continue;

            // OB 터치 여부
            double obHigh = prevBodyHigh;
            double obLow  = prevBodyLow;
            double prevCandleLow = prev.getBottomPrice();

            List<SwingPoint> swingPoints = findSwingPoints(
                    candles.subList(Math.max(0, candles.size() - 30), candles.size() - 1)
            );
            Optional<SwingPoint> latestHigh = swingPoints.stream()
                    .filter(sp -> sp.getType() == SwingPointType.HIGH)
                    .max(Comparator.comparingInt(SwingPoint::getIndex));
            if (latestHigh.isEmpty()) continue;

            if (PriceUtil.getEarnMoney(
                    obHigh,
                    latestHigh.get().getPrice(),
                    2000 / obHigh,
                    LongShortType.LONG
            ) < 3) continue;

            foundOBs.add(new OrderBlock(obHigh, obLow, i, false, prevCandleLow));
        }

        return foundOBs;
    }

    @Override
    public List<OrderBlock> getRecentDownOrderBlocks(List<Candle> candles, int maxCount) {
        int LENGTH = 60;
        List<OrderBlock> foundOBs = new ArrayList<>();
        if (candles == null || candles.size() < 2) return foundOBs;

        int end = candles.size() - 1;
        int start = Math.max(1, candles.size() - LENGTH);

        for (int i = end; i >= start && foundOBs.size() < maxCount; i--) {

            Candle prev = candles.get(i - 1);
            Candle cur  = candles.get(i);

            double prevOpen = prev.getStartPrice();
            double prevClose = prev.getEndPrice();
            double curOpen  = cur.getStartPrice();
            double curClose = cur.getEndPrice();

            // 1. 이전 캔들 양봉
            if (prevClose <= prevOpen) continue;

            // 2. 현재 캔들 음봉
            if (curClose >= curOpen) continue;

            // 3. 현재 종가가 이전 시가보다 0.5% 이상 하락
            double dropFromPrevOpen = ((prevOpen - curClose) / prevOpen) * 100.0;
            if (dropFromPrevOpen < 0.5) continue;

            // Bearish Engulfing 체크
            double prevBodyLow  = Math.min(prevOpen, prevClose);
            double prevBodyHigh = Math.max(prevOpen, prevClose);
            double curBodyLow   = Math.min(curOpen, curClose);
            double curBodyHigh  = Math.max(curOpen, curClose);

            if (!(curBodyHigh >= prevBodyHigh && curBodyLow <= prevBodyLow)) continue;

            // OB 터치 여부
            double obHigh = prevBodyHigh;
            double obLow  = prevBodyLow;
            double prevCandleHigh = prev.getTopPrice();

            List<SwingPoint> swingPoints = findSwingPoints(
                    candles.subList(Math.max(0, candles.size() - 30), candles.size() - 1)
            );
            Optional<SwingPoint> latestLow = swingPoints.stream()
                    .filter(sp -> sp.getType() == SwingPointType.LOW)
                    .max(Comparator.comparingInt(SwingPoint::getIndex));
            if (latestLow.isEmpty()) continue;

            if (PriceUtil.getEarnMoney(
                    obLow,
                    latestLow.get().getPrice(),
                    2000 / obLow,
                    LongShortType.SHORT
            ) < 3) continue;

            foundOBs.add(new OrderBlock(obHigh, obLow, i, true, prevCandleHigh));
        }

        return foundOBs;
    }



}
