package com.khang.attendanceapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

/** Lưu cài đặt người dùng bằng SharedPreferences. */
public class AppPrefs {

    public static final int SORT_NEWEST = 0;
    public static final int SORT_OLDEST = 1;
    public static final int SORT_NAME = 2;

    private static final String FILE = "attendance_prefs";
    private static final String K_USER = "user_name";
    private static final String K_SESSION = "session_name";
    private static final String K_SOUND = "sound";
    private static final String K_VIBRATE = "vibrate";
    private static final String K_DARK = "dark_mode";
    private static final String K_LATE_ON = "late_enabled";
    private static final String K_START_H = "start_hour";
    private static final String K_START_M = "start_minute";
    private static final String K_SORT = "sort_order";

    private final SharedPreferences sp;

    public AppPrefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public String getUserName() {
        return sp.getString(K_USER, "");
    }

    public void setUserName(String v) {
        sp.edit().putString(K_USER, v).apply();
    }

    public String getSessionName() {
        return sp.getString(K_SESSION, "LING301 - Lập trình Android");
    }

    public void setSessionName(String v) {
        sp.edit().putString(K_SESSION, v).apply();
    }

    public boolean isSoundOn() {
        return sp.getBoolean(K_SOUND, true);
    }

    public void setSoundOn(boolean v) {
        sp.edit().putBoolean(K_SOUND, v).apply();
    }

    public boolean isVibrateOn() {
        return sp.getBoolean(K_VIBRATE, true);
    }

    public void setVibrateOn(boolean v) {
        sp.edit().putBoolean(K_VIBRATE, v).apply();
    }

    public boolean isDarkMode() {
        return sp.getBoolean(K_DARK, false);
    }

    public void setDarkMode(boolean v) {
        sp.edit().putBoolean(K_DARK, v).apply();
    }

    public boolean isLateEnabled() {
        return sp.getBoolean(K_LATE_ON, false);
    }

    public void setLateEnabled(boolean v) {
        sp.edit().putBoolean(K_LATE_ON, v).apply();
    }

    public int getStartHour() {
        return sp.getInt(K_START_H, 7);
    }

    public int getStartMinute() {
        return sp.getInt(K_START_M, 0);
    }

    public void setStartTime(int hour, int minute) {
        sp.edit().putInt(K_START_H, hour).putInt(K_START_M, minute).apply();
    }

    public int getSortOrder() {
        return sp.getInt(K_SORT, SORT_NEWEST);
    }

    public void setSortOrder(int v) {
        sp.edit().putInt(K_SORT, v).apply();
    }
}
