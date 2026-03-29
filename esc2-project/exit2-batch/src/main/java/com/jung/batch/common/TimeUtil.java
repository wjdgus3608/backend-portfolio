package com.jung.batch.common;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class TimeUtil {

    private static final SimpleDateFormat FORMAT =
            new SimpleDateFormat("yyyyMMddHHmmss");

    public static String plusOneMinute(String time) {
        try {
            Date date = FORMAT.parse(time);
            long next = date.getTime() + 60_000;
            return FORMAT.format(new Date(next));
        } catch (Exception e) {
            throw new RuntimeException("시간 파싱 실패: " + time, e);
        }
    }

    public static String plusMinutes(String time, int minutes) {
        return format(parse(time).plusMinutes(minutes));
    }

    public static boolean beforeOrEqual(String t1, String t2) {
        return t1.compareTo(t2) <= 0;
    }

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

    public static boolean is90MinutesPassed(String targetTime) {
        // 문자열을 LocalDateTime으로 변환
        LocalDateTime target = LocalDateTime.parse(targetTime, FORMATTER);

        // 현재 시간
        LocalDateTime now = LocalDateTime.now();

        // Duration 계산
        Duration duration = Duration.between(target, now);

        // 30분(30 * 60초) 이상이면 true
        return duration.toMinutes() >= 150;
    }

    public static long toEpochMilli(String yyyyMMddHHmmss) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        LocalDateTime ldt = LocalDateTime.parse(yyyyMMddHHmmss, formatter);

        return ldt
                .atZone(ZoneId.of("Asia/Seoul"))
                .toInstant()
                .toEpochMilli();
    }

    /** yyyyMMddHHmmss → LocalDateTime */
    public static LocalDateTime parse(String time) {
        return LocalDateTime.parse(time, FORMATTER);
    }

    /** LocalDateTime → yyyyMMddHHmmss */
    public static String format(LocalDateTime time) {
        return time.format(FORMATTER);
    }

    public static String toBucketTime(String timeAt, int minutes) {
        LocalDateTime t = TimeUtil.parse(timeAt);

        int bucketMinute = (t.getMinute() / minutes) * minutes;

        LocalDateTime bucketTime = t
                .withMinute(bucketMinute)
                .withSecond(0);

        return TimeUtil.format(bucketTime);
    }


    /** -N분 🔥 추가 */
    public static String minusMinutes(String time, int minutes) {
        return format(parse(time).minusMinutes(minutes));
    }
}

