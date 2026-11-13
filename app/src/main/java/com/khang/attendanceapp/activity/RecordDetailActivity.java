package com.khang.attendanceapp.activity;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.khang.attendanceapp.R;
import com.khang.attendanceapp.database.DatabaseHelper;
import com.khang.attendanceapp.model.AttendanceRecord;
import com.khang.attendanceapp.utils.AvatarUtils;
import com.khang.attendanceapp.utils.DateUtils;
import com.khang.attendanceapp.utils.DialogUtils;
import com.khang.attendanceapp.utils.ExportUtils;
import com.khang.attendanceapp.utils.TextUtils;

/** Chi tiết 1 lượt điểm danh: sửa trạng thái, ghi chú, chia sẻ hoặc xoá. */
public class RecordDetailActivity extends BaseActivity {

    public static final String EXTRA_RECORD_ID = "extra_record_id";

    private DatabaseHelper db;
    private AttendanceRecord record;
    private ChipGroup chipGroup;
    private TextInputEditText etNote;
    private TextView tvStatusBadge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_detail);
        db = DatabaseHelper.getInstance(this);

        applyTopInset(findViewById(R.id.headerLayout));
        applyBottomInset(findViewById(R.id.scrollDetail));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.title_detail);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        long id = getIntent().getLongExtra(EXTRA_RECORD_ID, -1);
        record = db.getAttendance(id);
        if (record == null) {
            finish();
            return;
        }

        chipGroup = findViewById(R.id.chipGroupStatus);
        etNote = findViewById(R.id.etNote);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);

        AvatarUtils.bind((TextView) findViewById(R.id.tvAvatar), record.studentName, record.studentCode);
        ((TextView) findViewById(R.id.tvName)).setText(record.studentName);
        ((TextView) findViewById(R.id.tvCode)).setText(getString(R.string.student_code_fmt, record.studentCode));
        ((TextView) findViewById(R.id.tvClass)).setText(
                TextUtils.isBlank(record.className) ? getString(R.string.student_no_class) : record.className);
        ((TextView) findViewById(R.id.tvSession)).setText(TextUtils.safe(record.sessionName));
        ((TextView) findViewById(R.id.tvTime)).setText(DateUtils.formatDateTime(record.timestamp));
        etNote.setText(record.note);

        if (AttendanceRecord.STATUS_LATE.equals(record.status)) {
            chipGroup.check(R.id.chipLate);
        } else if (AttendanceRecord.STATUS_EXCUSED.equals(record.status)) {
            chipGroup.check(R.id.chipExcused);
        } else {
            chipGroup.check(R.id.chipPresent);
        }
        showBadge(record.status);

        // Sự kiện: đổi trạng thái -> cập nhật badge ngay
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> showBadge(currentStatus()));

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String note = etNote.getText() == null ? "" : etNote.getText().toString().trim();
            db.updateAttendance(record.id, currentStatus(), note);
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });

        // Intent ngầm định: chia sẻ
        findViewById(R.id.btnShare).setOnClickListener(v -> ExportUtils.shareRecord(this, record));

        findViewById(R.id.btnDelete).setOnClickListener(v -> DialogUtils.confirm(
                this, R.string.confirm_delete_title,
                getString(R.string.confirm_delete_record, record.studentName),
                R.string.ctx_delete, () -> {
                    db.deleteAttendance(record.id);
                    Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }));
    }

    private String currentStatus() {
        int id = chipGroup.getCheckedChipId();
        if (id == R.id.chipLate) return AttendanceRecord.STATUS_LATE;
        if (id == R.id.chipExcused) return AttendanceRecord.STATUS_EXCUSED;
        return AttendanceRecord.STATUS_PRESENT;
    }

    private void showBadge(String status) {
        tvStatusBadge.setText(AttendanceRecord.statusLabel(status));
        tvStatusBadge.setTextColor(ContextCompat.getColor(this, AttendanceRecord.statusColor(status)));
        tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, AttendanceRecord.statusBgColor(status))));
    }
}
