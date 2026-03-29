package com.jung.app.service.chart;

import com.jung.app.common.PriceUtil;
import com.jung.app.domain.vo.BoxRange;
import com.jung.app.domain.vo.Candle;
import com.jung.app.domain.vo.OrderBlock;
import com.jung.app.domain.vo.SwingPoint;
import com.jung.app.domain.vo.em.LongShortType;
import com.jung.app.domain.vo.em.SwingPointType;
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
            if (riseFromPrevOpen < 0.2) continue;

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
            if (dropFromPrevOpen < 0.2) continue;

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



}
