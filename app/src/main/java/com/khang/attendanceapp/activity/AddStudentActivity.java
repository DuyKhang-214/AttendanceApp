package com.khang.attendanceapp.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.khang.attendanceapp.R;
import com.khang.attendanceapp.database.DatabaseHelper;
import com.khang.attendanceapp.model.Student;
import com.khang.attendanceapp.utils.AvatarUtils;

import android.widget.TextView;

/**
 * Thêm / sửa sinh viên. Nhận extra:
 * - EXTRA_STUDENT_ID: id sinh viên cần sửa (không có = thêm mới)
 * - EXTRA_PREFILL_CODE: MSSV điền sẵn (khi quét ra mã chưa có trong danh sách)
 */
public class AddStudentActivity extends BaseActivity {

    public static final String EXTRA_STUDENT_ID = "extra_student_id";
    public static final String EXTRA_PREFILL_CODE = "extra_prefill_code";

    private TextInputLayout tilCode, tilName, tilClass, tilEmail;
    private TextInputEditText etCode, etName, etClass, etPhone, etEmail;
    private TextView tvPreviewAvatar;
    private DatabaseHelper db;
    private long editingId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_student);
        db = DatabaseHelper.getInstance(this);

        applyTopInset(findViewById(R.id.headerLayout));
        applyBottomInset(findViewById(R.id.scrollForm));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilCode = findViewById(R.id.tilCode);
        tilName = findViewById(R.id.tilName);
        tilClass = findViewById(R.id.tilClass);
        tilEmail = findViewById(R.id.tilEmail);
        etCode = findViewById(R.id.etCode);
        etName = findViewById(R.id.etName);
        etClass = findViewById(R.id.etClass);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        tvPreviewAvatar = findViewById(R.id.tvPreviewAvatar);
        MaterialButton btnSave = findViewById(R.id.btnSave);

        editingId = getIntent().getLongExtra(EXTRA_STUDENT_ID, -1);
        if (editingId > 0) {
            toolbar.setTitle(R.string.title_edit_student);
            Student s = db.getStudentById(editingId);
            if (s == null) {
                finish();
                return;
            }
            etCode.setText(s.code);
            etName.setText(s.name);
            etClass.setText(s.className);
            etPhone.setText(s.phone);
            etEmail.setText(s.email);
        } else {
            toolbar.setTitle(R.string.title_add_student);
            String prefill = getIntent().getStringExtra(EXTRA_PREFILL_CODE);
            if (prefill != null) etCode.setText(prefill);
        }
        updatePreview();

        // Sự kiện: TextWatcher cập nhật avatar xem trước + xoá lỗi khi gõ
        etName.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                tilName.setError(null);
                updatePreview();
            }
        });
        etCode.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                tilCode.setError(null);
                updatePreview();
            }
        });
        etEmail.addTextChangedListener(new SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                tilEmail.setError(null);
            }
        });

        btnSave.setOnClickListener(v -> save());
    }

    private void updatePreview() {
        String name = text(etName);
        AvatarUtils.bind(tvPreviewAvatar, name.isEmpty() ? "?" : name, text(etCode));
    }

    private void save() {
        String code = text(etCode).toUpperCase();
        String name = text(etName);
        String cls = text(etClass).toUpperCase();
        String phone = text(etPhone);
        String email = text(etEmail);

        boolean ok = true;
        if (code.isEmpty()) {
            tilCode.setError(getString(R.string.error_required));
            ok = false;
        } else if (db.codeExists(code, editingId)) {
            tilCode.setError(getString(R.string.error_code_exists));
            ok = false;
        }
        if (name.isEmpty()) {
            tilName.setError(getString(R.string.error_required));
            ok = false;
        }
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email));
            ok = false;
        }
        if (!ok) return;

        Student s = editingId > 0 ? db.getStudentById(editingId) : new Student();
        if (s == null) s = new Student();
        s.code = code;
        s.name = name;
        s.className = cls;
        s.phone = phone;
        s.email = email;

        boolean success;
        if (editingId > 0) {
            success = db.updateStudent(s);
        } else {
            s.createdAt = System.currentTimeMillis();
            success = db.insertStudent(s) > 0;
        }
        if (success) {
            Toast.makeText(this, R.string.msg_student_saved, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            tilCode.setError(getString(R.string.error_code_exists));
        }
    }

    private String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
