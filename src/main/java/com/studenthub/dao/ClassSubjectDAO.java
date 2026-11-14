package com.studenthub.dao;

import com.studenthub.config.DBConnection;
import com.studenthub.exception.AppException;
import com.studenthub.model.ClassSubject;
import com.studenthub.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassSubjectDAO {

    private static final String BASE_SELECT = """
            SELECT cs.*,
                   c.class_code, c.name AS class_name,
                   sub.subject_code, sub.name AS subject_name, sub.credits,
                   t.teacher_code, t.full_name AS teacher_name
            FROM class_subject cs
            JOIN classes c ON cs.class_id = c.id
            JOIN subjects sub ON cs.subject_id = sub.id
            JOIN teachers t ON cs.teacher_id = t.id
            """;

    public List<ClassSubject> findAll() {
        String sql = BASE_SELECT + " ORDER BY cs.semester DESC, c.class_code ASC, sub.subject_code ASC";
        List<ClassSubject> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassSubjectDAO.findAll", e);
            throw new AppException("Lỗi lấy danh sách phân công giảng dạy.", e);
        }
        return list;
    }

    public List<ClassSubject> findByTeacherId(int teacherId) {
        String sql = BASE_SELECT + " WHERE cs.teacher_id = ? ORDER BY cs.semester DESC, c.class_code ASC";
        List<ClassSubject> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassSubjectDAO.findByTeacherId", e);
            throw new AppException("Lỗi lấy danh sách lớp học phần của giảng viên.", e);
        }
        return list;
    }

    public Optional<ClassSubject> findById(int id) {
        String sql = BASE_SELECT + " WHERE cs.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassSubjectDAO.findById", e);
            throw new AppException("Lỗi tìm lớp học phần theo ID.", e);
        }
        return Optional.empty();
    }

    public boolean exists(int classId, int subjectId, String semester) {
        String sql = "SELECT COUNT(*) FROM class_subject WHERE class_id = ? AND subject_id = ? AND semester = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            ps.setInt(2, subjectId);
            ps.setString(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassSubjectDAO.exists", e);
            throw new AppException("Lỗi kiểm tra trùng phân công giảng dạy.", e);
        }
    }

    public int insert(int classId, int subjectId, int teacherId, String semester) {
        String sql = "INSERT INTO class_subject (class_id, subject_id, teacher_id, semester, is_locked) VALUES (?, ?, ?, ?, FALSE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, classId);
            ps.setInt(2, subjectId);
            ps.setInt(3, teacherId);
            ps.setString(4, semester);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassSubjectDAO.insert", e);
            throw new AppException("Lỗi phân công giảng dạy: " + e.getMessage(), e);
        }
        return 0;
    }

    public void updateLockStatus(int classSubjectId, boolean locked) {
        String sqlCs = "UPDATE class_subject SET is_locked = ? WHERE id = ?";
        String sqlScores = "UPDATE scores SET locked = ? WHERE class_subject_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sqlCs);
                 PreparedStatement ps2 = conn.prepareStatement(sqlScores)) {
                ps1.setBoolean(1, locked);
                ps1.setInt(2, classSubjectId);
                ps1.executeUpdate();

                ps2.setBoolean(1, locked);
                ps2.setInt(2, classSubjectId);
                ps2.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                DBConnection.rollbackQuietly(conn);
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassSubjectDAO.updateLockStatus", e);
            throw new AppException("Lỗi khóa/mở khóa sổ điểm học phần.", e);
        }
    }

    private ClassSubject mapRow(ResultSet rs) throws SQLException {
        return ClassSubject.builder()
                .id(rs.getInt("id"))
                .classId(rs.getInt("class_id"))
                .classCode(rs.getString("class_code"))
                .className(rs.getString("class_name"))
                .subjectId(rs.getInt("subject_id"))
                .subjectCode(rs.getString("subject_code"))
                .subjectName(rs.getString("subject_name"))
                .credits(rs.getInt("credits"))
                .teacherId(rs.getInt("teacher_id"))
                .teacherCode(rs.getString("teacher_code"))
                .teacherName(rs.getString("teacher_name"))
                .semester(rs.getString("semester"))
                .locked(rs.getBoolean("is_locked"))
                .build();
    }
}
