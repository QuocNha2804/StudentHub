package com.studenthub.service;

import com.studenthub.config.DBConnection;
import com.studenthub.dao.TeacherDAO;
import com.studenthub.dao.UserDAO;
import com.studenthub.exception.AppException;
import com.studenthub.exception.DuplicateResourceException;
import com.studenthub.exception.NotFoundException;
import com.studenthub.model.Teacher;
import com.studenthub.model.User;
import com.studenthub.model.enums.Role;
import com.studenthub.util.LogUtil;
import com.studenthub.util.PasswordUtil;
import com.studenthub.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class TeacherService {
    private final TeacherDAO teacherDAO = new TeacherDAO();
    private final UserDAO userDAO = new UserDAO();

    public List<Teacher> getAllTeachers() {
        return teacherDAO.findAll();
    }

    public Teacher getById(int id) {
        return teacherDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên có ID = " + id));
    }

    public Teacher getByUserId(int userId) {
        return teacherDAO.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Tài khoản giảng viên này chưa được liên kết hồ sơ giảng viên."));
    }

    public Teacher createTeacherWithAccount(String teacherCode, String fullName, String email, String phone, String department, String defaultPassword) {
        Validator.validateCode(teacherCode, "Mã giảng viên");
        Validator.requireNonBlank(fullName, "Họ tên giảng viên");
        Validator.validateEmail(email);
        Validator.validatePhone(phone);
        Validator.requireNonBlank(department, "Khoa/Bộ môn");
        Validator.validatePassword(defaultPassword);

        String normalizedCode = teacherCode.trim().toUpperCase();
        String username = normalizedCode.toLowerCase();

        if (teacherDAO.findByCode(normalizedCode).isPresent()) {
            throw new DuplicateResourceException("Mã giảng viên '" + normalizedCode + "' đã tồn tại.");
        }
        if (userDAO.findByUsername(username).isPresent()) {
            throw new DuplicateResourceException("Username '" + username + "' đã tồn tại.");
        }
        if (userDAO.findByEmail(email.trim()).isPresent()) {
            throw new DuplicateResourceException("Email '" + email + "' đã tồn tại.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User user = User.builder()
                        .username(username)
                        .email(email.trim())
                        .passwordHash(PasswordUtil.hashPassword(defaultPassword))
                        .role(Role.TEACHER)
                        .active(true)
                        .build();
                int userId = userDAO.insert(conn, user);

                Teacher teacher = Teacher.builder()
                        .teacherCode(normalizedCode)
                        .fullName(fullName.trim())
                        .email(email.trim())
                        .phone(phone == null ? "" : phone.trim())
                        .department(department.trim())
                        .userId(userId)
                        .username(username)
                        .build();
                teacherDAO.insert(conn, teacher);

                conn.commit();
                LogUtil.info("Đã tạo giảng viên mới kèm tài khoản: " + normalizedCode + " (username=" + username + ")");
                return teacher;
            } catch (SQLException ex) {
                DBConnection.rollbackQuietly(conn);
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi tạo giảng viên kèm tài khoản", e);
            throw new AppException("Không thể thêm giảng viên: " + e.getMessage(), e);
        }
    }

    public void updateTeacher(int id, String fullName, String email, String phone, String department) {
        Teacher existing = getById(id);
        Validator.requireNonBlank(fullName, "Họ tên giảng viên");
        Validator.validateEmail(email);
        Validator.validatePhone(phone);
        Validator.requireNonBlank(department, "Khoa/Bộ môn");

        existing.setFullName(fullName.trim());
        existing.setEmail(email.trim());
        existing.setPhone(phone == null ? "" : phone.trim());
        existing.setDepartment(department.trim());

        teacherDAO.update(existing);
        LogUtil.info("Cập nhật thông tin giảng viên: " + existing.getTeacherCode());
    }
}
