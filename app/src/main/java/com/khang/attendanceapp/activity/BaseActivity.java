package com.khang.attendanceapp.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Activity cơ sở: bật giao diện tràn viền (edge-to-edge), icon status bar màu sáng
 * (vì header là nền gradient tối) và có sẵn hàm xử lý inset cho status bar / nav bar.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT));
    }

    /** Thêm padding top bằng chiều cao status bar (và vùng khuyết camera). */
    protected void applyTopInset(View view) {
        final int base = view.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), base + bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    /** Thêm padding bottom bằng chiều cao thanh điều hướng hệ thống. */
    protected void applyBottomInset(View view) {
        final int base = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), base + bars.bottom);
            return insets;
        });
    }
}
