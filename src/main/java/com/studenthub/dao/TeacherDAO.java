package com.studenthub.dao;

import com.studenthub.config.DBConnection;
import com.studenthub.exception.AppException;
import com.studenthub.model.Teacher;
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

public class TeacherDAO {

    private static final String BASE_SELECT = """
            SELECT t.*, u.username
            FROM teachers t
            LEFT JOIN users u ON t.user_id = u.id
            """;

    public List<Teacher> findAll() {
        String sql = BASE_SELECT + " ORDER BY t.teacher_code ASC";
        List<Teacher> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi TeacherDAO.findAll", e);
            throw new AppException("Lỗi lấy danh sách giảng viên.", e);
        }
        return list;
    }

    public Optional<Teacher> findById(int id) {
        String sql = BASE_SELECT + " WHERE t.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi TeacherDAO.findById", e);
            throw new AppException("Lỗi tìm giảng viên theo ID.", e);
        }
        return Optional.empty();
    }

    public Optional<Teacher> findByCode(String code) {
        String sql = BASE_SELECT + " WHERE t.teacher_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi TeacherDAO.findByCode", e);
            throw new AppException("Lỗi tìm giảng viên theo mã.", e);
        }
        return Optional.empty();
    }

    public Optional<Teacher> findByUserId(int userId) {
        String sql = BASE_SELECT + " WHERE t.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi TeacherDAO.findByUserId", e);
            throw new AppException("Lỗi tìm hồ sơ giảng viên theo tài khoản.", e);
        }
        return Optional.empty();
    }

    public int insert(Connection conn, Teacher teacher) throws SQLException {
        String sql = "INSERT INTO teachers (teacher_code, full_name, email, phone, department, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, teacher.getTeacherCode());
            ps.setString(2, teacher.getFullName());
            ps.setString(3, teacher.getEmail());
            ps.setString(4, teacher.getPhone());
            ps.setString(5, teacher.getDepartment());
            if (teacher.getUserId() != null) {
                ps.setInt(6, teacher.getUserId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    teacher.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public void update(Teacher teacher) {
        String sql = "UPDATE teachers SET full_name = ?, email = ?, phone = ?, department = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, teacher.getFullName());
            ps.setString(2, teacher.getEmail());
            ps.setString(3, teacher.getPhone());
            ps.setString(4, teacher.getDepartment());
            ps.setInt(5, teacher.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi TeacherDAO.update", e);
            throw new AppException("Lỗi cập nhật giảng viên: " + e.getMessage(), e);
        }
    }

    private Teacher mapRow(ResultSet rs) throws SQLException {
        int userIdVal = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : userIdVal;
        return Teacher.builder()
                .id(rs.getInt("id"))
                .teacherCode(rs.getString("teacher_code"))
                .fullName(rs.getString("full_name"))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .department(rs.getString("department"))
                .userId(userId)
                .username(rs.getString("username"))
                .build();
    }
}
