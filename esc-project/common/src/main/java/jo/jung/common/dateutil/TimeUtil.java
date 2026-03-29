package jo.jung.common.dateutil;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    /**
     * 오늘날짜구하기 "yyyyMMdd" 형식으로 반환
     */
    public static String getToday() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }

    /**
     * 현재 시간을 "HHmmss" 형식으로 반환
     */
    public static String getCurrentTimeHHmmss() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    public static String getTodayAddDays( int n) {
        return LocalDateTime.now().plusDays(n).format(DATE_FORMATTER);
    }

    /**
     * 현재 시간에 분 단위 덧셈
     */
    public static String addMinutes(String timeStr, long minutes) {
        LocalDateTime time = strToTime(timeStr);
        return time.plusMinutes(minutes).format(TIME_FORMATTER);
    }

    /**
     * 현재 시간에 분 단위 뺄셈
     */
    public static String subtractMinutes(String timeStr, long minutes) {
        LocalDateTime time = strToTime(timeStr);
        return time.minusMinutes(minutes).format(TIME_FORMATTER);
    }

    private static LocalDateTime strToTime(String timeStr){
        int hour = Integer.parseInt(timeStr.substring(0, 2));
        int minute = Integer.parseInt(timeStr.substring(2, 4));
        int second = Integer.parseInt(timeStr.substring(4, 6));

        LocalDateTime now = LocalDateTime.now();
        return LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute, second));
    }

    // yyyymmddhhmmss 형식의 시간 문자열 간 차이를 분 단위로 계산하는 함수
    public static long calculateTimeDifferenceInMinutes(String time1, String time2) {
        // 포맷 정의
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // 문자열을 LocalDateTime으로 파싱
        LocalDateTime dt1 = LocalDateTime.parse(time1, formatter);
        LocalDateTime dt2 = LocalDateTime.parse(time2, formatter);


        // 두 LocalDateTime 간의 차이를 Duration 객체로 구함
        Duration duration = Duration.between(dt1, dt2).abs();

        // 차이를 분 단위로 반환
        return duration.toMinutes();
    }

    /**
     * 주어진 두 yyyyMMddHHmmss 문자열 중에서 first가 second보다 이전인지 판단합니다.
     *
     * @param first  비교할 첫 번째 시간 문자열
     * @param second 비교할 두 번째 시간 문자열
     * @return true if first < second, false otherwise
     */
    public static boolean isBefore(String first, String second) {
        LocalDateTime dt1 = LocalDateTime.parse(first, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDateTime dt2 = LocalDateTime.parse(second, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return dt1.isBefore(dt2);
    }

    public static boolean isAfter(String first, String second) {
        LocalDateTime dt1 = LocalDateTime.parse(first, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDateTime dt2 = LocalDateTime.parse(second, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return dt1.isAfter(dt2);
    }

    public static boolean isAfterOrEqual(String first, String second) {
        LocalDateTime dt1 = LocalDateTime.parse(first, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDateTime dt2 = LocalDateTime.parse(second, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return dt1.isAfter(dt2) || dt1.isEqual(dt2);
    }

    public static boolean isAfterDate(String first, String second) {
        LocalDate dt1 = LocalDate.parse(first, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate dt2 = LocalDate.parse(second, DateTimeFormatter.ofPattern("yyyyMMdd"));
        return dt1.isAfter(dt2);
    }
}
