package com.framework.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * DateUtils - Date/time formatting helpers used in test data and assertions.
 */
public final class DateUtils {

    public static final DateTimeFormatter DEFAULT_DATE      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DEFAULT_DATETIME  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter ISO_DATE          = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter TIMESTAMP_FILE    = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    public static final DateTimeFormatter DISPLAY_DATE      = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private DateUtils() {}

    public static String today() {
        return LocalDate.now().format(DEFAULT_DATE);
    }

    public static String todayIso() {
        return LocalDate.now().format(ISO_DATE);
    }

    public static String now() {
        return LocalDateTime.now().format(DEFAULT_DATETIME);
    }

    public static String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FILE);
    }

    public static String format(LocalDate date, DateTimeFormatter formatter) {
        return date.format(formatter);
    }

    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime.format(formatter);
    }

    public static LocalDate parseDate(String date, DateTimeFormatter formatter) {
        return LocalDate.parse(date, formatter);
    }

    public static LocalDate tomorrow() {
        return LocalDate.now().plusDays(1);
    }

    public static LocalDate yesterday() {
        return LocalDate.now().minusDays(1);
    }

    public static LocalDate daysFromNow(int days) {
        return LocalDate.now().plusDays(days);
    }

    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
