package com.khang.attendanceapp.model;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import com.khang.attendanceapp.R;

import java.io.Serializable;

/** Một lượt điểm danh. */
public class AttendanceRecord implements Serializable {

    public static final String STATUS_PRESENT = "PRESENT";
    public static final String STATUS_LATE = "LATE";
    public static final String STATUS_EXCUSED = "EXCUSED";

    public long id;
    public String studentCode;
    public String studentName;
    public String className;
    public String sessionName;
    public long timestamp;
    public String status = STATUS_PRESENT;
    public String note;

    @StringRes
    public static int statusLabel(String status) {
        if (STATUS_LATE.equals(status)) return R.string.status_late;
        if (STATUS_EXCUSED.equals(status)) return R.string.status_excused;
        return R.string.status_present;
    }

    @ColorRes
    public static int statusColor(String status) {
        if (STATUS_LATE.equals(status)) return R.color.warning;
        if (STATUS_EXCUSED.equals(status)) return R.color.info;
        return R.color.success;
    }

    @ColorRes
    public static int statusBgColor(String status) {
        if (STATUS_LATE.equals(status)) return R.color.warning_bg;
        if (STATUS_EXCUSED.equals(status)) return R.color.info_bg;
        return R.color.success_bg;
    }
}
