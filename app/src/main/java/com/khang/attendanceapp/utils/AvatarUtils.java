package com.khang.attendanceapp.utils;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

/** Tạo avatar chữ cái đầu với màu ổn định theo MSSV. */
public final class AvatarUtils {

    private static final int[] PALETTE = {
            0xFF4F46E5, 0xFF0EA5E9, 0xFF10B981, 0xFFF59E0B,
            0xFFEF4444, 0xFF8B5CF6, 0xFFEC4899, 0xFF14B8A6
    };

    private AvatarUtils() {
    }

    /** Theo thói quen tiếng Việt: lấy chữ cái đầu của TÊN (từ cuối cùng). */
    public static String initial(String name) {
        if (TextUtils.isBlank(name)) return "?";
        String[] parts = name.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.substring(0, 1).toUpperCase();
    }

    public static int colorFor(String seed) {
        int h = seed == null ? 0 : Math.abs(seed.hashCode());
        return PALETTE[h % PALETTE.length];
    }

    public static void bind(TextView tv, String name, String seed) {
        tv.setText(initial(name));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(colorFor(seed));
        tv.setBackground(bg);
        tv.setTextColor(Color.WHITE);
    }
}
