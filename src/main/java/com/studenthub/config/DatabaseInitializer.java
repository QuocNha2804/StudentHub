package com.studenthub.config;

import com.studenthub.util.LogUtil;
import com.studenthub.util.PasswordUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        try (Connection conn = DBConnection.getConnection()) {
            runSchemaScript(conn);
            seedInitialDataIfEmpty(conn);
        } catch (Exception e) {
            LogUtil.error("Khởi tạo Database thất bại", e);
            throw new RuntimeException("Không thể khởi tạo Database: " + e.getMessage(), e);
        }
    }

    private static void runSchemaScript(Connection conn) throws Exception {
        try (InputStream is = DatabaseInitializer.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                LogUtil.warn("Không tìm thấy file schema.sql trong resources.");
                return;
            }
            String sqlContent = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));

            String[] statements = sqlContent.split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String rawStmt : statements) {
                    String sql = rawStmt.trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                }
            }
            LogUtil.info("Đã kiểm tra và khởi tạo schema CSDL thành công.");
        }
    }

    private static void seedInitialDataIfEmpty(Connection conn) throws Exception {
        String checkSql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        conn.setAutoCommit(false);
        try {
            String adminPass = PasswordUtil.hashPassword("Admin@123");
            String defaultPass = PasswordUtil.hashPassword("123456");

            // 1. Tạo Users (Admin, 2 Teachers, 3 Students)
            int adminUserId = insertUser(conn, "admin", "admin@studenthub.edu.vn", adminPass, "ADMIN");
            int gv1UserId = insertUser(conn, "gv001", "nguyenvanan@studenthub.edu.vn", defaultPass, "TEACHER");
            int gv2UserId = insertUser(conn, "gv002", "tranthibinh@studenthub.edu.vn", defaultPass, "TEACHER");
            int sv1UserId = insertUser(conn, "SV001", "sv001@studenthub.edu.vn", defaultPass, "STUDENT");
            int sv2UserId = insertUser(conn, "SV002", "sv002@studenthub.edu.vn", defaultPass, "STUDENT");
            int sv3UserId = insertUser(conn, "SV003", "sv003@studenthub.edu.vn", defaultPass, "STUDENT");

            // 2. Tạo Teachers
            int teacher1Id = insertTeacher(conn, "GV001", "TS. Nguyễn Văn An", "nguyenvanan@studenthub.edu.vn", "0901234567", "Công nghệ Phần mềm", gv1UserId);
            int teacher2Id = insertTeacher(conn, "GV002", "ThS. Trần Thị Bình", "tranthibinh@studenthub.edu.vn", "0902345678", "Hệ thống Thông tin", gv2UserId);

            // 3. Tạo Classes
            int class1Id = insertClass(conn, "DHKTPM18A", "Đại học Kỹ thuật Phần mềm 18A", "2022-2026", teacher1Id);
            int class2Id = insertClass(conn, "DHHTTT18B", "Đại học Hệ thống Thông tin 18B", "2022-2026", teacher2Id);

            // 4. Tạo Subjects
            int subJavaId = insertSubject(conn, "JAVA01", "Lập trình Hướng đối tượng với Java", 3, "Công nghệ Phần mềm");
            int subDbId = insertSubject(conn, "DB01", "Hệ Quản trị Cơ sở dữ liệu", 3, "Hệ thống Thông tin");
            int subDsaId = insertSubject(conn, "DSA01", "Cấu trúc Dữ liệu và Giải thuật", 4, "Khoa học Máy tính");

            // 5. Tạo Students
            int st1Id = insertStudent(conn, "SV001", "Lê Hoàng Nam", "2004-03-15", "MALE", "sv001@studenthub.edu.vn", "0911222333", "Quận Gò Vấp, TP.HCM", class1Id, sv1UserId);
            int st2Id = insertStudent(conn, "SV002", "Phạm Ngọc Mai", "2004-07-22", "FEMALE", "sv002@studenthub.edu.vn", "0922333444", "Quận Bình Thạnh, TP.HCM", class1Id, sv2UserId);
            int st3Id = insertStudent(conn, "SV003", "Đỗ Minh Tuấn", "2004-11-05", "MALE", "sv003@studenthub.edu.vn", "0933444555", "TP. Thủ Đức, TP.HCM", class2Id, sv3UserId);

            // 6. Tạo Class_Subject (Phân công giảng dạy)
            int cs1Id = insertClassSubject(conn, class1Id, subJavaId, teacher1Id, "2026-1");
            int cs2Id = insertClassSubject(conn, class1Id, subDbId, teacher2Id, "2026-1");
            int cs3Id = insertClassSubject(conn, class2Id, subDsaId, teacher1Id, "2026-1");

            // 7. Tạo Điểm mẫu
            insertScore(conn, st1Id, cs1Id, 9.0, 8.5, 9.0, 8.85, "A");
            insertScore(conn, st1Id, cs2Id, 8.0, 7.5, 8.0, 7.85, "B");
            insertScore(conn, st2Id, cs1Id, 7.0, 6.5, 7.0, 6.85, "C");
            insertScore(conn, st2Id, cs2Id, 5.0, 3.5, 3.0, 3.35, "F");
            insertScore(conn, st3Id, cs3Id, 8.0, 8.0, 7.5, 7.70, "B");

            conn.commit();
            LogUtil.info("Seed dữ liệu mẫu thành công (adminUserId=" + adminUserId + ").");
        } catch (Exception e) {
            DBConnection.rollbackQuietly(conn);
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static int insertUser(Connection conn, String username, String email, String hash, String role) throws Exception {
        String sql = "INSERT INTO users (username, email, password_hash, role, is_active) VALUES (?, ?, ?, ?, TRUE)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, role);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int insertTeacher(Connection conn, String code, String name, String email, String phone, String dept, int userId) throws Exception {
        String sql = "INSERT INTO teachers (teacher_code, full_name, email, phone, department, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setString(5, dept);
            ps.setInt(6, userId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int insertClass(Connection conn, String code, String name, String year, int teacherId) throws Exception {
        String sql = "INSERT INTO classes (class_code, name, academic_year, homeroom_teacher_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, year);
            ps.setInt(4, teacherId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int insertSubject(Connection conn, String code, String name, int credits, String dept) throws Exception {
        String sql = "INSERT INTO subjects (subject_code, name, credits, department) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setInt(3, credits);
            ps.setString(4, dept);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int insertStudent(Connection conn, String code, String name, String dob, String gender, String email, String phone, String address, int classId, int userId) throws Exception {
        String sql = "INSERT INTO students (student_code, full_name, dob, gender, email, phone, address, class_id, user_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setDate(3, java.sql.Date.valueOf(dob));
            ps.setString(4, gender);
            ps.setString(5, email);
            ps.setString(6, phone);
            ps.setString(7, address);
            ps.setInt(8, classId);
            ps.setInt(9, userId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static int insertClassSubject(Connection conn, int classId, int subjectId, int teacherId, String semester) throws Exception {
        String sql = "INSERT INTO class_subject (class_id, subject_id, teacher_id, semester, is_locked) VALUES (?, ?, ?, ?, FALSE)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, classId);
            ps.setInt(2, subjectId);
            ps.setInt(3, teacherId);
            ps.setString(4, semester);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void insertScore(Connection conn, int studentId, int csId, double att, double mid, double fin, double total, String grade) throws Exception {
        String sql = "INSERT INTO scores (student_id, class_subject_id, attendance_score, midterm_score, final_score, total_score, grade, locked) VALUES (?, ?, ?, ?, ?, ?, ?, FALSE)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, csId);
            ps.setDouble(3, att);
            ps.setDouble(4, mid);
            ps.setDouble(5, fin);
            ps.setDouble(6, total);
            ps.setString(7, grade);
            ps.executeUpdate();
        }
    }
}
