package com.jung.domain.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LogVO {
    private long id;
    private String log;
    private LocalDateTime time;
    public static List<LogVO> stringToLogList(String str){
        List<LogVO> list = new LinkedList<>();
        JSONParser parser = new JSONParser();
        try {
            JSONObject object = (JSONObject) parser.parse(str);
            JSONArray jsonArray = (JSONArray) object.get("data");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject data = (JSONObject) jsonArray.get(i);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                LocalDateTime dateTime = LocalDateTime.parse((String)data.get("time"), formatter);
                LogVO log = LogVO.builder()
                        .id((Long) data.get("id"))
                        .log((String) data.get("log"))
                        .time(dateTime)
                        .build();
                list.add(log);
            }
            return list;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
