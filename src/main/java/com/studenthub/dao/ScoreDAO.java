package com.studenthub.dao;

import com.studenthub.config.DBConnection;
import com.studenthub.dto.TranscriptItemDTO;
import com.studenthub.exception.AppException;
import com.studenthub.model.Score;
import com.studenthub.model.enums.Grade;
import com.studenthub.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScoreDAO {

    /**
     * Lấy danh sách toàn bộ SV thuộc lớp của học phần (kể cả SV chưa nhập điểm).
     */
    public List<Score> findScoresByClassSubject(int classSubjectId) {
        String sql = """
                SELECT sc.id,
                       s.id AS student_id, s.student_code, s.full_name AS student_name,
                       cs.id AS class_subject_id, cs.semester, cs.is_locked AS cs_locked,
                       sub.subject_code, sub.name AS subject_name, sub.credits,
                       sc.attendance_score, sc.midterm_score, sc.final_score,
                       sc.total_score, sc.grade, COALESCE(sc.locked, cs.is_locked) AS locked, sc.updated_at
                FROM class_subject cs
                JOIN students s ON s.class_id = cs.class_id AND s.deleted_at IS NULL
                JOIN subjects sub ON cs.subject_id = sub.id
                LEFT JOIN scores sc ON sc.student_id = s.id AND sc.class_subject_id = cs.id
                WHERE cs.id = ?
                ORDER BY s.student_code ASC
                """;
        List<Score> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classSubjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ScoreDAO.findScoresByClassSubject", e);
            throw new AppException("Lỗi tải bảng điểm lớp học phần.", e);
        }
        return list;
    }

    public Optional<Score> findByStudentAndClassSubject(int studentId, int classSubjectId) {
        String sql = """
                SELECT sc.id,
                       s.id AS student_id, s.student_code, s.full_name AS student_name,
                       cs.id AS class_subject_id, cs.semester,
                       sub.subject_code, sub.name AS subject_name, sub.credits,
                       sc.attendance_score, sc.midterm_score, sc.final_score,
                       sc.total_score, sc.grade, sc.locked, sc.updated_at
                FROM scores sc
                JOIN students s ON sc.student_id = s.id
                JOIN class_subject cs ON sc.class_subject_id = cs.id
                JOIN subjects sub ON cs.subject_id = sub.id
                WHERE sc.student_id = ? AND sc.class_subject_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, classSubjectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ScoreDAO.findByStudentAndClassSubject", e);
            throw new AppException("Lỗi kiểm tra điểm của sinh viên.", e);
        }
        return Optional.empty();
    }

    public void upsertScore(Score score) {
        String sql = """
                INSERT INTO scores (student_id, class_subject_id, attendance_score, midterm_score, final_score, total_score, grade, locked)
                VALUES (?, ?, ?, ?, ?, ?, ?, FALSE)
                ON DUPLICATE KEY UPDATE
                    attendance_score = VALUES(attendance_score),
                    midterm_score = VALUES(midterm_score),
                    final_score = VALUES(final_score),
                    total_score = VALUES(total_score),
                    grade = VALUES(grade),
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, score.getStudentId());
            ps.setInt(2, score.getClassSubjectId());
            setNullableDouble(ps, 3, score.getAttendanceScore());
            setNullableDouble(ps, 4, score.getMidtermScore());
            setNullableDouble(ps, 5, score.getFinalScore());
            setNullableDouble(ps, 6, score.getTotalScore());
            if (score.getGrade() != null) {
                ps.setString(7, score.getGrade().name());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi ScoreDAO.upsertScore", e);
            throw new AppException("Lỗi lưu điểm sinh viên: " + e.getMessage(), e);
        }
    }

    public List<TranscriptItemDTO> findTranscriptByStudent(int studentId, String semesterFilter) {
        StringBuilder sql = new StringBuilder("""
                SELECT cs.semester, sub.subject_code, sub.name AS subject_name, sub.credits,
                       sc.attendance_score, sc.midterm_score, sc.final_score, sc.total_score,
                       sc.grade, COALESCE(sc.locked, cs.is_locked) AS locked
                FROM students s
                JOIN class_subject cs ON s.class_id = cs.class_id
                JOIN subjects sub ON cs.subject_id = sub.id
                LEFT JOIN scores sc ON sc.student_id = s.id AND sc.class_subject_id = cs.id
                WHERE s.id = ?
                """);
        boolean hasSem = semesterFilter != null && !semesterFilter.trim().isEmpty();
        if (hasSem) {
            sql.append(" AND cs.semester = ? ");
        }
        sql.append(" ORDER BY cs.semester ASC, sub.subject_code ASC");

        List<TranscriptItemDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, studentId);
            if (hasSem) {
                ps.setString(2, semesterFilter.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String gradeStr = rs.getString("grade");
                    list.add(TranscriptItemDTO.builder()
                            .semester(rs.getString("semester"))
                            .subjectCode(rs.getString("subject_code"))
                            .subjectName(rs.getString("subject_name"))
                            .credits(rs.getInt("credits"))
                            .attendanceScore(getNullableDouble(rs, "attendance_score"))
                            .midtermScore(getNullableDouble(rs, "midterm_score"))
                            .finalScore(getNullableDouble(rs, "final_score"))
                            .totalScore(getNullableDouble(rs, "total_score"))
                            .grade(gradeStr != null ? Grade.valueOf(gradeStr) : null)
                            .locked(rs.getBoolean("locked"))
                            .build());
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ScoreDAO.findTranscriptByStudent", e);
            throw new AppException("Lỗi lấy bảng điểm học tập của sinh viên.", e);
        }
        return list;
    }

    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value != null) {
            ps.setDouble(index, value);
        } else {
            ps.setNull(index, Types.DECIMAL);
        }
    }

    private Double getNullableDouble(ResultSet rs, String col) throws SQLException {
        double val = rs.getDouble(col);
        return rs.wasNull() ? null : val;
    }

    private Score mapRow(ResultSet rs) throws SQLException {
        String gradeStr = rs.getString("grade");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        return Score.builder()
                .id(rs.getInt("id"))
                .studentId(rs.getInt("student_id"))
                .studentCode(rs.getString("student_code"))
                .studentName(rs.getString("student_name"))
                .classSubjectId(rs.getInt("class_subject_id"))
                .subjectCode(rs.getString("subject_code"))
                .subjectName(rs.getString("subject_name"))
                .credits(rs.getInt("credits"))
                .semester(rs.getString("semester"))
                .attendanceScore(getNullableDouble(rs, "attendance_score"))
                .midtermScore(getNullableDouble(rs, "midterm_score"))
                .finalScore(getNullableDouble(rs, "final_score"))
                .totalScore(getNullableDouble(rs, "total_score"))
                .grade(gradeStr != null ? Grade.valueOf(gradeStr) : null)
                .locked(rs.getBoolean("locked"))
                .updatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null)
                .build();
    }
}
