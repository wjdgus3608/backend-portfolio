package com.jung.app.logdb.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;


@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(length = 1000)
    private String log;
    @CreationTimestamp
    private LocalDateTime time;

    public static List<Log> stringToLogList(String str){
        List<Log> list = new LinkedList<>();
        JSONParser parser = new JSONParser();
        try {
            JSONObject object = (JSONObject) parser.parse(str);
            JSONArray jsonArray = (JSONArray) object.get("data");
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject data = (JSONObject) jsonArray.get(i);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnn");
                LocalDateTime dateTime = LocalDateTime.parse((String)data.get("time"), formatter);
                Log log = Log.builder()
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
