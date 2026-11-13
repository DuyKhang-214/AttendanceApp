package com.khang.attendanceapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.khang.attendanceapp.R;
import com.khang.attendanceapp.database.DatabaseHelper;
import com.khang.attendanceapp.utils.AppPrefs;
import com.khang.attendanceapp.utils.DialogUtils;
import com.khang.attendanceapp.utils.ExportUtils;
import com.khang.attendanceapp.utils.TextUtils;

import java.util.Locale;

/** Tab "Cài đặt": thông tin buổi học, tính đi trễ, âm thanh/rung, chế độ tối, dữ liệu. */
public class SettingsFragment extends Fragment {

    private AppPrefs prefs;
    private DatabaseHelper db;
    private TextView tvUserNameValue, tvSessionValue, tvLateDesc, tvStartTimeValue;
    private View rowStartTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new AppPrefs(requireContext());
        db = DatabaseHelper.getInstance(requireContext());

        tvUserNameValue = view.findViewById(R.id.tvUserNameValue);
        tvSessionValue = view.findViewById(R.id.tvSessionValue);
        tvLateDesc = view.findViewById(R.id.tvLateDesc);
        tvStartTimeValue = view.findViewById(R.id.tvStartTimeValue);
        rowStartTime = view.findViewById(R.id.rowStartTime);

        ((TextView) view.findViewById(R.id.tvExportDesc)).setText(R.string.set_export_desc);
        ((TextView) view.findViewById(R.id.tvSampleDesc)).setText(R.string.set_sample_desc);
        ((TextView) view.findViewById(R.id.tvClearDesc)).setText(R.string.set_clear_desc);
        ((TextView) view.findViewById(R.id.tvVersion)).setText(
                getString(R.string.set_version_fmt, versionName()));

        final MaterialSwitch swLate = view.findViewById(R.id.switchLate);
        final MaterialSwitch swSound = view.findViewById(R.id.switchSound);
        final MaterialSwitch swVibrate = view.findViewById(R.id.switchVibrate);
        final MaterialSwitch swDark = view.findViewById(R.id.switchDark);

        swLate.setChecked(prefs.isLateEnabled());
        swSound.setChecked(prefs.isSoundOn());
        swVibrate.setChecked(prefs.isVibrateOn());
        swDark.setChecked(prefs.isDarkMode());
        refreshTexts();

        view.findViewById(R.id.rowUserName).setOnClickListener(v ->
                DialogUtils.showInput(requireContext(), R.string.set_user_name, R.string.set_user_name_hint,
                        prefs.getUserName(), text -> {
                            prefs.setUserName(text);
                            refreshTexts();
                        }));
        view.findViewById(R.id.rowSession).setOnClickListener(v ->
                DialogUtils.showInput(requireContext(), R.string.set_session, R.string.set_session_hint,
                        prefs.getSessionName(), text -> {
                            prefs.setSessionName(text);
                            refreshTexts();
                        }));

        // Sự kiện: công tắc (CheckedChange)
        swLate.setOnCheckedChangeListener((b, checked) -> {
            prefs.setLateEnabled(checked);
            refreshTexts();
        });
        swSound.setOnCheckedChangeListener((b, checked) -> prefs.setSoundOn(checked));
        swVibrate.setOnCheckedChangeListener((b, checked) -> prefs.setVibrateOn(checked));
        swDark.setOnCheckedChangeListener((b, checked) -> {
            prefs.setDarkMode(checked);
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
        view.findViewById(R.id.rowLate).setOnClickListener(v -> swLate.toggle());
        view.findViewById(R.id.rowSound).setOnClickListener(v -> swSound.toggle());
        view.findViewById(R.id.rowVibrate).setOnClickListener(v -> swVibrate.toggle());
        view.findViewById(R.id.rowDark).setOnClickListener(v -> swDark.toggle());

        rowStartTime.setOnClickListener(v -> {
            if (!prefs.isLateEnabled()) {
                Toast.makeText(requireContext(), R.string.set_late_off_hint, Toast.LENGTH_SHORT).show();
                return;
            }
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(prefs.getStartHour())
                    .setMinute(prefs.getStartMinute())
                    .setTitleText(R.string.set_start_time)
                    .build();
            picker.addOnPositiveButtonClickListener(x -> {
                prefs.setStartTime(picker.getHour(), picker.getMinute());
                refreshTexts();
            });
            picker.show(getChildFragmentManager(), "start_time");
        });

        view.findViewById(R.id.rowExport).setOnClickListener(v ->
                ExportUtils.shareCsv(requireContext(), db.getAttendance(0, null, prefs.getSortOrder())));
        view.findViewById(R.id.rowSample).setOnClickListener(v -> {
            db.insertSampleData(prefs.getSessionName());
            Toast.makeText(requireContext(), R.string.msg_sample_added, Toast.LENGTH_SHORT).show();
        });
        view.findViewById(R.id.rowClear).setOnClickListener(v ->
                DialogUtils.confirm(requireContext(), R.string.confirm_delete_title,
                        getString(R.string.confirm_delete_everything), R.string.ctx_delete, () -> {
                            db.deleteAllAttendance();
                            db.deleteAllStudents();
                            Toast.makeText(requireContext(), R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                        }));
        view.findViewById(R.id.rowAbout).setOnClickListener(v ->
                Toast.makeText(requireContext(), R.string.about_message, Toast.LENGTH_LONG).show());

        animateEntrance((LinearLayout) view.findViewById(R.id.settingsContent));
    }

    private void refreshTexts() {
        String user = prefs.getUserName();
        tvUserNameValue.setText(TextUtils.isBlank(user) ? getString(R.string.set_not_set) : user);
        tvSessionValue.setText(prefs.getSessionName());
        tvStartTimeValue.setText(String.format(Locale.ROOT, "%02d:%02d",
                prefs.getStartHour(), prefs.getStartMinute()));
        tvLateDesc.setText(prefs.isLateEnabled() ? R.string.set_late_on_desc : R.string.set_late_off_desc);
        rowStartTime.setAlpha(prefs.isLateEnabled() ? 1f : 0.45f);
    }

    private void animateEntrance(LinearLayout content) {
        float shift = 28 * getResources().getDisplayMetrics().density;
        for (int i = 0; i < content.getChildCount(); i++) {
            View c = content.getChildAt(i);
            c.setAlpha(0f);
            c.setTranslationY(shift);
            c.animate().alpha(1f).translationY(0f).setStartDelay(40L * i).setDuration(380).start();
        }
    }

    private String versionName() {
        try {
            return requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }
}
