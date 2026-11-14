package com.studenthub.dao;

import com.studenthub.config.DBConnection;
import com.studenthub.exception.AppException;
import com.studenthub.model.User;
import com.studenthub.model.enums.Role;
import com.studenthub.util.LogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.findByUsername: " + username, e);
            throw new AppException("Lỗi truy vấn người dùng theo username.", e);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.findByEmail: " + email, e);
            throw new AppException("Lỗi truy vấn người dùng theo email.", e);
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.findById: " + id, e);
            throw new AppException("Lỗi truy vấn người dùng theo ID.", e);
        }
        return Optional.empty();
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.findAll", e);
            throw new AppException("Lỗi lấy danh sách tài khoản.", e);
        }
        return list;
    }

    public List<User> searchByUsernameOrEmail(String keyword) {
        String sql = "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? ORDER BY id ASC";
        List<User> list = new ArrayList<>();
        String pattern = "%" + keyword.trim() + "%";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.searchByUsernameOrEmail", e);
            throw new AppException("Lỗi tìm kiếm tài khoản.", e);
        }
        return list;
    }

    public int insert(User user) {
        try (Connection conn = DBConnection.getConnection()) {
            return insert(conn, user);
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.insert", e);
            throw new AppException("Lỗi thêm mới tài khoản: " + e.getMessage(), e);
        }
    }

    public int insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, role, is_active, failed_attempts, locked_until) VALUES (?, ?, ?, ?, ?, 0, NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    user.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public void updateRoleAndEmail(int userId, String email, Role role) {
        String sql = "UPDATE users SET email = ?, role = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, role.name());
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.updateRoleAndEmail", e);
            throw new AppException("Lỗi cập nhật thông tin tài khoản.", e);
        }
    }

    public void updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, failed_attempts = 0, locked_until = NULL WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.updatePassword", e);
            throw new AppException("Lỗi cập nhật mật khẩu.", e);
        }
    }

    public void updateLoginFailure(int userId, int failedAttempts, LocalDateTime lockedUntil) {
        String sql = "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, failedAttempts);
            if (lockedUntil != null) {
                ps.setTimestamp(2, Timestamp.valueOf(lockedUntil));
            } else {
                ps.setNull(2, java.sql.Types.TIMESTAMP);
            }
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.updateLoginFailure", e);
            throw new AppException("Lỗi cập nhật trạng thái đăng nhập sai.", e);
        }
    }

    public void resetLoginFailure(int userId) {
        String sql = "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.resetLoginFailure", e);
            throw new AppException("Lỗi reset số lần đăng nhập sai.", e);
        }
    }

    public void updateActiveStatus(int userId, boolean active) {
        String sql = "UPDATE users SET is_active = ?, failed_attempts = 0, locked_until = NULL WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.updateActiveStatus", e);
            throw new AppException("Lỗi khóa/mở khóa tài khoản.", e);
        }
    }

    public void deleteById(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi UserDAO.deleteById", e);
            throw new AppException("Không thể xóa tài khoản (có thể đang liên kết dữ liệu khác).", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        Timestamp lockedTs = rs.getTimestamp("locked_until");
        Timestamp createdTs = rs.getTimestamp("created_at");
        return User.builder()
                .id(rs.getInt("id"))
                .username(rs.getString("username"))
                .email(rs.getString("email"))
                .passwordHash(rs.getString("password_hash"))
                .role(Role.valueOf(rs.getString("role")))
                .active(rs.getBoolean("is_active"))
                .failedAttempts(rs.getInt("failed_attempts"))
                .lockedUntil(lockedTs != null ? lockedTs.toLocalDateTime() : null)
                .createdAt(createdTs != null ? createdTs.toLocalDateTime() : null)
                .build();
    }
}
