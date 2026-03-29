package com.jung.feedbot.feedbot.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class DateUtil {

    public static String getDate(){
        LocalDate today = LocalDate.now();

        // DateTimeFormatter를 사용하여 yyyyMMdd 형식으로 포맷
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return today.format(formatter);
    }

    public static String getDate(LocalDate today){
        // DateTimeFormatter를 사용하여 yyyyMMdd 형식으로 포맷
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return today.format(formatter);
    }

    public static String addDays(String dateStr, int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(dateStr, formatter);
        LocalDate newDate = date.plusDays(days);
        return newDate.format(formatter);
    }

    public static String addTradingDays(String dateStr, int days){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(dateStr, formatter);

        if (days == 0) {
            return date.format(formatter);
        }

        int daysMoved = 0;
        while (daysMoved < Math.abs(days)) {
            date = (days > 0) ? date.plusDays(1) : date.minusDays(1);
            if (isTradingDay(date)) {
                daysMoved++;
            }
        }

        return date.format(formatter);
    }

    private static boolean isTradingDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return !(dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
    }
}
