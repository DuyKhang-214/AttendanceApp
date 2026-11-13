package com.khang.attendanceapp.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.khang.attendanceapp.R;
import com.khang.attendanceapp.model.AttendanceRecord;
import com.khang.attendanceapp.utils.AvatarUtils;
import com.khang.attendanceapp.utils.DateUtils;
import com.khang.attendanceapp.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;

/** Danh sách lượt điểm danh. Nhấn giữ để mở ContextMenu (xem / chia sẻ / xoá). */
public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {

    public interface Listener {
        void onItemClick(AttendanceRecord record);

        void onShare(AttendanceRecord record);

        void onDelete(AttendanceRecord record);
    }

    private final List<AttendanceRecord> items = new ArrayList<>();
    private final Listener listener;

    public AttendanceAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<AttendanceRecord> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    public AttendanceRecord getItem(int position) {
        return items.get(position);
    }

    public List<AttendanceRecord> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final AttendanceRecord r = items.get(position);
        Context ctx = h.itemView.getContext();

        AvatarUtils.bind(h.tvAvatar, r.studentName, r.studentCode);
        h.tvName.setText(r.studentName);
        String cls = TextUtils.isBlank(r.className) ? "" : " • " + r.className;
        h.tvSub.setText(r.studentCode + cls);
        h.tvTime.setText(DateUtils.formatTime(r.timestamp));
        h.tvDate.setText(DateUtils.formatDate(r.timestamp));

        h.tvStatus.setText(AttendanceRecord.statusLabel(r.status));
        h.tvStatus.setTextColor(ContextCompat.getColor(ctx, AttendanceRecord.statusColor(r.status)));
        h.tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(ctx, AttendanceRecord.statusBgColor(r.status))));

        h.itemView.setOnClickListener(v -> listener.onItemClick(r));

        // ContextMenu (nhấn giữ)
        h.itemView.setOnCreateContextMenuListener((menu, view, info) -> {
            new MenuInflater(ctx).inflate(R.menu.context_attendance, menu);
            menu.setHeaderTitle(r.studentName);
            menu.findItem(R.id.ctx_detail).setOnMenuItemClickListener(item -> {
                listener.onItemClick(r);
                return true;
            });
            menu.findItem(R.id.ctx_share).setOnMenuItemClickListener(item -> {
                listener.onShare(r);
                return true;
            });
            menu.findItem(R.id.ctx_delete).setOnMenuItemClickListener(item -> {
                listener.onDelete(r);
                return true;
            });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvAvatar, tvName, tvSub, tvTime, tvDate, tvStatus;

        VH(@NonNull View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvAvatar);
            tvName = v.findViewById(R.id.tvName);
            tvSub = v.findViewById(R.id.tvSub);
            tvTime = v.findViewById(R.id.tvTime);
            tvDate = v.findViewById(R.id.tvDate);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}
