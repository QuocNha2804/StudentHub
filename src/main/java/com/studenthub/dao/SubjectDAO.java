package com.studenthub.dao;

import com.studenthub.config.DBConnection;
import com.studenthub.exception.AppException;
import com.studenthub.model.Subject;
import com.studenthub.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubjectDAO {

    public List<Subject> findAll() {
        String sql = "SELECT * FROM subjects ORDER BY subject_code ASC";
        List<Subject> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi SubjectDAO.findAll", e);
            throw new AppException("Lỗi lấy danh sách môn học.", e);
        }
        return list;
    }

    public Optional<Subject> findById(int id) {
        String sql = "SELECT * FROM subjects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi SubjectDAO.findById", e);
            throw new AppException("Lỗi tìm môn học theo ID.", e);
        }
        return Optional.empty();
    }

    public Optional<Subject> findByCode(String code) {
        String sql = "SELECT * FROM subjects WHERE subject_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi SubjectDAO.findByCode", e);
            throw new AppException("Lỗi tìm môn học theo mã môn.", e);
        }
        return Optional.empty();
    }

    public int insert(Subject subject) {
        String sql = "INSERT INTO subjects (subject_code, name, credits, department) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, subject.getSubjectCode());
            ps.setString(2, subject.getName());
            ps.setInt(3, subject.getCredits());
            ps.setString(4, subject.getDepartment());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi SubjectDAO.insert", e);
            throw new AppException("Lỗi thêm môn học: " + e.getMessage(), e);
        }
        return 0;
    }

    public void update(Subject subject) {
        String sql = "UPDATE subjects SET subject_code = ?, name = ?, credits = ?, department = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getSubjectCode());
            ps.setString(2, subject.getName());
            ps.setInt(3, subject.getCredits());
            ps.setString(4, subject.getDepartment());
            ps.setInt(5, subject.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi SubjectDAO.update", e);
            throw new AppException("Lỗi cập nhật môn học: " + e.getMessage(), e);
        }
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi SubjectDAO.deleteById", e);
            throw new AppException("Không thể xóa môn học đã có phân công giảng dạy hoặc bảng điểm.", e);
        }
    }

    private Subject mapRow(ResultSet rs) throws SQLException {
        return Subject.builder()
                .id(rs.getInt("id"))
                .subjectCode(rs.getString("subject_code"))
                .name(rs.getString("name"))
                .credits(rs.getInt("credits"))
                .department(rs.getString("department"))
                .build();
    }
}
