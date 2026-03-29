package com.jung.app.common;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .withZone(ZoneId.of(("Asia/Seoul"))); // 바이낸스 시간은 UTC 기준

    public static String millisToString(long millis) {
        return FORMATTER.format(Instant.ofEpochMilli(millis));
    }

    public static String getNowTime() {
        return ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    public static boolean is30MinutesPassed(String targetTime) {
        // 문자열을 LocalDateTime으로 변환
        LocalDateTime target = LocalDateTime.parse(targetTime, FORMATTER);

        // 현재 시간
        LocalDateTime now = LocalDateTime.now();

        // Duration 계산
        Duration duration = Duration.between(target, now);

        // 30분(30 * 60초) 이상이면 true
        return duration.toMinutes() >= 30;
    }

    public static boolean isNMinutesPassed(String targetTime, int overMinute) {
        // 문자열을 LocalDateTime으로 변환
        LocalDateTime target = LocalDateTime.parse(targetTime, FORMATTER);

        // 현재 시간
        LocalDateTime now = LocalDateTime.now();

        // Duration 계산
        Duration duration = Duration.between(target, now);

        // 30분(30 * 60초) 이상이면 true
        return duration.toMinutes() >= overMinute;
    }
}
