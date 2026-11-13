package com.studenthub.util;

import com.studenthub.exception.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class Validator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)[0-9]{8,10}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,20}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Validator() {
    }

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " không được để trống.");
        }
    }

    public static void validateUsername(String username) {
        requireNonBlank(username, "Username");
        if (!USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new ValidationException("Username chỉ gồm chữ cái, số, dấu chấm/gạch dưới/gạch ngang (3-50 ký tự).");
        }
    }

    public static void validatePassword(String password) {
        requireNonBlank(password, "Mật khẩu");
        if (password.length() < 6) {
            throw new ValidationException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
    }

    public static void validateCode(String code, String fieldName) {
        requireNonBlank(code, fieldName);
        if (!CODE_PATTERN.matcher(code.trim()).matches()) {
            throw new ValidationException(fieldName + " không hợp lệ (3-20 ký tự chữ/số, không khoảng trắng).");
        }
    }

    public static void validateEmail(String email) {
        requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new ValidationException("Định dạng email không hợp lệ: " + email);
        }
    }

    public static void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return;
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new ValidationException("Số điện thoại không hợp lệ (bắt đầu bằng 0 hoặc +84, 9-11 chữ số).");
        }
    }

    public static LocalDate parseAndValidateDob(String dobStr) {
        requireNonBlank(dobStr, "Ngày sinh");
        try {
            LocalDate dob = LocalDate.parse(dobStr.trim(), DATE_FORMATTER);
            if (dob.isAfter(LocalDate.now().minusYears(15)) || dob.isBefore(LocalDate.of(1950, 1, 1))) {
                throw new ValidationException("Ngày sinh không hợp lệ (sinh viên phải từ 15 tuổi trở lên).");
            }
            return dob;
        } catch (DateTimeParseException e) {
            throw new ValidationException("Ngày sinh phải đúng định dạng yyyy-MM-dd (VD: 2004-08-25).");
        }
    }

    public static void validateScore(Double score, String scoreName) {
        if (score == null) {
            return;
        }
        if (score < 0.0 || score > 10.0) {
            throw new ValidationException("Điểm " + scoreName + " phải nằm trong khoảng 0.0 đến 10.0.");
        }
    }

    public static void validateCredits(int credits) {
        if (credits <= 0 || credits > 15) {
            throw new ValidationException("Số tín chỉ phải từ 1 đến 15.");
        }
    }
}
