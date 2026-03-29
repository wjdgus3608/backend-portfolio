package com.jung.client.api;

import com.google.gson.Gson;
import com.jung.client.api.ApiClient;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import com.jung.common.utils.JsonUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.*;


@Component
public class UpbitApiClient implements ApiClient {
    @Value("${my.AKey}")
    private String accessKey;
    @Value("${my.SKey}")
    private String secretKey;
    @Value("${ticket}")
    private String ticket;

    private static String authenticationToken;
    private static String jwtToken;
    private static int requestCnt = 0;
    private static int orderRequestCnt = 0;

    @Override
    public ResponseEntity<String> send(String msg) throws NoSuchAlgorithmException, UnsupportedEncodingException {

        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .sign(algorithm);

        authenticationToken = "Bearer " + jwtToken;
//        LogUtil.printLog(authenticationToken);
        upAndCheckRequestCnt();
        return null;
    }

    @Override
    public List<JSONObject> searchAllCoins() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.upbit.com/v1/market/all?isDetails=true"))
                .header("Accept", "application/json")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = null;
        try {
            response = HttpClient
                    .newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<JSONObject> list = filterKCoin(JsonUtil.strToJsonList(response.body()));
        List<JSONObject> ret = new ArrayList<>();
        //유의종목 걸러내기
        for(JSONObject obj:list){
            JSONObject coin = (JSONObject) obj;
            String warning = (String) coin.get("market_warning");
            if(!warning.equals("CAUTION"))
                ret.add(coin);
        }
//        System.out.println(list);
        upAndCheckRequestCnt();
        return ret;
    }

    @Override
    public List<JSONObject> searchMinuteCandle(int unit, String cName, String to, int count) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.upbit.com/v1/candles/minutes/"+unit+
                        "?market="+cName+
//                        "&to="+to+
                        "&count="+count))
                .header("Accept", "application/json")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
//        System.out.println(response.body());
        upAndCheckRequestCnt();
        return JsonUtil.strToJsonList(response.body());
    }

    @Override
    public List<JSONObject> getMyBox() {

        try {
            send("");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.upbit.com/v1/accounts"))
                .header("Accept", "application/json")
                .header("Authorization",authenticationToken)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        LogUtil.printLog(response.body());
        upAndCheckRequestCnt();
        return JsonUtil.strToJsonList(response.body());
    }

    @Override
    public Double getNowPrice(String cName) {
        List<JSONObject> list = getNowInfo(cName);
        if(list==null){
            return -1d;
        }

        Object oj = list.get(0).get("trade_price");
        if(oj instanceof Double){
            return (double) oj;
        }
        Double price =  ((Long) oj).doubleValue();
        return price;
    }

    @Override
    public List<JSONObject> getNowInfo(String cName) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.upbit.com/v1/ticker?markets="+cName))
                .header("Accept", "application/json")
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = null;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
//        System.out.println(response.body());
        upAndCheckRequestCnt();
        if(response.statusCode()==404 ||response.statusCode()==400){
            return null;
        }
        return JsonUtil.strToJsonList(response.body());
    }

    @Override
    public ResponseEntity<String> buy(JSONObject json) {
        HashMap<String, String> params = new HashMap<>();
        params.put("market", (String)json.get("market"));
        params.put("side", "bid");
        params.put("price", (String)json.get("price"));
        params.put("ord_type", "price");

        commonBuySell(params);
        upAndCheckOrderRequestCnt();

        return null;
    }

    @Override
    public ResponseEntity sell(JSONObject json) {
        HashMap<String, String> params = new HashMap<>();
        params.put("market", (String)json.get("market"));
        params.put("side", "ask");
        params.put("volume", (String)json.get("volume"));
        params.put("ord_type", "market");

        commonBuySell(params);
        upAndCheckOrderRequestCnt();

        double volume = Double.parseDouble((String) json.get("volume"));
        double nowPrice = (double) json.get("nowPrice");
        double earnMoney = Math.floor(calculateEarnMoney(volume,nowPrice)-Double.parseDouble(ticket)
                -(Double.parseDouble(ticket)*0.001));
        DecimalFormat decimalFormat = new DecimalFormat("###,###");
        String money = decimalFormat.format(earnMoney);
        if(earnMoney>=0){
            LogUtil.printLog("earn money +"+money);
            LogUtil.saveLog("earn money +"+money);
        }
        else{
            LogUtil.printLog("lost money "+money);
            LogUtil.saveLog("lost money "+money);
        }
        return null;
    }

    private void commonBuySell(HashMap<String, String> params){
        ArrayList<String> queryElements = new ArrayList<>();
        for(Map.Entry<String, String> entity : params.entrySet()) {
            queryElements.add(entity.getKey() + "=" + entity.getValue());
        }

        String queryString = String.join("&", queryElements.toArray(new String[0]));

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            md.update(queryString.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String queryHash = String.format("%0128x", new BigInteger(1, md.digest()));

        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(algorithm);

        authenticationToken = "Bearer " + jwtToken;

        try {
            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost("https://api.upbit.com/v1/orders");
            request.setHeader("Content-Type", "application/json");
            request.addHeader("Authorization", authenticationToken);
            request.setEntity(new StringEntity(new Gson().toJson(params)));

            CloseableHttpResponse response = client.execute(request);
            LogUtil.printLog(Integer.toString(response.getStatusLine().getStatusCode()));
//            LogUtil.saveLog(Integer.toString(response.getStatusLine().getStatusCode()));
            HttpEntity entity = response.getEntity();

//            System.out.println(EntityUtils.toString(entity, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initRequestCnt() {
        requestCnt = 0;
    }

    @Override
    public void upAndCheckRequestCnt() {
        requestCnt++;
        if(requestCnt>=9){
//            LogUtil.printLog("wait for request limit...");
//            LogUtil.saveLog("wait for request limit...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            initRequestCnt();
        }
    }

    public void initOrderRequestCnt() {
        orderRequestCnt = 0;
    }

    public void upAndCheckOrderRequestCnt() {
        orderRequestCnt++;
        if(orderRequestCnt>=3){
//            LogUtil.printLog("wait for orderRequest limit...");
//            LogUtil.saveLog("wait for orderRequest limit...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            initOrderRequestCnt();
        }
    }



    private List<JSONObject> filterKCoin(List<JSONObject> arr){
        List<JSONObject> list = new LinkedList<>();
        for(int i=0; i<arr.size(); i++){
            JSONObject json = arr.get(i);
            if(json.get("market").toString().contains("KRW")){
                list.add(json);
            }
        }
        return list;
    }

    private double calculateEarnMoney(double volume, double nowPrice){
        return volume*nowPrice;
    }

}
