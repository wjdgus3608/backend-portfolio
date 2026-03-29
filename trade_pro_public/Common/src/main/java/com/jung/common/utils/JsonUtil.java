package com.jung.common.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.LinkedList;
import java.util.List;

public class JsonUtil {
    private static final JSONParser parser = new JSONParser();
    public static JSONObject strToJson(String str){
        try {
            return (JSONObject) parser.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<JSONObject> strToJsonList(String str){
        List<JSONObject> list = new LinkedList<>();
        try {
            JSONArray array = (JSONArray) parser.parse(str);
            for(int i=0; i<array.size(); i++){
                list.add((JSONObject) array.get(i));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return list;
    }
}
