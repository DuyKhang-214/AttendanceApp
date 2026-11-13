package com.khang.attendanceapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khang.attendanceapp.R;
import com.khang.attendanceapp.model.Student;
import com.khang.attendanceapp.utils.AvatarUtils;
import com.khang.attendanceapp.utils.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Danh sách sinh viên. Nhấn giữ để mở ContextMenu (sửa / gọi / email / xoá). */
public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.VH> {

    public interface Listener {
        void onItemClick(Student student);

        void onCall(Student student);

        void onEmail(Student student);

        void onDelete(Student student);
    }

    private final List<Student> items = new ArrayList<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private final Listener listener;

    public StudentAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Student> data, Map<String, Integer> attendanceCounts) {
        items.clear();
        items.addAll(data);
        counts.clear();
        counts.putAll(attendanceCounts);
        notifyDataSetChanged();
    }

    public Student getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Student s = items.get(position);
        final Context ctx = h.itemView.getContext();

        AvatarUtils.bind(h.tvAvatar, s.name, s.code);
        h.tvName.setText(s.name);
        h.tvCode.setText(ctx.getString(R.string.student_code_fmt, s.code));
        h.tvClass.setText(TextUtils.isBlank(s.className)
                ? ctx.getString(R.string.student_no_class)
                : ctx.getString(R.string.student_class_fmt, s.className));
        Integer n = counts.get(s.code);
        h.tvCount.setText(ctx.getString(R.string.student_count_fmt, n == null ? 0 : n));

        h.itemView.setOnClickListener(v -> listener.onItemClick(s));

        h.itemView.setOnCreateContextMenuListener((menu, view, info) -> {
            new MenuInflater(ctx).inflate(R.menu.context_student, menu);
            menu.setHeaderTitle(s.name);
            menu.findItem(R.id.ctx_edit).setOnMenuItemClickListener(item -> {
                listener.onItemClick(s);
                return true;
            });
            menu.findItem(R.id.ctx_call).setOnMenuItemClickListener(item -> {
                listener.onCall(s);
                return true;
            });
            menu.findItem(R.id.ctx_email).setOnMenuItemClickListener(item -> {
                listener.onEmail(s);
                return true;
            });
            menu.findItem(R.id.ctx_delete_student).setOnMenuItemClickListener(item -> {
                listener.onDelete(s);
                return true;
            });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvAvatar, tvName, tvCode, tvClass, tvCount;

        VH(@NonNull View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvAvatar);
            tvName = v.findViewById(R.id.tvName);
            tvCode = v.findViewById(R.id.tvCode);
            tvClass = v.findViewById(R.id.tvClass);
            tvCount = v.findViewById(R.id.tvCount);
        }
    }
}
