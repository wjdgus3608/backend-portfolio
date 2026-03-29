package com.jung.batch.api;

import com.jung.batch.common.BinanceCandleParser;
import com.jung.batch.common.TimeUtil;
import com.jung.batch.domain.Candle;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Service
public class BinanceApiService implements ApiClientService{
    private static final String FUTURES_BASE_URL = "https://fapi.binance.com";
    private static final String KLINE_ENDPOINT = "/fapi/v1/klines";

    @Override
    public List<Candle> getRecentCandlesByTicker(String ticker, String interval, int limit) {
        try {
            String urlStr = String.format(
                    "%s%s?symbol=%s&interval=%s&limit=%d",
                    FUTURES_BASE_URL,
                    KLINE_ENDPOINT,
                    ticker,
                    interval,
                    limit
            );

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException(
                        "Binance API Error: " + conn.getResponseCode()
                );
            }

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return BinanceCandleParser.parse(response.toString(),"5m");

        } catch (Exception e) {
            throw new RuntimeException("캔들 조회 실패", e);
        }
    }

    @Override
    public List<Candle> getCandlesByTickerAndPeriod(
            String ticker,
            String interval,
            String startTime,
            String endTime,
            int limit
    ) {
        try {
            long startMillis = TimeUtil.toEpochMilli(startTime);
            long endMillis   = TimeUtil.toEpochMilli(endTime);

            String urlStr = String.format(
                    "%s%s?symbol=%s&interval=%s&startTime=%d&endTime=%d&limit=%d",
                    FUTURES_BASE_URL,
                    KLINE_ENDPOINT,
                    ticker,
                    interval,
                    startMillis,
                    endMillis,
                    limit
            );

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException(
                        "Binance API Error: " + conn.getResponseCode()
                );
            }

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return BinanceCandleParser.parse(response.toString(), interval);

        } catch (Exception e) {
            throw new RuntimeException("기간별 캔들 조회 실패", e);
        }
    }
}
