package com.studenthub.service;

import com.studenthub.config.DBConnection;
import com.studenthub.dao.ClassDAO;
import com.studenthub.dao.StudentDAO;
import com.studenthub.dao.UserDAO;
import com.studenthub.exception.AppException;
import com.studenthub.exception.DuplicateResourceException;
import com.studenthub.exception.NotFoundException;
import com.studenthub.model.SchoolClass;
import com.studenthub.model.Student;
import com.studenthub.model.User;
import com.studenthub.model.enums.Gender;
import com.studenthub.model.enums.Role;
import com.studenthub.model.enums.StudentStatus;
import com.studenthub.util.CSVUtil;
import com.studenthub.util.LogUtil;
import com.studenthub.util.PasswordUtil;
import com.studenthub.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentService {
    private final StudentDAO studentDAO = new StudentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ClassDAO classDAO = new ClassDAO();

    public int countSearch(String keyword) {
        return studentDAO.countActiveSearch(keyword);
    }

    public List<Student> searchAndPaginate(String keyword, String sortBy, boolean asc, int page, int pageSize) {
        return studentDAO.searchAndPaginate(keyword, sortBy, asc, page, pageSize);
    }

    public List<Student> getStudentsByClass(int classId) {
        return studentDAO.findByClassId(classId);
    }

    public Student getById(int id) {
        return studentDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sinh viên có ID = " + id));
    }

    public Student getByCode(String studentCode) {
        return studentDAO.findByCode(studentCode.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sinh viên có mã: " + studentCode));
    }

    public Student getByUserId(int userId) {
        return studentDAO.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Tài khoản sinh viên này chưa được liên kết với hồ sơ sinh viên."));
    }

    public Student createStudent(String studentCode, String fullName, String dobStr, Gender gender,
                                 String email, String phone, String address, Integer classId, String defaultPassword) {
        Validator.validateCode(studentCode, "Mã sinh viên");
        Validator.requireNonBlank(fullName, "Họ tên sinh viên");
        LocalDate dob = Validator.parseAndValidateDob(dobStr);
        Validator.validateEmail(email);
        Validator.validatePhone(phone);

        String normalizedCode = studentCode.trim().toUpperCase();
        String trimmedEmail = email.trim();

        if (studentDAO.findByCode(normalizedCode).isPresent()) {
            throw new DuplicateResourceException("Mã sinh viên '" + normalizedCode + "' đã tồn tại.");
        }
        if (studentDAO.findByEmail(trimmedEmail).isPresent() || userDAO.findByEmail(trimmedEmail).isPresent()) {
            throw new DuplicateResourceException("Email '" + trimmedEmail + "' đã được sử dụng.");
        }
        if (userDAO.findByUsername(normalizedCode).isPresent()) {
            throw new DuplicateResourceException("Username '" + normalizedCode + "' đã tồn tại.");
        }
        if (classId != null && classDAO.findById(classId).isEmpty()) {
            throw new NotFoundException("Không tìm thấy lớp học có ID = " + classId);
        }

        String rawPass = (defaultPassword == null || defaultPassword.isBlank()) ? "123456" : defaultPassword.trim();
        Validator.validatePassword(rawPass);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                User user = User.builder()
                        .username(normalizedCode)
                        .email(trimmedEmail)
                        .passwordHash(PasswordUtil.hashPassword(rawPass))
                        .role(Role.STUDENT)
                        .active(true)
                        .build();
                int userId = userDAO.insert(conn, user);

                Student student = Student.builder()
                        .studentCode(normalizedCode)
                        .fullName(fullName.trim())
                        .dob(dob)
                        .gender(gender)
                        .email(trimmedEmail)
                        .phone(phone == null ? "" : phone.trim())
                        .address(address == null ? "" : address.trim())
                        .classId(classId)
                        .userId(userId)
                        .status(StudentStatus.ACTIVE)
                        .build();
                studentDAO.insert(conn, student);

                conn.commit();
                LogUtil.info("Thêm sinh viên mới thành công: " + normalizedCode + " - " + fullName);
                return student;
            } catch (SQLException ex) {
                DBConnection.rollbackQuietly(conn);
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi tạo sinh viên mới", e);
            throw new AppException("Không thể thêm sinh viên: " + e.getMessage(), e);
        }
    }

    public void updateStudent(int id, String fullName, String dobStr, Gender gender,
                              String email, String phone, String address, Integer classId, StudentStatus status) {
        Student existing = getById(id);
        Validator.requireNonBlank(fullName, "Họ tên sinh viên");
        LocalDate dob = Validator.parseAndValidateDob(dobStr);
        Validator.validateEmail(email);
        Validator.validatePhone(phone);

        String trimmedEmail = email.trim();
        studentDAO.findByEmail(trimmedEmail).ifPresent(other -> {
            if (other.getId() != id) {
                throw new DuplicateResourceException("Email '" + trimmedEmail + "' đã thuộc về sinh viên khác.");
            }
        });

        if (classId != null && classDAO.findById(classId).isEmpty()) {
            throw new NotFoundException("Không tìm thấy lớp học có ID = " + classId);
        }

        existing.setFullName(fullName.trim());
        existing.setDob(dob);
        existing.setGender(gender);
        existing.setEmail(trimmedEmail);
        existing.setPhone(phone == null ? "" : phone.trim());
        existing.setAddress(address == null ? "" : address.trim());
        existing.setClassId(classId);
        existing.setStatus(status != null ? status : existing.getStatus());

        studentDAO.update(existing);
        LogUtil.info("Cập nhật sinh viên: " + existing.getStudentCode());
    }

    public void transferClass(int studentId, int newClassId) {
        Student existing = getById(studentId);
        SchoolClass targetClass = classDAO.findById(newClassId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học có ID = " + newClassId));
        studentDAO.updateClass(studentId, newClassId);
        LogUtil.info("Chuyển lớp cho SV " + existing.getStudentCode() + " sang lớp " + targetClass.getClassCode());
    }

    public void softDeleteStudent(int id) {
        Student existing = getById(id);
        studentDAO.softDelete(id, existing.getUserId());
        LogUtil.info("Xóa mềm sinh viên: " + existing.getStudentCode());
    }

    public String exportStudentsToCsv(String fileName) {
        List<Student> students = studentDAO.findAllActive();
        String[] headers = {"Mã SV", "Họ và Tên", "Ngày sinh", "Giới tính", "Email", "SĐT", "Địa chỉ", "Mã Lớp", "Trạng thái", "GPA (Hệ 10)", "GPA (Hệ 4)"};
        List<String[]> rows = new ArrayList<>();
        for (Student s : students) {
            rows.add(new String[]{
                    s.getStudentCode(),
                    s.getFullName(),
                    s.getDob() != null ? s.getDob().toString() : "",
                    s.getGender().getVietnameseLabel(),
                    s.getEmail(),
                    s.getPhone(),
                    s.getAddress(),
                    s.getClassCode() != null ? s.getClassCode() : "Chưa xếp lớp",
                    s.getStatus().getLabel(),
                    s.getGpa10() != null ? String.format("%.2f", s.getGpa10()) : "-",
                    s.getGpa4() != null ? String.format("%.2f", s.getGpa4()) : "-"
            });
        }
        return CSVUtil.exportToCsv(fileName, headers, rows);
    }

    /**
     * Import sinh viên từ CSV.
     * Định dạng cột: student_code,full_name,dob(yyyy-MM-dd),gender(MALE/FEMALE/OTHER),email,phone,address,class_code
     */
    public String importStudentsFromCsv(String filePath) {
        List<String[]> lines = CSVUtil.readCsv(filePath, true);
        int success = 0;
        int failed = 0;
        StringBuilder errorReport = new StringBuilder();

        for (int i = 0; i < lines.size(); i++) {
            String[] cols = lines.get(i);
            int rowNum = i + 2;
            if (cols.length < 5) {
                failed++;
                errorReport.append(String.format("  - Dòng %d: Thiếu cột dữ liệu (cần tối thiểu 5 cột).%n", rowNum));
                continue;
            }
            try {
                String code = cols[0].trim();
                String name = cols[1].trim();
                String dob = cols[2].trim();
                Gender gender = Gender.fromString(cols[3]);
                String email = cols[4].trim();
                String phone = cols.length > 5 ? cols[5].trim() : "";
                String address = cols.length > 6 ? cols[6].trim() : "";
                String classCode = cols.length > 7 ? cols[7].trim() : "";

                Integer classId = null;
                if (!classCode.isEmpty()) {
                    Optional<SchoolClass> sc = classDAO.findByCode(classCode.toUpperCase());
                    if (sc.isPresent()) {
                        classId = sc.get().getId();
                    }
                }

                createStudent(code, name, dob, gender, email, phone, address, classId, "123456");
                success++;
            } catch (Exception e) {
                failed++;
                errorReport.append(String.format("  - Dòng %d (%s): %s%n", rowNum, cols[0], e.getMessage()));
            }
        }

        String summary = String.format("Kết quả Import CSV: Thành công %d dòng, Thất bại %d dòng.", success, failed);
        if (failed > 0) {
            summary += "\nChi tiết lỗi:\n" + errorReport;
        }
        LogUtil.info("Import CSV hoàn tất: success=" + success + ", failed=" + failed);
        return summary;
    }
}
