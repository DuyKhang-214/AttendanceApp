package com.khang.attendanceapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

import com.khang.attendanceapp.R;

/**
 * Màn hình chào: animation logo rồi chuyển sang MainActivity bằng Intent tường minh.
 */
public class SplashActivity extends BaseActivity {

    private static final long SPLASH_MS = 1900;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goMain = () -> {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.logoContainer);
        View ring = findViewById(R.id.logoRing);
        View title = findViewById(R.id.tvSplashTitle);
        View tagline = findViewById(R.id.tvSplashTagline);

        // Logo: phóng to bật nảy
        logo.setScaleX(0f);
        logo.setScaleY(0f);
        logo.animate().scaleX(1f).scaleY(1f).setDuration(700)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();

        // Vòng sáng đập nhẹ
        ring.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));

        // Chữ trượt lên + hiện dần
        float shift = 40 * getResources().getDisplayMetrics().density;
        title.setAlpha(0f);
        title.setTranslationY(shift);
        title.animate().alpha(1f).translationY(0f).setStartDelay(350).setDuration(500).start();
        tagline.setAlpha(0f);
        tagline.setTranslationY(shift);
        tagline.animate().alpha(0.85f).translationY(0f).setStartDelay(550).setDuration(500).start();

        handler.postDelayed(goMain, SPLASH_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(goMain);
        super.onDestroy();
    }
}
