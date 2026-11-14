package com.studenthub.ui;

import com.studenthub.config.AppConfig;
import com.studenthub.dto.StudentGpaDTO;
import com.studenthub.dto.SubjectPassRateDTO;
import com.studenthub.exception.AppException;
import com.studenthub.model.ClassSubject;
import com.studenthub.model.SchoolClass;
import com.studenthub.model.Student;
import com.studenthub.model.Subject;
import com.studenthub.model.Teacher;
import com.studenthub.model.User;
import com.studenthub.model.enums.Gender;
import com.studenthub.model.enums.Role;
import com.studenthub.model.enums.StudentStatus;
import com.studenthub.service.AuthService;
import com.studenthub.service.ClassService;
import com.studenthub.service.ReportService;
import com.studenthub.service.ScoreService;
import com.studenthub.service.StudentService;
import com.studenthub.service.SubjectService;
import com.studenthub.service.TeacherService;
import com.studenthub.service.UserService;
import com.studenthub.util.ConsoleUtil;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AdminMenu {
    private final User currentUser;
    private final AuthService authService = new AuthService();
    private final UserService userService = new UserService();
    private final TeacherService teacherService = new TeacherService();
    private final StudentService studentService = new StudentService();
    private final ClassService classService = new ClassService();
    private final SubjectService subjectService = new SubjectService();
    private final ScoreService scoreService = new ScoreService();
    private final ReportService reportService = new ReportService();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AdminMenu(User currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        String[] options = {
                "Quản lý Tài khoản (Users)",
                "Quản lý Giảng viên (Teachers)",
                "Quản lý Sinh viên (Students - Phân trang, Tìm kiếm, CSV)",
                "Quản lý Lớp học (Classes)",
                "Quản lý Môn học & Phân công giảng dạy (Subjects)",
                "Báo cáo & Thống kê toàn trường (Reports & CSV)",
                "Đổi mật khẩu cá nhân"
        };

        while (true) {
            ConsoleUtil.printMenu("ADMIN DASHBOARD — Xin chào: " + currentUser.getUsername(), options);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, options.length);
            try {
                switch (choice) {
                    case 1 -> manageUsersMenu();
                    case 2 -> manageTeachersMenu();
                    case 3 -> manageStudentsMenu();
                    case 4 -> manageClassesMenu();
                    case 5 -> manageSubjectsMenu();
                    case 6 -> manageReportsMenu();
                    case 7 -> changeOwnPassword();
                    case 0 -> {
                        ConsoleUtil.printInfo("Đã đăng xuất khỏi tài khoản ADMIN.");
                        return;
                    }
                }
            } catch (AppException e) {
                ConsoleUtil.printError(e.getMessage());
            } catch (Exception e) {
                ConsoleUtil.printError("Lỗi hệ thống: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // B. QUẢN LÝ TÀI KHOẢN (USERS)
    // =========================================================================
    private void manageUsersMenu() {
        String[] subOptions = {
                "Xem danh sách toàn bộ tài khoản",
                "Tìm kiếm tài khoản theo Username / Email",
                "Thêm tài khoản mới",
                "Cập nhật Email & Gán Role",
                "Reset mật khẩu cho tài khoản",
                "Khóa / Mở khóa tài khoản",
                "Xóa tài khoản"
        };
        while (true) {
            ConsoleUtil.printMenu("QUẢN LÝ TÀI KHOẢN NGƯỜI DÙNG", subOptions);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, subOptions.length);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> printUserList(userService.getAllUsers());
                    case 2 -> {
                        String kw = ConsoleUtil.readString("Nhập từ khóa (username hoặc email)", true);
                        printUserList(userService.searchUsers(kw));
                    }
                    case 3 -> {
                        String username = ConsoleUtil.readString("Nhập Username", true);
                        String email = ConsoleUtil.readString("Nhập Email", true);
                        String pass = ConsoleUtil.readString("Nhập Mật khẩu (tối thiểu 6 ký tự)", true);
                        Role role = Role.fromString(ConsoleUtil.readString("Nhập Role (ADMIN / TEACHER / STUDENT)", true));
                        userService.createUser(username, email, pass, role);
                        ConsoleUtil.printSuccess("Tạo tài khoản '" + username + "' thành công!");
                    }
                    case 4 -> {
                        printUserList(userService.getAllUsers());
                        int id = ConsoleUtil.readInt("Nhập ID tài khoản cần sửa", 1, Integer.MAX_VALUE);
                        User u = userService.getById(id);
                        String newEmail = ConsoleUtil.readStringWithDefault("Email mới", u.getEmail());
                        String roleStr = ConsoleUtil.readStringWithDefault("Role mới (ADMIN/TEACHER/STUDENT)", u.getRole().name());
                        userService.updateUser(id, newEmail, Role.fromString(roleStr));
                        ConsoleUtil.printSuccess("Đã cập nhật tài khoản thành công!");
                    }
                    case 5 -> {
                        int id = ConsoleUtil.readInt("Nhập ID tài khoản cần reset mật khẩu", 1, Integer.MAX_VALUE);
                        String newPass = ConsoleUtil.readStringWithDefault("Mật khẩu mới", "123456");
                        userService.resetPassword(id, newPass);
                        ConsoleUtil.printSuccess("Đã reset mật khẩu thành công!");
                    }
                    case 6 -> {
                        int id = ConsoleUtil.readInt("Nhập ID tài khoản cần Khóa/Mở khóa", 1, Integer.MAX_VALUE);
                        User u = userService.getById(id);
                        boolean targetState = !u.isActive();
                        userService.toggleLockUser(id, targetState, currentUser.getId());
                        ConsoleUtil.printSuccess("Đã " + (targetState ? "MỞ KHÓA" : "KHÓA") + " tài khoản '" + u.getUsername() + "'.");
                    }
                    case 7 -> {
                        int id = ConsoleUtil.readInt("Nhập ID tài khoản cần xóa", 1, Integer.MAX_VALUE);
                        if (ConsoleUtil.confirm("Bạn có chắc chắn muốn xóa tài khoản ID = " + id + "?")) {
                            userService.deleteUser(id, currentUser.getId());
                            ConsoleUtil.printSuccess("Đã xóa tài khoản.");
                        }
                    }
                }
            } catch (Exception e) {
                ConsoleUtil.printError(e.getMessage());
            }
            ConsoleUtil.pause();
        }
    }

    private void printUserList(List<User> users) {
        String[] headers = {"ID", "Username", "Email", "Role", "Trạng thái", "Sai MK", "Tạm khóa đến", "Ngày tạo"};
        List<String[]> rows = new ArrayList<>();
        for (User u : users) {
            String status = u.isActive() ? (u.isTemporarilyLocked() ? "Tạm khóa 15p" : "Hoạt động") : "Đã khóa";
            rows.add(new String[]{
                    String.valueOf(u.getId()),
                    u.getUsername(),
                    u.getEmail(),
                    u.getRole().name(),
                    status,
                    String.valueOf(u.getFailedAttempts()),
                    u.getLockedUntil() != null ? u.getLockedUntil().format(DT_FMT) : "-",
                    u.getCreatedAt() != null ? u.getCreatedAt().format(DT_FMT) : "-"
            });
        }
        ConsoleUtil.printTable(headers, rows);
    }

    // =========================================================================
    // QUẢN LÝ GIẢNG VIÊN (TEACHERS)
    // =========================================================================
    private void manageTeachersMenu() {
        String[] subOptions = {
                "Xem danh sách giảng viên",
                "Thêm giảng viên mới (Tự động tạo tài khoản TEACHER)",
                "Cập nhật thông tin giảng viên"
        };
        while (true) {
            ConsoleUtil.printMenu("QUẢN LÝ GIẢNG VIÊN", subOptions);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, subOptions.length);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> printTeacherList(teacherService.getAllTeachers());
                    case 2 -> {
                        String code = ConsoleUtil.readString("Mã giảng viên (VD: GV003)", true);
                        String name = ConsoleUtil.readString("Họ và tên giảng viên", true);
                        String email = ConsoleUtil.readString("Email", true);
                        String phone = ConsoleUtil.readString("Số điện thoại", false);
                        String dept = ConsoleUtil.readString("Khoa / Bộ môn", true);
                        String pass = ConsoleUtil.readStringWithDefault("Mật khẩu mặc định", "123456");
                        Teacher t = teacherService.createTeacherWithAccount(code, name, email, phone, dept, pass);
                        ConsoleUtil.printSuccess("Đã tạo giảng viên " + t.getTeacherCode() + " (Tài khoản đăng nhập: " + t.getUsername() + ")");
                    }
                    case 3 -> {
                        printTeacherList(teacherService.getAllTeachers());
                        int id = ConsoleUtil.readInt("Nhập ID giảng viên cần sửa", 1, Integer.MAX_VALUE);
                        Teacher t = teacherService.getById(id);
                        String name = ConsoleUtil.readStringWithDefault("Họ tên", t.getFullName());
                        String email = ConsoleUtil.readStringWithDefault("Email", t.getEmail());
                        String phone = ConsoleUtil.readStringWithDefault("Số điện thoại", t.getPhone());
                        String dept = ConsoleUtil.readStringWithDefault("Khoa / Bộ môn", t.getDepartment());
                        teacherService.updateTeacher(id, name, email, phone, dept);
                        ConsoleUtil.printSuccess("Đã cập nhật thông tin giảng viên!");
                    }
                }
            } catch (Exception e) {
                ConsoleUtil.printError(e.getMessage());
            }
            ConsoleUtil.pause();
        }
    }

    private void printTeacherList(List<Teacher> teachers) {
        String[] headers = {"ID", "Mã GV", "Họ và Tên", "Email", "SĐT", "Khoa / Bộ môn", "Tài khoản"};
        List<String[]> rows = new ArrayList<>();
        for (Teacher t : teachers) {
            rows.add(new String[]{
                    String.valueOf(t.getId()),
                    t.getTeacherCode(),
                    t.getFullName(),
                    t.getEmail(),
                    t.getPhone(),
                    t.getDepartment(),
                    t.getUsername() != null ? t.getUsername() : "-"
            });
        }
        ConsoleUtil.printTable(headers, rows);
    }

    // =========================================================================
    // C. QUẢN LÝ SINH VIÊN (STUDENTS)
    // =========================================================================
    private void manageStudentsMenu() {
        String[] subOptions = {
                "Xem / Tìm kiếm / Sắp xếp / Phân trang Sinh viên (10 SV/trang)",
                "Thêm sinh viên mới (Tự động tạo tài khoản STUDENT)",
                "Cập nhật thông tin sinh viên",
                "Xóa mềm (Soft Delete) sinh viên",
                "Chuyển lớp cho sinh viên",
                "Import sinh viên từ file CSV",
                "Xuất danh sách sinh viên ra file CSV"
        };
        while (true) {
            ConsoleUtil.printMenu("QUẢN LÝ SINH VIÊN", subOptions);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, subOptions.length);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> browseStudentsInteractive();
                    case 2 -> createStudentUI();
                    case 3 -> updateStudentUI();
                    case 4 -> softDeleteStudentUI();
                    case 5 -> transferStudentClassUI();
                    case 6 -> {
                        ConsoleUtil.printInfo("Định dạng file CSV: student_code,full_name,dob(yyyy-MM-dd),gender(MALE/FEMALE/OTHER),email,phone,address,class_code");
                        String path = ConsoleUtil.readString("Nhập đường dẫn file CSV cần Import", true);
                        String result = studentService.importStudentsFromCsv(path);
                        ConsoleUtil.printSuccess(result);
                        ConsoleUtil.pause();
                    }
                    case 7 -> {
                        String fileName = ConsoleUtil.readStringWithDefault("Nhập tên file CSV xuất ra", "students_export.csv");
                        String savedPath = studentService.exportStudentsToCsv(fileName);
                        ConsoleUtil.printSuccess("Đã xuất danh sách sinh viên tại: " + savedPath);
                        ConsoleUtil.pause();
                    }
                }
            } catch (Exception e) {
                ConsoleUtil.printError(e.getMessage());
                ConsoleUtil.pause();
            }
        }
    }

    private void browseStudentsInteractive() {
        String keyword = "";
        String sortBy = "code"; // code | name | gpa
        boolean asc = true;
        int page = 1;
        int pageSize = AppConfig.getPageSize();

        while (true) {
            int total = studentService.countSearch(keyword);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
            if (page > totalPages) page = totalPages;

            List<Student> list = studentService.searchAndPaginate(keyword, sortBy, asc, page, pageSize);

            ConsoleUtil.printSubHeader(String.format(
                    "DANH SÁCH SINH VIÊN | Từ khóa: '%s' | Sắp xếp: %s (%s) | Trang %d/%d (Tổng: %d SV)",
                    keyword.isEmpty() ? "Tất cả" : keyword, sortBy.toUpperCase(), asc ? "Tăng dần" : "Giảm dần", page, totalPages, total));
            printStudentTable(list);

            System.out.println("  [N] Trang sau | [P] Trang trước | [S] Tìm kiếm | [O] Đổi sắp xếp | [R] Reset bộ lọc | [0] Quay lại");
            String cmd = ConsoleUtil.readString("Lựa chọn điều hướng", true).toUpperCase();
            switch (cmd) {
                case "N" -> {
                    if (page < totalPages) page++;
                    else ConsoleUtil.printWarning("Đang ở trang cuối cùng!");
                }
                case "P" -> {
                    if (page > 1) page--;
                    else ConsoleUtil.printWarning("Đang ở trang đầu tiên!");
                }
                case "S" -> {
                    keyword = ConsoleUtil.readString("Nhập mã SV / họ tên / mã lớp cần tìm (Enter để xem tất cả)", false);
                    page = 1;
                }
                case "O" -> {
                    sortBy = ConsoleUtil.readStringWithDefault("Sắp xếp theo (code / name / gpa)", "code");
                    String dir = ConsoleUtil.readStringWithDefault("Thứ tự (asc / desc)", sortBy.equalsIgnoreCase("gpa") ? "desc" : "asc");
                    asc = !dir.equalsIgnoreCase("desc");
                    page = 1;
                }
                case "R" -> {
                    keyword = "";
                    sortBy = "code";
                    asc = true;
                    page = 1;
                }
                case "0" -> {
                    return;
                }
                default -> ConsoleUtil.printWarning("Lệnh không hợp lệ!");
            }
        }
    }

    private void createStudentUI() {
        String code = ConsoleUtil.readString("Mã sinh viên (VD: SV004)", true);
        String name = ConsoleUtil.readString("Họ và tên", true);
        String dob = ConsoleUtil.readString("Ngày sinh (yyyy-MM-dd, VD: 2004-05-20)", true);
        Gender gender = Gender.fromString(ConsoleUtil.readStringWithDefault("Giới tính (MALE / FEMALE / OTHER)", "MALE"));
        String email = ConsoleUtil.readString("Email", true);
        String phone = ConsoleUtil.readString("Số điện thoại", false);
        String address = ConsoleUtil.readString("Địa chỉ", false);

        printClassList(classService.getAllClasses());
        Integer classId = ConsoleUtil.readOptionalInt("Nhập ID lớp học");
        String pass = ConsoleUtil.readStringWithDefault("Mật khẩu tài khoản mặc định", "123456");

        Student s = studentService.createStudent(code, name, dob, gender, email, phone, address, classId, pass);
        ConsoleUtil.printSuccess("Đã thêm sinh viên " + s.getStudentCode() + " (Tài khoản đăng nhập: " + s.getStudentCode() + ")");
        ConsoleUtil.pause();
    }

    private void updateStudentUI() {
        int id = ConsoleUtil.readInt("Nhập ID sinh viên cần cập nhật", 1, Integer.MAX_VALUE);
        Student s = studentService.getById(id);
        ConsoleUtil.printInfo("Đang sửa SV: " + s.getStudentCode() + " - " + s.getFullName());

        String name = ConsoleUtil.readStringWithDefault("Họ và tên", s.getFullName());
        String dob = ConsoleUtil.readStringWithDefault("Ngày sinh (yyyy-MM-dd)", s.getDob().toString());
        String genderStr = ConsoleUtil.readStringWithDefault("Giới tính (MALE/FEMALE/OTHER)", s.getGender().name());
        String email = ConsoleUtil.readStringWithDefault("Email", s.getEmail());
        String phone = ConsoleUtil.readStringWithDefault("Số điện thoại", s.getPhone());
        String address = ConsoleUtil.readStringWithDefault("Địa chỉ", s.getAddress());
        String classIdStr = ConsoleUtil.readStringWithDefault("ID lớp học", s.getClassId() != null ? s.getClassId().toString() : "");
        Integer classId = classIdStr.isBlank() ? null : Integer.parseInt(classIdStr);
        String statusStr = ConsoleUtil.readStringWithDefault("Trạng thái (ACTIVE/INACTIVE/GRADUATED)", s.getStatus().name());

        studentService.updateStudent(id, name, dob, Gender.fromString(genderStr), email, phone, address, classId, StudentStatus.valueOf(statusStr.toUpperCase()));
        ConsoleUtil.printSuccess("Cập nhật sinh viên thành công!");
        ConsoleUtil.pause();
    }

    private void softDeleteStudentUI() {
        int id = ConsoleUtil.readInt("Nhập ID sinh viên cần xóa mềm", 1, Integer.MAX_VALUE);
        Student s = studentService.getById(id);
        if (ConsoleUtil.confirm("Xác nhận xóa mềm sinh viên " + s.getStudentCode() + " - " + s.getFullName() + "?")) {
            studentService.softDeleteStudent(id);
            ConsoleUtil.printSuccess("Đã xóa mềm sinh viên và vô hiệu hóa tài khoản đăng nhập.");
        }
        ConsoleUtil.pause();
    }

    private void transferStudentClassUI() {
        int studentId = ConsoleUtil.readInt("Nhập ID sinh viên cần chuyển lớp", 1, Integer.MAX_VALUE);
        Student s = studentService.getById(studentId);
        ConsoleUtil.printInfo("Sinh viên: " + s.getFullName() + " | Lớp hiện tại: " + (s.getClassCode() != null ? s.getClassCode() : "Chưa xếp lớp"));
        printClassList(classService.getAllClasses());
        int newClassId = ConsoleUtil.readInt("Nhập ID lớp mới muốn chuyển đến", 1, Integer.MAX_VALUE);
        studentService.transferClass(studentId, newClassId);
        ConsoleUtil.printSuccess("Đã chuyển lớp thành công!");
        ConsoleUtil.pause();
    }

    private void printStudentTable(List<Student> students) {
        String[] headers = {"ID", "Mã SV", "Họ và Tên", "Ngày sinh", "Giới tính", "Email", "SĐT", "Lớp", "GPA (10)", "GPA (4)"};
        List<String[]> rows = new ArrayList<>();
        for (Student s : students) {
            rows.add(new String[]{
                    String.valueOf(s.getId()),
                    s.getStudentCode(),
                    s.getFullName(),
                    s.getDob() != null ? s.getDob().toString() : "-",
                    s.getGender().getVietnameseLabel(),
                    s.getEmail(),
                    s.getPhone(),
                    s.getClassCode() != null ? s.getClassCode() : "Chưa xếp",
                    s.getGpa10() != null ? String.format("%.2f", s.getGpa10()) : "-",
                    s.getGpa4() != null ? String.format("%.2f", s.getGpa4()) : "-"
            });
        }
        ConsoleUtil.printTable(headers, rows);
    }

    // =========================================================================
    // D. QUẢN LÝ LỚP HỌC (CLASSES)
    // =========================================================================
    private void manageClassesMenu() {
        String[] subOptions = {
                "Xem danh sách tất cả lớp học",
                "Thêm lớp học mới",
                "Cập nhật lớp học & Gán GV chủ nhiệm",
                "Xóa lớp học",
                "Xem danh sách sinh viên theo lớp"
        };
        while (true) {
            ConsoleUtil.printMenu("QUẢN LÝ LỚP HỌC", subOptions);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, subOptions.length);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> printClassList(classService.getAllClasses());
                    case 2 -> {
                        String code = ConsoleUtil.readString("Mã lớp (VD: DHKTPM19A)", true);
                        String name = ConsoleUtil.readString("Tên lớp", true);
                        String year = ConsoleUtil.readString("Niên khóa (VD: 2023-2027)", true);
                        printTeacherList(teacherService.getAllTeachers());
                        Integer homeroomId = ConsoleUtil.readOptionalInt("Nhập ID giảng viên chủ nhiệm");
                        classService.createClass(code, name, year, homeroomId);
                        ConsoleUtil.printSuccess("Thêm lớp học thành công!");
                    }
                    case 3 -> {
                        printClassList(classService.getAllClasses());
                        int id = ConsoleUtil.readInt("Nhập ID lớp cần cập nhật", 1, Integer.MAX_VALUE);
                        SchoolClass sc = classService.getById(id);
                        String code = ConsoleUtil.readStringWithDefault("Mã lớp", sc.getClassCode());
                        String name = ConsoleUtil.readStringWithDefault("Tên lớp", sc.getName());
                        String year = ConsoleUtil.readStringWithDefault("Niên khóa", sc.getAcademicYear());
                        printTeacherList(teacherService.getAllTeachers());
                        Integer homeroomId = ConsoleUtil.readOptionalInt("Nhập ID GV chủ nhiệm mới");
                        classService.updateClass(id, code, name, year, homeroomId != null ? homeroomId : sc.getHomeroomTeacherId());
                        ConsoleUtil.printSuccess("Cập nhật lớp học thành công!");
                    }
                    case 4 -> {
                        printClassList(classService.getAllClasses());
                        int id = ConsoleUtil.readInt("Nhập ID lớp cần xóa", 1, Integer.MAX_VALUE);
                        if (ConsoleUtil.confirm("Xác nhận xóa lớp ID = " + id + "?")) {
                            classService.deleteClass(id);
                            ConsoleUtil.printSuccess("Đã xóa lớp học.");
                        }
                    }
                    case 5 -> {
                        printClassList(classService.getAllClasses());
                        int classId = ConsoleUtil.readInt("Nhập ID lớp muốn xem danh sách SV", 1, Integer.MAX_VALUE);
                        SchoolClass sc = classService.getById(classId);
                        ConsoleUtil.printSubHeader("DANH SÁCH SINH VIÊN LỚP " + sc.getClassCode() + " - " + sc.getName());
                        printStudentTable(studentService.getStudentsByClass(classId));
                    }
                }
            } catch (Exception e) {
                ConsoleUtil.printError(e.getMessage());
            }
            ConsoleUtil.pause();
        }
    }

    private void printClassList(List<SchoolClass> classes) {
        String[] headers = {"ID", "Mã Lớp", "Tên Lớp", "Niên khóa", "GV Chủ nhiệm", "Sĩ số SV"};
        List<String[]> rows = new ArrayList<>();
        for (SchoolClass c : classes) {
            rows.add(new String[]{
                    String.valueOf(c.getId()),
                    c.getClassCode(),
                    c.getName(),
                    c.getAcademicYear(),
                    c.getHomeroomTeacherName() != null ? c.getHomeroomTeacherName() : "Chưa phân công",
                    String.valueOf(c.getStudentCount())
            });
        }
        ConsoleUtil.printTable(headers, rows);
    }

    // =========================================================================
    // E. QUẢN LÝ MÔN HỌC & PHÂN CÔNG GIẢNG DẠY (SUBJECTS & CLASS_SUBJECT)
    // =========================================================================
    private void manageSubjectsMenu() {
        String[] subOptions = {
                "Xem danh sách môn học",
                "Thêm môn học mới",
                "Cập nhật môn học",
                "Xóa môn học",
                "Xem danh sách Phân công giảng dạy (Lớp học phần)",
                "Gán Giảng viên dạy Môn học cho Lớp theo Học kỳ",
                "Mở khóa sổ điểm học phần (Hỗ trợ Giảng viên)"
        };
        while (true) {
            ConsoleUtil.printMenu("QUẢN LÝ MÔN HỌC & PHÂN CÔNG GIẢNG DẠY", subOptions);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, subOptions.length);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> printSubjectList(subjectService.getAllSubjects());
                    case 2 -> {
                        String code = ConsoleUtil.readString("Mã môn học (VD: WEB01)", true);
                        String name = ConsoleUtil.readString("Tên môn học", true);
                        int credits = ConsoleUtil.readInt("Số tín chỉ (1-15)", 1, 15);
                        String dept = ConsoleUtil.readString("Khoa phụ trách", true);
                        subjectService.createSubject(code, name, credits, dept);
                        ConsoleUtil.printSuccess("Thêm môn học thành công!");
                    }
                    case 3 -> {
                        printSubjectList(subjectService.getAllSubjects());
                        int id = ConsoleUtil.readInt("Nhập ID môn học cần sửa", 1, Integer.MAX_VALUE);
                        Subject sub = subjectService.getById(id);
                        String code = ConsoleUtil.readStringWithDefault("Mã môn", sub.getSubjectCode());
                        String name = ConsoleUtil.readStringWithDefault("Tên môn", sub.getName());
                        int credits = Integer.parseInt(ConsoleUtil.readStringWithDefault("Số tín chỉ", String.valueOf(sub.getCredits())));
                        String dept = ConsoleUtil.readStringWithDefault("Khoa phụ trách", sub.getDepartment());
                        subjectService.updateSubject(id, code, name, credits, dept);
                        ConsoleUtil.printSuccess("Cập nhật môn học thành công!");
                    }
                    case 4 -> {
                        printSubjectList(subjectService.getAllSubjects());
                        int id = ConsoleUtil.readInt("Nhập ID môn học cần xóa", 1, Integer.MAX_VALUE);
                        if (ConsoleUtil.confirm("Xác nhận xóa môn học ID = " + id + "?")) {
                            subjectService.deleteSubject(id);
                            ConsoleUtil.printSuccess("Đã xóa môn học.");
                        }
                    }
                    case 5 -> printClassSubjectList(subjectService.getAllClassSubjects());
                    case 6 -> {
                        printClassList(classService.getAllClasses());
                        int classId = ConsoleUtil.readInt("Chọn ID Lớp học", 1, Integer.MAX_VALUE);
                        printSubjectList(subjectService.getAllSubjects());
                        int subjectId = ConsoleUtil.readInt("Chọn ID Môn học", 1, Integer.MAX_VALUE);
                        printTeacherList(teacherService.getAllTeachers());
                        int teacherId = ConsoleUtil.readInt("Chọn ID Giảng viên giảng dạy", 1, Integer.MAX_VALUE);
                        String semester = ConsoleUtil.readStringWithDefault("Học kỳ (VD: 2026-1, 2026-2)", "2026-1");
                        subjectService.assignTeacherToSubject(classId, subjectId, teacherId, semester);
                        ConsoleUtil.printSuccess("Đã phân công giảng viên dạy môn học thành công!");
                    }
                    case 7 -> {
                        printClassSubjectList(subjectService.getAllClassSubjects());
                        int csId = ConsoleUtil.readInt("Nhập ID Lớp học phần cần mở khóa sổ điểm", 1, Integer.MAX_VALUE);
                        scoreService.unlockCourseScoresByAdmin(csId);
                        ConsoleUtil.printSuccess("Đã mở khóa sổ điểm học phần!");
                    }
                }
            } catch (Exception e) {
                ConsoleUtil.printError(e.getMessage());
            }
            ConsoleUtil.pause();
        }
    }

    private void printSubjectList(List<Subject> subjects) {
        String[] headers = {"ID", "Mã Môn", "Tên Môn Học", "Số Tín Chỉ", "Khoa Phụ Trách"};
        List<String[]> rows = new ArrayList<>();
        for (Subject s : subjects) {
            rows.add(new String[]{
                    String.valueOf(s.getId()),
                    s.getSubjectCode(),
                    s.getName(),
                    String.valueOf(s.getCredits()),
                    s.getDepartment()
            });
        }
        ConsoleUtil.printTable(headers, rows);
    }

    private void printClassSubjectList(List<ClassSubject> list) {
        String[] headers = {"ID Học phần", "Học kỳ", "Mã Lớp", "Mã Môn", "Tên Môn Học", "Số TC", "Giảng viên dạy", "Trạng thái sổ"};
        List<String[]> rows = new ArrayList<>();
        for (ClassSubject cs : list) {
            rows.add(new String[]{
                    String.valueOf(cs.getId()),
                    cs.getSemester(),
                    cs.getClassCode(),
                    cs.getSubjectCode(),
                    cs.getSubjectName(),
                    String.valueOf(cs.getCredits()),
                    cs.getTeacherCode() + " - " + cs.getTeacherName(),
                    cs.isLocked() ? "ĐÃ KHÓA SỔ" : "Đang mở"
            });
        }
        ConsoleUtil.printTable(headers, rows);
    }

    // =========================================================================
    // I. BÁO CÁO & THỐNG KÊ TOÀN TRƯỜNG (ADMIN)
    // =========================================================================
    private void manageReportsMenu() {
        String[] subOptions = {
                "Thống kê số lượng SV theo Lớp & theo Giới tính",
                "Top 10 Sinh viên có GPA cao nhất toàn trường",
                "Thống kê tỉ lệ Đạt / Rớt theo từng Môn học",
                "Xuất tất cả báo cáo thống kê ra file CSV"
        };
        while (true) {
            ConsoleUtil.printMenu("BÁO CÁO & THỐNG KÊ TOÀN TRƯỜNG", subOptions);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, subOptions.length);
            if (choice == 0) return;
            try {
                switch (choice) {
                    case 1 -> {
                        ConsoleUtil.printSubHeader("1. THỐNG KÊ SỐ LƯỢNG SINH VIÊN THEO LỚP");
                        String[] classHeaders = {"Mã Lớp", "Tên Lớp", "Niên khóa", "GV Chủ nhiệm", "Tổng SV", "SV Nam", "SV Nữ"};
                        ConsoleUtil.printTable(classHeaders, reportService.getStudentCountByClass());

                        ConsoleUtil.printSubHeader("2. THỐNG KÊ SINH VIÊN THEO GIỚI TÍNH");
                        String[] genderHeaders = {"Mã", "Giới tính", "Số lượng SV", "Tỉ lệ (%)"};
                        ConsoleUtil.printTable(genderHeaders, reportService.getStudentCountByGender());
                    }
                    case 2 -> {
                        ConsoleUtil.printSubHeader("TOP 10 SINH VIÊN GPA CAO NHẤT TOÀN TRƯỜNG");
                        List<StudentGpaDTO> top10 = reportService.getTop10GpaSchoolWide();
                        String[] headers = {"Hạng", "Mã SV", "Họ và Tên", "Lớp", "Giới tính", "Tín chỉ tích lũy", "GPA (Hệ 10)", "GPA (Hệ 4)", "Xếp loại"};
                        List<String[]> rows = new ArrayList<>();
                        for (int i = 0; i < top10.size(); i++) {
                            StudentGpaDTO d = top10.get(i);
                            rows.add(new String[]{
                                    "#" + (i + 1),
                                    d.getStudentCode(),
                                    d.getFullName(),
                                    d.getClassCode(),
                                    d.getGender(),
                                    String.valueOf(d.getTotalCredits()),
                                    String.format("%.2f", d.getGpa10()),
                                    String.format("%.2f", d.getGpa4()),
                                    d.getAcademicRank()
                            });
                        }
                        ConsoleUtil.printTable(headers, rows);
                    }
                    case 3 -> {
                        ConsoleUtil.printSubHeader("THỐNG KÊ TỈ LỆ ĐẠT / RỚT THEO MÔN HỌC");
                        List<SubjectPassRateDTO> stats = reportService.getPassRateBySubject();
                        String[] headers = {"Mã Môn", "Tên Môn Học", "Số TC", "Số SV Đã chấm", "Số SV Đạt", "Số SV Rớt", "Tỉ lệ Đạt", "Điểm TB"};
                        List<String[]> rows = new ArrayList<>();
                        for (SubjectPassRateDTO d : stats) {
                            rows.add(new String[]{
                                    d.getSubjectCode(),
                                    d.getSubjectName(),
                                    String.valueOf(d.getCredits()),
                                    String.valueOf(d.getTotalGradedStudents()),
                                    String.valueOf(d.getPassedCount()),
                                    String.valueOf(d.getFailedCount()),
                                    String.format("%.2f%%", d.getPassRatePercent()),
                                    String.format("%.2f", d.getAverageScore())
                            });
                        }
                        ConsoleUtil.printTable(headers, rows);
                    }
                    case 4 -> {
                        String p1 = reportService.exportClassStatsToCsv("report_class_statistics.csv");
                        String p2 = reportService.exportTop10GpaToCsv("report_top10_gpa.csv");
                        String p3 = reportService.exportSubjectPassRateToCsv("report_subject_pass_rate.csv");
                        ConsoleUtil.printSuccess("Đã xuất thành công 3 file báo cáo CSV:");
                        System.out.println("  1. " + p1);
                        System.out.println("  2. " + p2);
                        System.out.println("  3. " + p3);
                    }
                }
            } catch (Exception e) {
                ConsoleUtil.printError(e.getMessage());
            }
            ConsoleUtil.pause();
        }
    }

    private void changeOwnPassword() {
        ConsoleUtil.printSubHeader("ĐỔI MẬT KHẨU CÁ NHÂN");
        String oldPass = ConsoleUtil.readString("Nhập mật khẩu hiện tại", true);
        String newPass = ConsoleUtil.readString("Nhập mật khẩu mới (tối thiểu 6 ký tự)", true);
        String confirm = ConsoleUtil.readString("Xác nhận mật khẩu mới", true);
        authService.changePassword(currentUser.getId(), oldPass, newPass, confirm);
        ConsoleUtil.printSuccess("Đổi mật khẩu thành công!");
        ConsoleUtil.pause();
    }
}
