package com.studenthub.dao;

import com.studenthub.config.DBConnection;
import com.studenthub.exception.AppException;
import com.studenthub.model.SchoolClass;
import com.studenthub.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassDAO {

    private static final String BASE_SELECT = """
            SELECT c.*, t.full_name AS teacher_name,
                   (SELECT COUNT(*) FROM students s WHERE s.class_id = c.id AND s.deleted_at IS NULL) AS student_count
            FROM classes c
            LEFT JOIN teachers t ON c.homeroom_teacher_id = t.id
            """;

    public List<SchoolClass> findAll() {
        String sql = BASE_SELECT + " ORDER BY c.class_code ASC";
        List<SchoolClass> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassDAO.findAll", e);
            throw new AppException("Lỗi lấy danh sách lớp học.", e);
        }
        return list;
    }

    public List<SchoolClass> findByHomeroomTeacherId(int teacherId) {
        String sql = BASE_SELECT + " WHERE c.homeroom_teacher_id = ? ORDER BY c.class_code ASC";
        List<SchoolClass> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassDAO.findByHomeroomTeacherId", e);
            throw new AppException("Lỗi lấy danh sách lớp chủ nhiệm.", e);
        }
        return list;
    }

    public Optional<SchoolClass> findById(int id) {
        String sql = BASE_SELECT + " WHERE c.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassDAO.findById", e);
            throw new AppException("Lỗi tìm lớp học theo ID.", e);
        }
        return Optional.empty();
    }

    public Optional<SchoolClass> findByCode(String classCode) {
        String sql = BASE_SELECT + " WHERE c.class_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, classCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassDAO.findByCode", e);
            throw new AppException("Lỗi tìm lớp học theo mã lớp.", e);
        }
        return Optional.empty();
    }

    public int insert(SchoolClass schoolClass) {
        String sql = "INSERT INTO classes (class_code, name, academic_year, homeroom_teacher_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, schoolClass.getClassCode());
            ps.setString(2, schoolClass.getName());
            ps.setString(3, schoolClass.getAcademicYear());
            if (schoolClass.getHomeroomTeacherId() != null) {
                ps.setInt(4, schoolClass.getHomeroomTeacherId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassDAO.insert", e);
            throw new AppException("Lỗi tạo lớp học: " + e.getMessage(), e);
        }
        return 0;
    }

    public void update(SchoolClass schoolClass) {
        String sql = "UPDATE classes SET class_code = ?, name = ?, academic_year = ?, homeroom_teacher_id = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schoolClass.getClassCode());
            ps.setString(2, schoolClass.getName());
            ps.setString(3, schoolClass.getAcademicYear());
            if (schoolClass.getHomeroomTeacherId() != null) {
                ps.setInt(4, schoolClass.getHomeroomTeacherId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, schoolClass.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassDAO.update", e);
            throw new AppException("Lỗi cập nhật lớp học: " + e.getMessage(), e);
        }
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM classes WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi ClassDAO.deleteById", e);
            throw new AppException("Không thể xóa lớp học (có thể đang chứa học phần hoặc dữ liệu liên quan).", e);
        }
    }

    private SchoolClass mapRow(ResultSet rs) throws SQLException {
        int teacherIdVal = rs.getInt("homeroom_teacher_id");
        Integer teacherId = rs.wasNull() ? null : teacherIdVal;
        return SchoolClass.builder()
                .id(rs.getInt("id"))
                .classCode(rs.getString("class_code"))
                .name(rs.getString("name"))
                .academicYear(rs.getString("academic_year"))
                .homeroomTeacherId(teacherId)
                .homeroomTeacherName(rs.getString("teacher_name"))
                .studentCount(rs.getInt("student_count"))
                .build();
    }
}
