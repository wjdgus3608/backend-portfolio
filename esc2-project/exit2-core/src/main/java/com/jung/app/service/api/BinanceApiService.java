package com.jung.app.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jung.app.common.BinanceCandleParser;
import com.jung.app.domain.vo.Candle;
import com.jung.app.domain.vo.Order;
import com.jung.app.domain.vo.Position;
import com.jung.app.domain.vo.Stock;
import com.jung.app.domain.vo.em.BuySellType;
import com.jung.app.domain.vo.em.LongShortType;
import com.jung.app.domain.vo.em.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BinanceApiService implements ApiClientService{
    private static final String FUTURES_BASE_URL = "https://fapi.binance.com";
    private static final String KLINE_ENDPOINT = "/fapi/v1/klines";
    // 전역 상수로 포맷터 정의
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .withZone(ZoneId.of("Asia/Seoul"));

    @Value("${binance.api-key}")
    private String apiKey;
    @Value("${binance.secret-key}")
    private String secretKey;
    private final ObjectMapper mapper = new ObjectMapper();


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

    /** ----------------- 공용 HTTP GET ----------------- */
    private String sendGet(String endpoint, Map<String, String> params) throws Exception {
        long timestamp = System.currentTimeMillis();
        params.put("timestamp", String.valueOf(timestamp));

        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            queryBuilder.append(e.getKey()).append("=").append(e.getValue()).append("&");
        }
        // 서명
        String query = queryBuilder.toString();
        if (query.endsWith("&")) query = query.substring(0, query.length() - 1);
        String signature = BinanceSigner.sign(query, secretKey);

        String urlStr = FUTURES_BASE_URL + endpoint + "?" + query + "&signature=" + signature;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-MBX-APIKEY", apiKey);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Binance API Error: " + conn.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        return response.toString();
    }

    /** ----------------- 선물 포지션 조회 ----------------- */
    @Override
    public List<Position> getFuturesPositionsAsList() {
        try {
            JsonNode root = mapper.readTree(sendGet("/fapi/v2/positionRisk", new HashMap<>()));
            List<Position> positions = new ArrayList<>();

            for (JsonNode node : root) {
                double positionAmt = node.get("positionAmt").asDouble();
                if (positionAmt == 0) continue;

                String symbol = node.get("symbol").asText();
                double entryPrice = node.get("entryPrice").asDouble();
                double leverage = node.get("leverage").asDouble();
                long updateTime = node.get("updateTime").asLong();
                String timeAt = TIME_FORMATTER.format(Instant.ofEpochMilli(updateTime));

                LongShortType longShortType = positionAmt > 0 ? LongShortType.LONG : LongShortType.SHORT;

                Position position = Position.builder()
                        .positionId(symbol + "_" + longShortType)
                        .stock(Stock.builder().stockName(symbol).build())
                        .longShortType(longShortType)
                        .leverageRate(leverage)
                        .orderPrice(entryPrice)
                        .orderAmount(Math.abs(positionAmt))
                        .timeAt(timeAt)
                        .build();

                positions.add(position);
            }

            return positions;

        } catch (Exception e) {
            throw new RuntimeException("바이낸스 포지션 조회 실패", e);
        }
    }

    /** ----------------- 사용 가능한 USDT ----------------- */
    @Override
    public double getFuturesAvailableUSDT() {
        try {
            JsonNode root = mapper.readTree(sendGet("/fapi/v2/balance", new HashMap<>()));
            for (JsonNode node : root) {
                if ("USDT".equals(node.get("asset").asText())) {
                    return node.get("availableBalance").asDouble();
                }
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException("잔고 조회 실패", e);
        }
    }

    @Override
    public List<Order> getAllOrders(List<String> symbols) {
        try {
            List<Order> allOrders = new ArrayList<>();

            for (String symbol : symbols) {
                // 기본 파라미터
                Map<String, String> params = new HashMap<>();
                params.put("symbol", symbol);

                String response = sendGet("/fapi/v1/allOrders", params);
                JsonNode root = mapper.readTree(response);

                for (JsonNode node : root) {
                    double price = node.get("price").asDouble();
                    double qty = node.get("origQty").asDouble();
                    String side = node.get("side").asText(); // BUY / SELL
                    String positionSide = node.get("positionSide").asText(); // LONG / SHORT
                    double leverage = 1; // API에 직접 없으면 기본값
                    long updateTime = node.get("updateTime").asLong();
                    String timeAt = TIME_FORMATTER.format(Instant.ofEpochMilli(updateTime));

                    Order order = Order.builder()
                            .orderId(UUID.fromString(node.get("orderId").asText() + "-" + UUID.randomUUID()))
                            .stock(Stock.builder().stockName(symbol).build())
                            .buySellType("BUY".equals(side) ? BuySellType.BUY : BuySellType.SELL)
                            .longShortType("LONG".equals(positionSide) ? LongShortType.LONG : LongShortType.SHORT)
                            .leverageRate(leverage)
                            .orderPrice(price)
                            .orderAmount(qty)
                            .timeAt(timeAt)
                            .orderStatus(OrderStatus.valueOf(node.get("status").asText()))
                            .build();

                    allOrders.add(order);
                }
            }

            return allOrders;

        } catch (Exception e) {
            throw new RuntimeException("바이낸스 주문 조회 실패", e);
        }
    }

    @Override
    public Order createOrder(Order order) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("symbol", order.getStock().getStockName());
            params.put("side", order.getBuySellType() == BuySellType.BUY ? "BUY" : "SELL");
            params.put("positionSide", order.getLongShortType() == LongShortType.LONG ? "LONG" : "SHORT");
            params.put("type", "LIMIT"); // 필요에 따라 MARKET 등으로 변경 가능
            params.put("quantity", String.valueOf(order.getOrderAmount()));
            params.put("price", String.valueOf(order.getOrderPrice()));
            params.put("timeInForce", "GTC"); // Good Till Cancel

            String response = sendPost("/fapi/v1/order", params);
            JsonNode node = mapper.readTree(response);

            // 주문 결과를 Order 객체로 매핑
            order.setOrderId(UUID.fromString(node.get("orderId").asText() + "-" + UUID.randomUUID()));
            long updateTime = node.get("updateTime").asLong();
            order.setTimeAt(TIME_FORMATTER.format(Instant.ofEpochMilli(updateTime)));
            order.setOrderStatus(OrderStatus.valueOf(node.get("status").asText()));

            return order;

        } catch (Exception e) {
            throw new RuntimeException("바이낸스 주문 생성 실패", e);
        }
    }

    @Override
    public boolean deleteOrder(long orderId) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("orderId", String.valueOf(orderId));

            String response = sendDelete("/fapi/v1/order", params);
            JsonNode node = mapper.readTree(response);

            return "CANCELED".equals(node.get("status").asText());

        } catch (Exception e) {
            throw new RuntimeException("바이낸스 주문 취소 실패", e);
        }
    }

    private String sendPost(String endpoint, Map<String, String> params) throws Exception {
        // timestamp + signature 추가
        long timestamp = System.currentTimeMillis();
        params.put("timestamp", String.valueOf(timestamp));

        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            queryBuilder.append(e.getKey()).append("=").append(e.getValue()).append("&");
        }
        String query = queryBuilder.toString();
        if (query.endsWith("&")) query = query.substring(0, query.length() - 1);
        String signature = BinanceSigner.sign(query, secretKey);

        URL url = new URL(FUTURES_BASE_URL + endpoint + "?" + query + "&signature=" + signature);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-MBX-APIKEY", apiKey);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        return response.toString();
    }

    private String sendDelete(String endpoint, Map<String, String> params) throws Exception {
        long timestamp = System.currentTimeMillis();
        params.put("timestamp", String.valueOf(timestamp));

        StringBuilder queryBuilder = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            queryBuilder.append(e.getKey()).append("=").append(e.getValue()).append("&");
        }
        String query = queryBuilder.toString();
        if (query.endsWith("&")) query = query.substring(0, query.length() - 1);
        String signature = BinanceSigner.sign(query, secretKey);

        URL url = new URL(FUTURES_BASE_URL + endpoint + "?" + query + "&signature=" + signature);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("X-MBX-APIKEY", apiKey);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        return response.toString();
    }
}
