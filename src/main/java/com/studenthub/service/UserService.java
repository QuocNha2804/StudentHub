package com.studenthub.service;

import com.studenthub.dao.UserDAO;
import com.studenthub.exception.DuplicateResourceException;
import com.studenthub.exception.NotFoundException;
import com.studenthub.exception.ValidationException;
import com.studenthub.model.User;
import com.studenthub.model.enums.Role;
import com.studenthub.util.LogUtil;
import com.studenthub.util.PasswordUtil;
import com.studenthub.util.Validator;

import java.util.List;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userDAO.findAll();
        }
        return userDAO.searchByUsernameOrEmail(keyword);
    }

    public User getById(int id) {
        return userDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản có ID = " + id));
    }

    public User createUser(String username, String email, String plainPassword, Role role) {
        Validator.validateUsername(username);
        Validator.validateEmail(email);
        Validator.validatePassword(plainPassword);

        if (userDAO.findByUsername(username.trim()).isPresent()) {
            throw new DuplicateResourceException("Username '" + username + "' đã tồn tại trong hệ thống.");
        }
        if (userDAO.findByEmail(email.trim()).isPresent()) {
            throw new DuplicateResourceException("Email '" + email + "' đã được sử dụng bởi tài khoản khác.");
        }

        User user = User.builder()
                .username(username.trim())
                .email(email.trim())
                .passwordHash(PasswordUtil.hashPassword(plainPassword))
                .role(role)
                .active(true)
                .build();

        userDAO.insert(user);
        LogUtil.info("Admin đã tạo tài khoản mới: " + user.getUsername() + " (" + role + ")");
        return user;
    }

    public void updateUser(int userId, String email, Role newRole) {
        User existing = getById(userId);
        Validator.validateEmail(email);

        userDAO.findByEmail(email.trim()).ifPresent(other -> {
            if (other.getId() != userId) {
                throw new DuplicateResourceException("Email '" + email + "' đã thuộc về tài khoản khác.");
            }
        });

        userDAO.updateRoleAndEmail(userId, email.trim(), newRole);
        LogUtil.info("Admin cập nhật tài khoản ID=" + userId + " (" + existing.getUsername() + "): email=" + email + ", role=" + newRole);
    }

    public void resetPassword(int userId, String newPlainPassword) {
        User existing = getById(userId);
        Validator.validatePassword(newPlainPassword);
        userDAO.updatePassword(userId, PasswordUtil.hashPassword(newPlainPassword));
        LogUtil.info("Admin đã reset mật khẩu cho tài khoản: " + existing.getUsername());
    }

    public void toggleLockUser(int userId, boolean active, int currentAdminId) {
        if (userId == currentAdminId && !active) {
            throw new ValidationException("Bạn không thể tự khóa tài khoản Admin đang đăng nhập!");
        }
        User existing = getById(userId);
        userDAO.updateActiveStatus(userId, active);
        LogUtil.info("Admin đã " + (active ? "mở khóa" : "khóa") + " tài khoản: " + existing.getUsername());
    }

    public void deleteUser(int userId, int currentAdminId) {
        if (userId == currentAdminId) {
            throw new ValidationException("Bạn không thể xóa tài khoản Admin đang đăng nhập!");
        }
        User existing = getById(userId);
        userDAO.deleteById(userId);
        LogUtil.info("Admin đã xóa tài khoản: " + existing.getUsername());
    }
}
