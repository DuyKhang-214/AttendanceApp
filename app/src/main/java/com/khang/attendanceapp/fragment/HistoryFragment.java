package com.khang.attendanceapp.fragment;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.khang.attendanceapp.R;
import com.khang.attendanceapp.activity.RecordDetailActivity;
import com.khang.attendanceapp.adapter.AttendanceAdapter;
import com.khang.attendanceapp.database.DatabaseHelper;
import com.khang.attendanceapp.model.AttendanceRecord;
import com.khang.attendanceapp.utils.AppPrefs;
import com.khang.attendanceapp.utils.DateUtils;
import com.khang.attendanceapp.utils.DialogUtils;
import com.khang.attendanceapp.utils.ExportUtils;

import java.util.List;

/**
 * Tab "Lịch sử": tìm kiếm không dấu, lọc theo khoảng thời gian (Chip), sắp xếp (SubMenu),
 * xuất CSV, ContextMenu khi nhấn giữ và vuốt để xoá (có Hoàn tác).
 */
public class HistoryFragment extends Fragment {

    private DatabaseHelper db;
    private AppPrefs prefs;
    private AttendanceAdapter adapter;
    private RecyclerView rv;
    private TextView tvCount;
    private View emptyState;
    private TextView tvEmptyTitle, tvEmptyDesc;

    private String query = "";
    private int range = 2; // 0 = hôm nay, 1 = 7 ngày, 2 = tất cả

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
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
        rv = view.findViewById(R.id.rvHistory);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AttendanceAdapter(new AttendanceAdapter.Listener() {
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
                            load();
                        });
            }
        });
        rv.setAdapter(adapter);
        attachSwipeToDelete();

        // Sự kiện: TextWatcher tìm kiếm
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

        // Sự kiện: chọn Chip lọc thời gian
        ChipGroup chips = view.findViewById(R.id.chipGroupRange);
        chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            range = id == R.id.chipToday ? 0 : id == R.id.chipWeek ? 1 : 2;
            load();
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_history, menu);
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                int sort = prefs.getSortOrder();
                int id = sort == AppPrefs.SORT_OLDEST ? R.id.sort_oldest
                        : sort == AppPrefs.SORT_NAME ? R.id.sort_name : R.id.sort_newest;
                MenuItem item = menu.findItem(id);
                if (item != null) item.setChecked(true);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.sort_newest || id == R.id.sort_oldest || id == R.id.sort_name) {
                    item.setChecked(true);
                    prefs.setSortOrder(id == R.id.sort_oldest ? AppPrefs.SORT_OLDEST
                            : id == R.id.sort_name ? AppPrefs.SORT_NAME : AppPrefs.SORT_NEWEST);
                    load();
                    return true;
                } else if (id == R.id.export_csv) {
                    ExportUtils.shareCsv(requireContext(), adapter.getItems());
                    return true;
                } else if (id == R.id.export_text) {
                    ExportUtils.shareText(requireContext(), adapter.getItems());
                    return true;
                } else if (id == R.id.action_delete_all) {
                    DialogUtils.confirm(requireContext(), R.string.confirm_delete_title,
                            getString(R.string.confirm_delete_all_attendance),
                            R.string.ctx_delete, () -> {
                                db.deleteAllAttendance();
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
        long from = range == 0 ? DateUtils.startOfDay(System.currentTimeMillis())
                : range == 1 ? DateUtils.startOfDayAgo(6) : 0;
        List<AttendanceRecord> list = db.getAttendance(from, query, prefs.getSortOrder());
        adapter.submit(list);
        rv.scheduleLayoutAnimation();

        tvCount.setText(getString(R.string.history_count_fmt, list.size()));
        boolean empty = list.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            boolean filtering = !query.trim().isEmpty() || range != 2;
            tvEmptyTitle.setText(filtering ? R.string.empty_search_title : R.string.empty_history_title);
            tvEmptyDesc.setText(filtering ? R.string.empty_search_desc : R.string.empty_history_desc);
        }
    }

    /** Vuốt ngang để xoá, có Snackbar "Hoàn tác". */
    private void attachSwipeToDelete() {
        final Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(ContextCompat.getColor(requireContext(), R.color.error));

        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                final AttendanceRecord r = adapter.getItem(pos);
                db.deleteAttendance(r.id);
                load();
                Snackbar.make(rv, getString(R.string.msg_deleted_record, r.studentName), Snackbar.LENGTH_LONG)
                        .setAction(R.string.btn_undo, v -> {
                            db.insertAttendance(r);
                            load();
                        }).show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder vh, float dX, float dY,
                                    int actionState, boolean isActive) {
                View item = vh.itemView;
                float d = getResources().getDisplayMetrics().density;
                float radius = 20 * d;
                if (dX > 0) {
                    c.drawRoundRect(item.getLeft(), item.getTop() + 5 * d, item.getLeft() + dX + radius,
                            item.getBottom() - 5 * d, radius, radius, paint);
                } else if (dX < 0) {
                    c.drawRoundRect(item.getRight() + dX - radius, item.getTop() + 5 * d, item.getRight(),
                            item.getBottom() - 5 * d, radius, radius, paint);
                }
                if (icon != null && dX != 0) {
                    int size = (int) (24 * d);
                    int top = item.getTop() + (item.getHeight() - size) / 2;
                    int left = dX > 0 ? item.getLeft() + (int) (20 * d)
                            : item.getRight() - (int) (20 * d) - size;
                    icon.setBounds(left, top, left + size, top + size);
                    icon.setTint(0xFFFFFFFF);
                    icon.draw(c);
                }
                super.onChildDraw(c, recyclerView, vh, dX, dY, actionState, isActive);
            }
        };
        new ItemTouchHelper(cb).attachToRecyclerView(rv);
    }
}
