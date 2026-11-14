package com.studenthub.service;

import com.studenthub.config.AppConfig;
import com.studenthub.dao.UserDAO;
import com.studenthub.exception.AuthException;
import com.studenthub.exception.NotFoundException;
import com.studenthub.exception.ValidationException;
import com.studenthub.model.User;
import com.studenthub.util.LogUtil;
import com.studenthub.util.PasswordUtil;
import com.studenthub.util.Validator;

import java.time.Duration;
import java.time.LocalDateTime;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public User login(String username, String password) {
        Validator.requireNonBlank(username, "Username");
        Validator.requireNonBlank(password, "Mật khẩu");

        User user = userDAO.findByUsername(username.trim())
                .orElseThrow(() -> {
                    LogUtil.warn("Đăng nhập thất bại: Không tìm thấy username '" + username + "'");
                    return new AuthException("Tài khoản hoặc mật khẩu không chính xác.");
                });

        if (!user.isActive()) {
            LogUtil.warn("Tài khoản bị khóa vĩnh viễn cố gắng đăng nhập: " + username);
            throw new AuthException("Tài khoản của bạn đã bị Quản trị viên khóa. Vui lòng liên hệ Admin.");
        }

        if (user.isTemporarilyLocked()) {
            long minutesLeft = Math.max(1, Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1);
            LogUtil.warn("Tài khoản đang trong thời gian khóa tạm thời đăng nhập: " + username);
            throw new AuthException("Tài khoản đang bị tạm khóa do nhập sai quá 5 lần. Vui lòng thử lại sau " + minutesLeft + " phút.");
        }

        boolean matched = PasswordUtil.checkPassword(password, user.getPasswordHash());
        if (!matched) {
            int maxAttempts = AppConfig.getMaxFailedAttempts();
            int lockMinutes = AppConfig.getLockDurationMinutes();
            int newAttempts = user.getFailedAttempts() + 1;

            if (newAttempts >= maxAttempts) {
                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
                userDAO.updateLoginFailure(user.getId(), newAttempts, lockedUntil);
                LogUtil.warn("Tài khoản '" + username + "' bị khóa " + lockMinutes + " phút do nhập sai mật khẩu " + newAttempts + " lần.");
                throw new AuthException("Bạn đã nhập sai mật khẩu " + maxAttempts + " lần! Tài khoản bị khóa trong " + lockMinutes + " phút.");
            } else {
                userDAO.updateLoginFailure(user.getId(), newAttempts, null);
                int remaining = maxAttempts - newAttempts;
                LogUtil.warn("Đăng nhập sai mật khẩu cho user '" + username + "' (Lần " + newAttempts + "/" + maxAttempts + ")");
                throw new AuthException("Mật khẩu không chính xác! Bạn còn " + remaining + " lần thử trước khi bị khóa 15 phút.");
            }
        }

        if (user.getFailedAttempts() > 0 || user.getLockedUntil() != null) {
            userDAO.resetLoginFailure(user.getId());
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        }

        LogUtil.info("Đăng nhập thành công: username=" + user.getUsername() + ", role=" + user.getRole());
        return user;
    }

    public void changePassword(int userId, String oldPassword, String newPassword, String confirmPassword) {
        Validator.requireNonBlank(oldPassword, "Mật khẩu hiện tại");
        Validator.validatePassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Mật khẩu xác nhận không khớp với mật khẩu mới.");
        }

        User user = userDAO.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản người dùng."));

        if (!PasswordUtil.checkPassword(oldPassword, user.getPasswordHash())) {
            throw new AuthException("Mật khẩu hiện tại không chính xác.");
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        userDAO.updatePassword(userId, newHash);
        LogUtil.info("Người dùng '" + user.getUsername() + "' đã đổi mật khẩu thành công.");
    }
}
