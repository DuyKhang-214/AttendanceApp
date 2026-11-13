package com.khang.attendanceapp.fragment;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.khang.attendanceapp.R;
import com.khang.attendanceapp.activity.AddStudentActivity;
import com.khang.attendanceapp.activity.MainActivity;
import com.khang.attendanceapp.activity.RecordDetailActivity;
import com.khang.attendanceapp.activity.ScanActivity;
import com.khang.attendanceapp.adapter.AttendanceAdapter;
import com.khang.attendanceapp.database.DatabaseHelper;
import com.khang.attendanceapp.model.AttendanceRecord;
import com.khang.attendanceapp.utils.AppPrefs;
import com.khang.attendanceapp.utils.DateUtils;
import com.khang.attendanceapp.utils.DialogUtils;
import com.khang.attendanceapp.utils.ExportUtils;
import com.khang.attendanceapp.utils.TextUtils;
import com.khang.attendanceapp.view.BarChartView;

import java.util.List;

/** Tab "Trang chủ": lời chào, nút quét, thống kê, biểu đồ 7 ngày, điểm danh gần đây. */
public class HomeFragment extends Fragment {

    private DatabaseHelper db;
    private AppPrefs prefs;

    private TextView tvGreeting, tvDate, tvSessionName;
    private TextView tvStatPresent, tvStatStudents, tvStatTotal;
    private TextView tvRatePercent, tvRateDesc, tvRecentEmpty;
    private CircularProgressIndicator progressRate;
    private BarChartView barChart;
    private AttendanceAdapter recentAdapter;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());
        prefs = new AppPrefs(requireContext());

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvDate = view.findViewById(R.id.tvDate);
        tvSessionName = view.findViewById(R.id.tvSessionName);
        tvStatPresent = view.findViewById(R.id.tvStatPresent);
        tvStatStudents = view.findViewById(R.id.tvStatStudents);
        tvStatTotal = view.findViewById(R.id.tvStatTotal);
        tvRatePercent = view.findViewById(R.id.tvRatePercent);
        tvRateDesc = view.findViewById(R.id.tvRateDesc);
        tvRecentEmpty = view.findViewById(R.id.tvRecentEmpty);
        progressRate = view.findViewById(R.id.progressRate);
        barChart = view.findViewById(R.id.barChart);

        // Danh sách gần đây
        RecyclerView rv = view.findViewById(R.id.rvRecent);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentAdapter = new AttendanceAdapter(new AttendanceAdapter.Listener() {
            @Override
            public void onItemClick(AttendanceRecord r) {
                Intent i = new Intent(requireContext(), RecordDetailActivity.class);
                i.putExtra(RecordDetailActivity.EXTRA_RECORD_ID, r.id);
                startActivity(i);
            }

            @Override
            public void onShare(AttendanceRecord r) {
                ExportUtils.shareRecord(requireContext(), r);
            }

            @Override
            public void onDelete(final AttendanceRecord r) {
                DialogUtils.confirm(requireContext(), R.string.confirm_delete_title,
                        getString(R.string.confirm_delete_record, r.studentName),
                        R.string.ctx_delete, () -> {
                            db.deleteAttendance(r.id);
                            loadData();
                        });
            }
        });
        rv.setAdapter(recentAdapter);

        // Sự kiện click
        View.OnClickListener startScan = v -> startActivity(new Intent(requireContext(), ScanActivity.class));
        view.findViewById(R.id.btnStartScan).setOnClickListener(startScan);
        view.findViewById(R.id.layoutSession).setOnClickListener(v ->
                DialogUtils.showInput(requireContext(), R.string.set_session, R.string.set_session_hint,
                        prefs.getSessionName(), text -> {
                            prefs.setSessionName(text);
                            tvSessionName.setText(text);
                        }));
        view.findViewById(R.id.actionAddStudent).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddStudentActivity.class)));
        view.findViewById(R.id.actionExport).setOnClickListener(v ->
                ExportUtils.shareCsv(requireContext(), db.getAttendance(0, null, prefs.getSortOrder())));
        View.OnClickListener goHistory = v -> ((MainActivity) requireActivity()).navigateTo(R.id.nav_history);
        view.findViewById(R.id.actionHistory).setOnClickListener(goHistory);
        view.findViewById(R.id.tvSeeAll).setOnClickListener(goHistory);

        // Options Menu của tab này
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_home, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_scan) {
                    startActivity(new Intent(requireContext(), ScanActivity.class));
                    return true;
                } else if (id == R.id.action_about) {
                    showAbout();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        animateEntrance((LinearLayout) view.findViewById(R.id.homeContent));
        // Nút quét nhịp nhẹ để thu hút
        view.findViewById(R.id.btnStartScan).startAnimation(
                android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.pulse));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        // Lời chào theo giờ
        int hour = DateUtils.hourOfDay(System.currentTimeMillis());
        int greetRes = hour < 11 ? R.string.greet_morning
                : hour < 14 ? R.string.greet_noon
                : hour < 18 ? R.string.greet_afternoon : R.string.greet_evening;
        String user = prefs.getUserName();
        tvGreeting.setText(TextUtils.isBlank(user)
                ? getString(greetRes) + " 👋"
                : getString(R.string.greet_with_name, getString(greetRes), user));
        tvDate.setText(DateUtils.formatFullDate(System.currentTimeMillis()));
        tvSessionName.setText(prefs.getSessionName());

        // Thống kê
        int present = db.countPresentToday();
        int students = db.countStudents();
        int total = db.countAttendance();
        countUp(tvStatPresent, present);
        countUp(tvStatStudents, students);
        countUp(tvStatTotal, total);

        int percent = students == 0 ? 0 : Math.min(100, Math.round(present * 100f / students));
        progressRate.setProgressCompat(percent, true);
        tvRatePercent.setText(percent + "%");
        tvRateDesc.setText(getString(R.string.home_rate_desc, present, students));

        // Biểu đồ 7 ngày
        int[] counts = db.countLastDays(7);
        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            labels[i] = DateUtils.shortDayName(DateUtils.startOfDayAgo(6 - i));
        }
        barChart.setData(counts, labels);

        // Gần đây
        List<AttendanceRecord> recent = db.getRecent(5);
        recentAdapter.submit(recent);
        tvRecentEmpty.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Hiệu ứng số đếm tăng dần. */
    private void countUp(final TextView tv, int target) {
        ValueAnimator a = ValueAnimator.ofInt(0, target);
        a.setDuration(firstLoad ? 800 : 350);
        a.addUpdateListener(an -> tv.setText(String.valueOf(an.getAnimatedValue())));
        a.start();
        firstLoad = false;
    }

    /** Các khối trượt lên lần lượt khi mở trang. */
    private void animateEntrance(LinearLayout content) {
        float shift = 36 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(shift);
            child.animate().alpha(1f).translationY(0f)
                    .setStartDelay(60L * i).setDuration(420).start();
        }
    }

    private void showAbout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }
}
