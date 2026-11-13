package com.khang.attendanceapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.khang.attendanceapp.model.AttendanceRecord;
import com.khang.attendanceapp.model.Student;
import com.khang.attendanceapp.utils.DateUtils;
import com.khang.attendanceapp.utils.TextUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * SQLite lưu danh sách sinh viên và lịch sử điểm danh.
 * (Dữ liệu nhỏ nên truy vấn trực tiếp trên main thread cho đơn giản.)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "attendance.db";
    private static final int DB_VERSION = 1;

    private static final String T_STUDENT = "students";
    private static final String T_ATT = "attendance";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_STUDENT + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "code TEXT NOT NULL UNIQUE,"
                + "name TEXT NOT NULL,"
                + "class_name TEXT,"
                + "phone TEXT,"
                + "email TEXT,"
                + "created_at INTEGER)");
        db.execSQL("CREATE TABLE " + T_ATT + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "student_code TEXT NOT NULL,"
                + "student_name TEXT,"
                + "class_name TEXT,"
                + "session_name TEXT,"
                + "timestamp INTEGER NOT NULL,"
                + "status TEXT,"
                + "note TEXT)");
        db.execSQL("CREATE INDEX idx_att_time ON " + T_ATT + "(timestamp)");
        db.execSQL("CREATE INDEX idx_att_code ON " + T_ATT + "(student_code)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_ATT);
        db.execSQL("DROP TABLE IF EXISTS " + T_STUDENT);
        onCreate(db);
    }

    // ============================== SINH VIÊN ==============================

    /** @return id mới, hoặc -1 nếu MSSV đã tồn tại. */
    public long insertStudent(Student s) {
        ContentValues v = studentValues(s);
        return getWritableDatabase().insertWithOnConflict(T_STUDENT, null, v,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean updateStudent(Student s) {
        ContentValues v = studentValues(s);
        try {
            return getWritableDatabase().update(T_STUDENT, v, "id=?",
                    new String[]{String.valueOf(s.id)}) > 0;
        } catch (Exception e) { // trùng MSSV
            return false;
        }
    }

    public void deleteStudent(long id) {
        getWritableDatabase().delete(T_STUDENT, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteAllStudents() {
        getWritableDatabase().delete(T_STUDENT, null, null);
    }

    public Student getStudentById(long id) {
        try (Cursor c = getReadableDatabase().query(T_STUDENT, null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? studentFrom(c) : null;
        }
    }

    public Student getStudentByCode(String code) {
        try (Cursor c = getReadableDatabase().query(T_STUDENT, null, "code=?",
                new String[]{code}, null, null, null)) {
            return c.moveToFirst() ? studentFrom(c) : null;
        }
    }

    /** Kiểm tra MSSV đã tồn tại ở sinh viên khác (excludeId = id đang sửa, hoặc -1). */
    public boolean codeExists(String code, long excludeId) {
        try (Cursor c = getReadableDatabase().query(T_STUDENT, new String[]{"id"},
                "code=? AND id<>?", new String[]{code, String.valueOf(excludeId)},
                null, null, null)) {
            return c.moveToFirst();
        }
    }

    /** Lấy tất cả sinh viên (sắp xếp A-Z theo tên), lọc theo từ khoá không phân biệt dấu. */
    public List<Student> getStudents(String query) {
        List<Student> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(T_STUDENT, null, null, null,
                null, null, null)) {
            while (c.moveToNext()) list.add(studentFrom(c));
        }
        final String q = TextUtils.normalize(query);
        if (!q.isEmpty()) {
            List<Student> filtered = new ArrayList<>();
            for (Student s : list) {
                String hay = TextUtils.normalize(s.code + " " + s.name + " " + s.className);
                if (hay.contains(q)) filtered.add(s);
            }
            list = filtered;
        }
        final Collator collator = Collator.getInstance(new Locale("vi", "VN"));
        Collections.sort(list, new Comparator<Student>() {
            @Override
            public int compare(Student a, Student b) {
                return collator.compare(lastWord(a.name), lastWord(b.name));
            }
        });
        return list;
    }

    public int countStudents() {
        return scalar("SELECT COUNT(*) FROM " + T_STUDENT, null);
    }

    private static String lastWord(String name) {
        if (name == null) return "";
        String[] p = name.trim().split("\\s+");
        return p[p.length - 1] + " " + name;
    }

    private ContentValues studentValues(Student s) {
        ContentValues v = new ContentValues();
        v.put("code", s.code);
        v.put("name", s.name);
        v.put("class_name", s.className);
        v.put("phone", s.phone);
        v.put("email", s.email);
        v.put("created_at", s.createdAt == 0 ? System.currentTimeMillis() : s.createdAt);
        return v;
    }

    private Student studentFrom(Cursor c) {
        Student s = new Student();
        s.id = c.getLong(c.getColumnIndexOrThrow("id"));
        s.code = c.getString(c.getColumnIndexOrThrow("code"));
        s.name = c.getString(c.getColumnIndexOrThrow("name"));
        s.className = c.getString(c.getColumnIndexOrThrow("class_name"));
        s.phone = c.getString(c.getColumnIndexOrThrow("phone"));
        s.email = c.getString(c.getColumnIndexOrThrow("email"));
        s.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        return s;
    }

    // ============================== ĐIỂM DANH ==============================

    public long insertAttendance(AttendanceRecord r) {
        ContentValues v = new ContentValues();
        v.put("student_code", r.studentCode);
        v.put("student_name", r.studentName);
        v.put("class_name", r.className);
        v.put("session_name", r.sessionName);
        v.put("timestamp", r.timestamp);
        v.put("status", r.status);
        v.put("note", r.note);
        long id = getWritableDatabase().insert(T_ATT, null, v);
        r.id = id;
        return id;
    }

    public void updateAttendance(long id, String status, String note) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        v.put("note", note);
        getWritableDatabase().update(T_ATT, v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteAttendance(long id) {
        getWritableDatabase().delete(T_ATT, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteAllAttendance() {
        getWritableDatabase().delete(T_ATT, null, null);
    }

    public AttendanceRecord getAttendance(long id) {
        try (Cursor c = getReadableDatabase().query(T_ATT, null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? recordFrom(c) : null;
        }
    }

    /**
     * Danh sách điểm danh.
     *
     * @param fromTs mốc thời gian bắt đầu (0 = tất cả)
     * @param query  từ khoá (tên / MSSV / lớp), không phân biệt dấu
     * @param sort   AppPrefs.SORT_*
     */
    public List<AttendanceRecord> getAttendance(long fromTs, String query, int sort) {
        List<AttendanceRecord> list = new ArrayList<>();
        String sel = null;
        String[] args = null;
        if (fromTs > 0) {
            sel = "timestamp>=?";
            args = new String[]{String.valueOf(fromTs)};
        }
        String order = sort == 1 ? "timestamp ASC" : "timestamp DESC";
        try (Cursor c = getReadableDatabase().query(T_ATT, null, sel, args, null, null, order)) {
            while (c.moveToNext()) list.add(recordFrom(c));
        }
        final String q = TextUtils.normalize(query);
        if (!q.isEmpty()) {
            List<AttendanceRecord> filtered = new ArrayList<>();
            for (AttendanceRecord r : list) {
                String hay = TextUtils.normalize(r.studentCode + " " + r.studentName + " " + r.className);
                if (hay.contains(q)) filtered.add(r);
            }
            list = filtered;
        }
        if (sort == 2) {
            final Collator collator = Collator.getInstance(new Locale("vi", "VN"));
            Collections.sort(list, new Comparator<AttendanceRecord>() {
                @Override
                public int compare(AttendanceRecord a, AttendanceRecord b) {
                    int r = collator.compare(lastWord(a.studentName), lastWord(b.studentName));
                    return r != 0 ? r : Long.compare(b.timestamp, a.timestamp);
                }
            });
        }
        return list;
    }

    public List<AttendanceRecord> getRecent(int limit) {
        List<AttendanceRecord> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(T_ATT, null, null, null, null, null,
                "timestamp DESC", String.valueOf(limit))) {
            while (c.moveToNext()) list.add(recordFrom(c));
        }
        return list;
    }

    /** Tìm lượt điểm danh của sinh viên trong cùng buổi học + cùng ngày (để chống quét trùng). */
    public AttendanceRecord findToday(String code, String session) {
        long start = DateUtils.startOfDay(System.currentTimeMillis());
        try (Cursor c = getReadableDatabase().query(T_ATT, null,
                "student_code=? AND session_name=? AND timestamp>=?",
                new String[]{code, session, String.valueOf(start)},
                null, null, "timestamp DESC", "1")) {
            return c.moveToFirst() ? recordFrom(c) : null;
        }
    }

    public int countAttendance() {
        return scalar("SELECT COUNT(*) FROM " + T_ATT, null);
    }

    /** Số sinh viên (không trùng) đã điểm danh hôm nay. */
    public int countPresentToday() {
        long start = DateUtils.startOfDay(System.currentTimeMillis());
        return scalar("SELECT COUNT(DISTINCT student_code) FROM " + T_ATT + " WHERE timestamp>=?",
                new String[]{String.valueOf(start)});
    }

    /** Số lượt điểm danh mỗi ngày trong n ngày gần nhất (phần tử cuối = hôm nay). */
    public int[] countLastDays(int n) {
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            long from = DateUtils.startOfDayAgo(n - 1 - i);
            long to = DateUtils.startOfDayAgo(n - 2 - i);
            if (i == n - 1) to = Long.MAX_VALUE;
            result[i] = scalar("SELECT COUNT(*) FROM " + T_ATT + " WHERE timestamp>=? AND timestamp<?",
                    new String[]{String.valueOf(from), String.valueOf(to)});
        }
        return result;
    }

    /** Map MSSV -> số lần đã điểm danh. */
    public Map<String, Integer> getAttendanceCounts() {
        Map<String, Integer> map = new HashMap<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT student_code, COUNT(*) FROM " + T_ATT + " GROUP BY student_code", null)) {
            while (c.moveToNext()) map.put(c.getString(0), c.getInt(1));
        }
        return map;
    }

    private AttendanceRecord recordFrom(Cursor c) {
        AttendanceRecord r = new AttendanceRecord();
        r.id = c.getLong(c.getColumnIndexOrThrow("id"));
        r.studentCode = c.getString(c.getColumnIndexOrThrow("student_code"));
        r.studentName = c.getString(c.getColumnIndexOrThrow("student_name"));
        r.className = c.getString(c.getColumnIndexOrThrow("class_name"));
        r.sessionName = c.getString(c.getColumnIndexOrThrow("session_name"));
        r.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));
        r.status = c.getString(c.getColumnIndexOrThrow("status"));
        r.note = c.getString(c.getColumnIndexOrThrow("note"));
        return r;
    }

    private int scalar(String sql, String[] args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    // ============================== DỮ LIỆU MẪU ==============================

    /** Thêm sinh viên + lịch sử điểm danh 7 ngày gần nhất để test giao diện. */
    public void insertSampleData(String sessionName) {
        String[][] data = {
                {"2424801030001", "Nguyễn Văn An", "D24_TH01"},
                {"2424801030002", "Trần Thị Bích Ngọc", "D24_TH01"},
                {"2424801030003", "Lê Hoàng Minh", "D24_TH01"},
                {"2424801030004", "Phạm Quốc Bảo", "D24_TH02"},
                {"2424801030005", "Võ Thị Thu Hà", "D24_TH02"},
                {"2424801030006", "Đặng Gia Huy", "D24_TH02"},
                {"2424801030007", "Huỳnh Thanh Tùng", "D24_TH03"},
                {"2424801030008", "Bùi Ngọc Diệp", "D24_TH03"},
        };
        for (String[] d : data) {
            Student s = new Student(d[0], d[1], d[2]);
            insertStudent(s);
        }
        List<Student> all = getStudents(null);
        Random rnd = new Random(42);
        for (int day = 6; day >= 0; day--) {
            long base = DateUtils.startOfDayAgo(day);
            for (Student s : all) {
                if (day == 0 && rnd.nextInt(3) == 0) continue;
                if (day > 0 && rnd.nextInt(4) == 0) continue;
                if (findOnDay(s.code, sessionName, base)) continue;
                AttendanceRecord r = new AttendanceRecord();
                r.studentCode = s.code;
                r.studentName = s.name;
                r.className = s.className;
                r.sessionName = sessionName;
                int minutes = 7 * 60 + rnd.nextInt(40);
                long ts = base + minutes * 60_000L;
                if (day == 0 && ts > System.currentTimeMillis()) {
                    ts = System.currentTimeMillis() - rnd.nextInt(30) * 60_000L;
                }
                r.timestamp = ts;
                r.status = minutes > 7 * 60 + 15 ? AttendanceRecord.STATUS_LATE
                        : AttendanceRecord.STATUS_PRESENT;
                insertAttendance(r);
            }
        }
    }

    private boolean findOnDay(String code, String session, long dayStart) {
        try (Cursor c = getReadableDatabase().query(T_ATT, new String[]{"id"},
                "student_code=? AND session_name=? AND timestamp>=? AND timestamp<?",
                new String[]{code, session, String.valueOf(dayStart),
                        String.valueOf(dayStart + 24L * 3600_000L)},
                null, null, null, "1")) {
            return c.moveToFirst();
        }
    }
}
