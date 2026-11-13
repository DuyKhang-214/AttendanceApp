package com.khang.attendanceapp.model;

import java.io.Serializable;

/** Sinh viên (đã đăng ký trong danh sách lớp). */
public class Student implements Serializable {
    public long id;
    public String code;      // MSSV (chính là nội dung mã vạch trên thẻ)
    public String name;
    public String className;
    public String phone;
    public String email;
    public long createdAt;

    public Student() {
    }

    public Student(String code, String name, String className) {
        this.code = code;
        this.name = name;
        this.className = className;
        this.createdAt = System.currentTimeMillis();
    }
}
