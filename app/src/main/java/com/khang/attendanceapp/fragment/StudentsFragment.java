package com.khang.attendanceapp.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.khang.attendanceapp.R;
import com.khang.attendanceapp.activity.AddStudentActivity;
import com.khang.attendanceapp.adapter.StudentAdapter;
import com.khang.attendanceapp.database.DatabaseHelper;
import com.khang.attendanceapp.model.Student;
import com.khang.attendanceapp.utils.AppPrefs;
import com.khang.attendanceapp.utils.DialogUtils;
import com.khang.attendanceapp.utils.TextUtils;

import java.util.List;

/** Tab "Sinh viên": danh sách lớp, tìm kiếm, sửa/xoá, gọi điện / gửi email bằng Intent ngầm định. */
public class StudentsFragment extends Fragment {

    private DatabaseHelper db;
    private AppPrefs prefs;
    private StudentAdapter adapter;
    private RecyclerView rv;
    private TextView tvCount, tvEmptyTitle, tvEmptyDesc;
    private View emptyState;
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_students, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());
        prefs = new AppPrefs(requireContext());

        tvCount = view.findViewById(R.id.tvCount);
        emptyState = view.findViewById(R.id.emptyState);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = view.findViewById(R.id.tvEmptyDesc);
        rv = view.findViewById(R.id.rvStudents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new StudentAdapter(new StudentAdapter.Listener() {
            @Override
            public void onItemClick(Student s) {
                Intent i = new Intent(requireContext(), AddStudentActivity.class);
                i.putExtra(AddStudentActivity.EXTRA_STUDENT_ID, s.id);
                startActivity(i);
            }

            @Override
            public void onCall(Student s) {
                if (TextUtils.isBlank(s.phone)) {
                    Toast.makeText(requireContext(), R.string.msg_no_phone, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Intent ngầm định: mở trình quay số
                safeStart(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + s.phone.trim())));
            }

            @Override
            public void onEmail(Student s) {
                if (TextUtils.isBlank(s.email)) {
                    Toast.makeText(requireContext(), R.string.msg_no_email, Toast.LENGTH_SHORT).show();
                    return;
                }
                // Intent ngầm định: mở ứng dụng email
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + s.email.trim()));
                i.putExtra(Intent.EXTRA_SUBJECT, prefs.getSessionName());
                safeStart(i);
            }

            @Override
            public void onDelete(final Student s) {
                DialogUtils.confirm(requireContext(), R.string.confirm_delete_title,
                        getString(R.string.confirm_delete_student, s.name),
                        R.string.ctx_delete, () -> {
                            db.deleteStudent(s.id);
                            load();
                        });
            }
        });
        rv.setAdapter(adapter);

        TextInputEditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString();
                load();
            }
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_students, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_sample_data) {
                    db.insertSampleData(prefs.getSessionName());
                    Toast.makeText(requireContext(), R.string.msg_sample_added, Toast.LENGTH_SHORT).show();
                    load();
                    return true;
                } else if (id == R.id.action_delete_students) {
                    DialogUtils.confirm(requireContext(), R.string.confirm_delete_title,
                            getString(R.string.confirm_delete_all_students),
                            R.string.ctx_delete, () -> {
                                db.deleteAllStudents();
                                load();
                            });
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        List<Student> list = db.getStudents(query);
        adapter.submit(list, db.getAttendanceCounts());
        rv.scheduleLayoutAnimation();
        tvCount.setText(getString(R.string.students_count_fmt, list.size()));
        boolean empty = list.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            boolean filtering = !query.trim().isEmpty();
            tvEmptyTitle.setText(filtering ? R.string.empty_search_title : R.string.empty_students_title);
            tvEmptyDesc.setText(filtering ? R.string.empty_search_desc : R.string.empty_students_desc);
        }
    }

    private void safeStart(Intent i) {
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.msg_no_app, Toast.LENGTH_SHORT).show();
        }
    }
}
