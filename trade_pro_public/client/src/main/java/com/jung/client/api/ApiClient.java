package com.jung.client.api;

import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface ApiClient<T> {
    ResponseEntity<String> send(String msg) throws NoSuchAlgorithmException, UnsupportedEncodingException;
    List<T> searchAllCoins();
    List<T> searchMinuteCandle(int unit, String cName, String to, int count);
    T getMyBox();
    T getNowPrice(String cName);
    List<T> getNowInfo(String cName);
    ResponseEntity<T> buy(JSONObject json);
    ResponseEntity<T> sell(JSONObject json);
    void initRequestCnt();
    void upAndCheckRequestCnt();

}
