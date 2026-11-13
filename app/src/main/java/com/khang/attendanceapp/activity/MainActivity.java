package com.khang.attendanceapp.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.khang.attendanceapp.R;
import com.khang.attendanceapp.fragment.HistoryFragment;
import com.khang.attendanceapp.fragment.HomeFragment;
import com.khang.attendanceapp.fragment.SettingsFragment;
import com.khang.attendanceapp.fragment.StudentsFragment;

/**
 * Màn hình chính: Toolbar (Options Menu do từng Fragment cung cấp) +
 * BottomNavigationView (Navigation bar) để chuyển giữa 4 Fragment.
 */
public class MainActivity extends BaseActivity {

    private static final String KEY_TAB = "current_tab";

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNav;
    private ExtendedFloatingActionButton fab;
    private int currentTab = R.id.nav_home;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyTopInset(findViewById(R.id.header));

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bottomNav = findViewById(R.id.bottomNav);
        fab = findViewById(R.id.fab);

        // Sự kiện: chọn mục trên Navigation bar
        bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });

        // Sự kiện: nhấn FAB -> mở màn hình tương ứng bằng Intent tường minh
        fab.setOnClickListener(v -> {
            if (currentTab == R.id.nav_students) {
                startActivity(new Intent(this, AddStudentActivity.class));
            } else {
                startActivity(new Intent(this, ScanActivity.class));
            }
        });

        if (savedInstanceState == null) {
            showTab(R.id.nav_home);
        } else {
            currentTab = savedInstanceState.getInt(KEY_TAB, R.id.nav_home);
            updateChrome(currentTab);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, currentTab);
    }

    /** Cho Fragment gọi để chuyển tab (ví dụ "Xem tất cả" ở trang chủ). */
    public void navigateTo(int menuId) {
        bottomNav.setSelectedItemId(menuId);
    }

    private void showTab(int id) {
        Fragment fragment;
        if (id == R.id.nav_history) {
            fragment = new HistoryFragment();
        } else if (id == R.id.nav_students) {
            fragment = new StudentsFragment();
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new HomeFragment();
        }
        currentTab = id;
        updateChrome(id);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /** Cập nhật tiêu đề Toolbar và FAB theo tab. */
    private void updateChrome(int id) {
        if (id == R.id.nav_history) {
            toolbar.setTitle(R.string.title_history);
            fab.setText(R.string.fab_scan);
            fab.setIconResource(R.drawable.ic_scan);
            fab.show();
        } else if (id == R.id.nav_students) {
            toolbar.setTitle(R.string.title_students);
            fab.setText(R.string.fab_add_student);
            fab.setIconResource(R.drawable.ic_add);
            fab.show();
        } else if (id == R.id.nav_settings) {
            toolbar.setTitle(R.string.title_settings);
            fab.hide();
        } else {
            toolbar.setTitle(R.string.app_name);
            fab.setText(R.string.fab_scan);
            fab.setIconResource(R.drawable.ic_scan);
            fab.show();
        }
    }
}
