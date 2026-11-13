package com.khang.attendanceapp.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.khang.attendanceapp.R;
import com.khang.attendanceapp.model.AttendanceRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Xuất danh sách điểm danh ra CSV / văn bản và chia sẻ qua Intent ngầm định (ACTION_SEND).
 */
public final class ExportUtils {

    private ExportUtils() {
    }

    /** Tạo file CSV (có BOM UTF-8 để Excel đọc đúng tiếng Việt) rồi mở bảng chia sẻ. */
    public static void shareCsv(Context ctx, List<AttendanceRecord> records) {
        if (records == null || records.isEmpty()) {
            Toast.makeText(ctx, R.string.export_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(ctx.getCacheDir(), "exports");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdirs failed");
            File file = new File(dir, "diemdanh_" + DateUtils.formatFileStamp(System.currentTimeMillis()) + ".csv");

            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                w.write('\uFEFF');
                w.write("STT,MSSV,Họ và tên,Lớp,Buổi học,Ngày,Giờ,Trạng thái,Ghi chú\n");
                int i = 1;
                for (AttendanceRecord r : records) {
                    w.write(i++ + ","
                            + csv(r.studentCode) + ","
                            + csv(r.studentName) + ","
                            + csv(r.className) + ","
                            + csv(r.sessionName) + ","
                            + DateUtils.formatDate(r.timestamp) + ","
                            + DateUtils.formatTime(r.timestamp) + ","
                            + csv(ctx.getString(AttendanceRecord.statusLabel(r.status))) + ","
                            + csv(r.note) + "\n");
                }
            }

            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/csv");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.export_subject));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.export_chooser)));
        } catch (Exception e) {
            Toast.makeText(ctx, R.string.export_failed, Toast.LENGTH_LONG).show();
        }
    }

    /** Chia sẻ danh sách dạng văn bản thuần (dán vào Zalo, Messenger, email...). */
    public static void shareText(Context ctx, List<AttendanceRecord> records) {
        if (records == null || records.isEmpty()) {
            Toast.makeText(ctx, R.string.export_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.getString(R.string.export_subject)).append("\n");
        sb.append(ctx.getString(R.string.export_text_total, records.size())).append("\n\n");
        int i = 1;
        for (AttendanceRecord r : records) {
            sb.append(i++).append(". ")
                    .append(r.studentName).append(" (").append(r.studentCode).append(")")
                    .append(" - ").append(DateUtils.formatTime(r.timestamp))
                    .append(" ").append(DateUtils.formatDate(r.timestamp))
                    .append(" - ").append(ctx.getString(AttendanceRecord.statusLabel(r.status)))
                    .append("\n");
        }
        shareRawText(ctx, ctx.getString(R.string.export_subject), sb.toString());
    }

    public static void shareRecord(Context ctx, AttendanceRecord r) {
        String text = ctx.getString(R.string.share_record_text,
                r.studentName, r.studentCode,
                ctx.getString(AttendanceRecord.statusLabel(r.status)),
                DateUtils.formatDateTime(r.timestamp),
                TextUtils.safe(r.sessionName));
        shareRawText(ctx, ctx.getString(R.string.export_subject), text);
    }

    private static void shareRawText(Context ctx, String subject, String text) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, subject);
        send.putExtra(Intent.EXTRA_TEXT, text);
        ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.export_chooser)));
    }

    private static String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\"")) return "\"" + v + "\"";
        return v;
    }
}
