package com.khang.attendanceapp.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Các hàm tiện ích về ngày giờ. */
public final class DateUtils {

    private static final Locale VI = new Locale("vi", "VN");

    private DateUtils() {
    }

    public static long startOfDay(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** Mốc 0h của ngày cách hôm nay {@code daysAgo} ngày. */
    public static long startOfDayAgo(int daysAgo) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay(System.currentTimeMillis()));
        c.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return c.getTimeInMillis();
    }

    public static String formatTime(long ts) {
        return new SimpleDateFormat("HH:mm", VI).format(new Date(ts));
    }

    public static String formatDate(long ts) {
        return new SimpleDateFormat("dd/MM/yyyy", VI).format(new Date(ts));
    }

    public static String formatDateTime(long ts) {
        return new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", VI).format(new Date(ts));
    }

    public static String formatFileStamp(long ts) {
        return new SimpleDateFormat("yyyyMMdd_HHmm", VI).format(new Date(ts));
    }

    /** "Thứ Tư, 11/11/2026" */
    public static String formatFullDate(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return longDayName(c.get(Calendar.DAY_OF_WEEK)) + ", " + formatDate(ts);
    }

    public static String longDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                return "Thứ Hai";
            case Calendar.TUESDAY:
                return "Thứ Ba";
            case Calendar.WEDNESDAY:
                return "Thứ Tư";
            case Calendar.THURSDAY:
                return "Thứ Năm";
            case Calendar.FRIDAY:
                return "Thứ Sáu";
            case Calendar.SATURDAY:
                return "Thứ Bảy";
            default:
                return "Chủ Nhật";
        }
    }

    /** Nhãn ngắn cho biểu đồ: T2, T3, ..., CN. */
    public static String shortDayName(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        switch (c.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return "T2";
            case Calendar.TUESDAY:
                return "T3";
            case Calendar.WEDNESDAY:
                return "T4";
            case Calendar.THURSDAY:
                return "T5";
            case Calendar.FRIDAY:
                return "T6";
            case Calendar.SATURDAY:
                return "T7";
            default:
                return "CN";
        }
    }

    public static int minutesOfDay(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    public static int hourOfDay(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return c.get(Calendar.HOUR_OF_DAY);
    }
}
