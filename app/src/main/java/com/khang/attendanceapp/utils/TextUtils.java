package com.khang.attendanceapp.utils;

import java.text.Normalizer;
import java.util.Locale;

/** Xử lý chuỗi tiếng Việt: bỏ dấu để tìm kiếm không phân biệt dấu. */
public final class TextUtils {

    private TextUtils() {
    }

    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return n.toLowerCase(Locale.ROOT).trim();
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String safe(String s) {
        return s == null ? "" : s;
    }
}
