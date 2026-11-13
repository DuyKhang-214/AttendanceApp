package com.khang.attendanceapp;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.khang.attendanceapp.utils.AppPrefs;

/**
 * Application: áp dụng chế độ Sáng/Tối đã lưu ngay khi ứng dụng khởi động.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        boolean dark = new AppPrefs(this).isDarkMode();
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
