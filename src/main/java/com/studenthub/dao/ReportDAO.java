package com.studenthub.dao;

import com.studenthub.config.DBConnection;
import com.studenthub.dto.StudentGpaDTO;
import com.studenthub.dto.SubjectPassRateDTO;
import com.studenthub.exception.AppException;
import com.studenthub.model.enums.Gender;
import com.studenthub.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    /**
     * Thống kê số lượng SV theo từng lớp niên chế.
     */
    public List<String[]> countStudentsByClass() {
        String sql = """
                SELECT c.class_code, c.name, c.academic_year, COALESCE(t.full_name, 'Chưa phân công') AS teacher_name,
                       COUNT(s.id) AS total_students,
                       SUM(CASE WHEN s.gender = 'MALE' THEN 1 ELSE 0 END) AS male_count,
                       SUM(CASE WHEN s.gender = 'FEMALE' THEN 1 ELSE 0 END) AS female_count
                FROM classes c
                LEFT JOIN teachers t ON c.homeroom_teacher_id = t.id
                LEFT JOIN students s ON s.class_id = c.id AND s.deleted_at IS NULL
                GROUP BY c.id
                ORDER BY c.class_code ASC
                """;
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("class_code"),
                        rs.getString("name"),
                        rs.getString("academic_year"),
                        rs.getString("teacher_name"),
                        String.valueOf(rs.getInt("total_students")),
                        String.valueOf(rs.getInt("male_count")),
                        String.valueOf(rs.getInt("female_count"))
                });
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ReportDAO.countStudentsByClass", e);
            throw new AppException("Lỗi thống kê số lượng sinh viên theo lớp.", e);
        }
        return rows;
    }

    /**
     * Thống kê số lượng SV theo giới tính toàn trường.
     */
    public List<String[]> countStudentsByGender() {
        String sql = """
                SELECT gender, COUNT(*) AS cnt,
                       ROUND(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM students WHERE deleted_at IS NULL), 0), 2) AS percentage
                FROM students
                WHERE deleted_at IS NULL
                GROUP BY gender
                ORDER BY cnt DESC
                """;
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Gender g = Gender.valueOf(rs.getString("gender"));
                rows.add(new String[]{
                        g.name(),
                        g.getVietnameseLabel(),
                        String.valueOf(rs.getInt("cnt")),
                        String.format("%.2f%%", rs.getDouble("percentage"))
                });
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ReportDAO.countStudentsByGender", e);
            throw new AppException("Lỗi thống kê sinh viên theo giới tính.", e);
        }
        return rows;
    }

    /**
     * Lấy danh sách SV kèm GPA (có thể lọc theo classId, sắp xếp cao nhất/thấp nhất, giới hạn limit).
     */
    public List<StudentGpaDTO> findTopStudentsByGpa(Integer classId, boolean highestFirst, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.id AS student_id, s.student_code, s.full_name,
                       COALESCE(c.class_code, 'Chưa xếp lớp') AS class_code, s.gender,
                       COALESCE(SUM(CASE WHEN sc.total_score IS NOT NULL THEN sub.credits ELSE 0 END), 0) AS total_credits,
                       ROUND(SUM(CASE WHEN sc.total_score IS NOT NULL THEN sc.total_score * sub.credits ELSE 0 END) /
                             NULLIF(SUM(CASE WHEN sc.total_score IS NOT NULL THEN sub.credits ELSE 0 END), 0), 2) AS gpa10,
                       ROUND(SUM(CASE sc.grade
                                   WHEN 'A' THEN 4.0 * sub.credits
                                   WHEN 'B' THEN 3.0 * sub.credits
                                   WHEN 'C' THEN 2.0 * sub.credits
                                   WHEN 'D' THEN 1.0 * sub.credits
                                   WHEN 'F' THEN 0.0
                                   ELSE 0 END) /
                             NULLIF(SUM(CASE WHEN sc.grade IS NOT NULL THEN sub.credits ELSE 0 END), 0), 2) AS gpa4
                FROM students s
                LEFT JOIN classes c ON s.class_id = c.id
                LEFT JOIN scores sc ON s.id = sc.student_id
                LEFT JOIN class_subject cs ON sc.class_subject_id = cs.id
                LEFT JOIN subjects sub ON cs.subject_id = sub.id
                WHERE s.deleted_at IS NULL
                """);
        if (classId != null) {
            sql.append(" AND s.class_id = ? ");
        }
        sql.append(" GROUP BY s.id HAVING total_credits > 0 ");
        sql.append(" ORDER BY gpa10 ").append(highestFirst ? "DESC" : "ASC").append(", gpa4 ").append(highestFirst ? "DESC" : "ASC");
        sql.append(" LIMIT ? ");

        List<StudentGpaDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (classId != null) {
                ps.setInt(idx++, classId);
            }
            ps.setInt(idx, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double gpa4 = rs.getDouble("gpa4");
                    list.add(StudentGpaDTO.builder()
                            .studentId(rs.getInt("student_id"))
                            .studentCode(rs.getString("student_code"))
                            .fullName(rs.getString("full_name"))
                            .classCode(rs.getString("class_code"))
                            .gender(Gender.valueOf(rs.getString("gender")).getVietnameseLabel())
                            .totalCredits(rs.getInt("total_credits"))
                            .gpa10(rs.getDouble("gpa10"))
                            .gpa4(gpa4)
                            .academicRank(classifyAcademicRank(gpa4))
                            .build());
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ReportDAO.findTopStudentsByGpa", e);
            throw new AppException("Lỗi thống kê xếp hạng GPA sinh viên.", e);
        }
        return list;
    }

    /**
     * Tỉ lệ đạt / rớt theo từng môn học toàn trường.
     */
    public List<SubjectPassRateDTO> getPassRateBySubject() {
        String sql = """
                SELECT sub.id AS subject_id, sub.subject_code, sub.name AS subject_name, sub.credits,
                       COUNT(sc.id) AS total_graded,
                       SUM(CASE WHEN sc.total_score >= 4.0 THEN 1 ELSE 0 END) AS passed_count,
                       SUM(CASE WHEN sc.total_score < 4.0 THEN 1 ELSE 0 END) AS failed_count,
                       ROUND(AVG(sc.total_score), 2) AS avg_score
                FROM subjects sub
                LEFT JOIN class_subject cs ON sub.id = cs.subject_id
                LEFT JOIN scores sc ON cs.id = sc.class_subject_id AND sc.total_score IS NOT NULL
                GROUP BY sub.id
                ORDER BY sub.subject_code ASC
                """;
        List<SubjectPassRateDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int total = rs.getInt("total_graded");
                int passed = rs.getInt("passed_count");
                int failed = rs.getInt("failed_count");
                double passRate = total > 0 ? Math.round((passed * 10000.0) / total) / 100.0 : 0.0;
                list.add(SubjectPassRateDTO.builder()
                        .subjectId(rs.getInt("subject_id"))
                        .subjectCode(rs.getString("subject_code"))
                        .subjectName(rs.getString("subject_name"))
                        .credits(rs.getInt("credits"))
                        .totalGradedStudents(total)
                        .passedCount(passed)
                        .failedCount(failed)
                        .passRatePercent(passRate)
                        .averageScore(rs.getDouble("avg_score"))
                        .build());
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ReportDAO.getPassRateBySubject", e);
            throw new AppException("Lỗi thống kê tỉ lệ đạt/rớt theo môn học.", e);
        }
        return list;
    }

    public static String classifyAcademicRank(double gpa4) {
        if (gpa4 >= 3.6) return "Xuất sắc";
        if (gpa4 >= 3.2) return "Giỏi";
        if (gpa4 >= 2.5) return "Khá";
        if (gpa4 >= 2.0) return "Trung bình";
        return "Yếu / Kém";
    }
}
