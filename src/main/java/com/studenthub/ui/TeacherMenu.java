package com.studenthub.ui;

import com.studenthub.dto.StudentGpaDTO;
import com.studenthub.exception.AppException;
import com.studenthub.exception.ValidationException;
import com.studenthub.model.ClassSubject;
import com.studenthub.model.SchoolClass;
import com.studenthub.model.Score;
import com.studenthub.model.Student;
import com.studenthub.model.Teacher;
import com.studenthub.model.User;
import com.studenthub.service.AuthService;
import com.studenthub.service.ClassService;
import com.studenthub.service.ReportService;
import com.studenthub.service.ScoreService;
import com.studenthub.service.StudentService;
import com.studenthub.service.SubjectService;
import com.studenthub.service.TeacherService;
import com.studenthub.util.ConsoleUtil;

import java.util.ArrayList;
import java.util.List;

public class TeacherMenu {
    private final User currentUser;
    private final AuthService authService = new AuthService();
    private final TeacherService teacherService = new TeacherService();
    private final SubjectService subjectService = new SubjectService();
    private final ScoreService scoreService = new ScoreService();
    private final ClassService classService = new ClassService();
    private final StudentService studentService = new StudentService();
    private final ReportService reportService = new ReportService();

    public TeacherMenu(User currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        Teacher teacher;
        try {
            teacher = teacherService.getByUserId(currentUser.getId());
        } catch (AppException e) {
            ConsoleUtil.printError(e.getMessage());
            return;
        }

        String[] options = {
                "Xem danh sách Lớp học phần được phân công giảng dạy",
                "Chọn Lớp + Môn → Xem & Nhập / Sửa điểm cho từng Sinh viên",
                "Khóa sổ điểm môn học (Sau khi khóa không thể sửa)",
                "Xuất bảng điểm Lớp học phần ra file CSV",
                "Báo cáo Lớp: Danh sách SV + GPA, Top 5 Cao/Thấp nhất, Tỉ lệ Đạt/Rớt",
                "Đổi mật khẩu cá nhân"
        };

        while (true) {
            ConsoleUtil.printMenu("TEACHER DASHBOARD — GV: " + teacher.getFullName() + " (" + teacher.getTeacherCode() + ")", options);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, options.length);
            try {
                switch (choice) {
                    case 1 -> {
                        printAssignedCourses(teacher.getId());
                        ConsoleUtil.pause();
                    }
                    case 2 -> enterOrUpdateScoresUI(teacher);
                    case 3 -> lockCourseGradebookUI(teacher);
                    case 4 -> exportCourseScoresUI(teacher);
                    case 5 -> viewClassReportMenu(teacher);
                    case 6 -> changeOwnPassword();
                    case 0 -> {
                        ConsoleUtil.printInfo("Đã đăng xuất khỏi tài khoản GIẢNG VIÊN.");
                        return;
                    }
                }
            } catch (AppException e) {
                ConsoleUtil.printError(e.getMessage());
                ConsoleUtil.pause();
            } catch (Exception e) {
                ConsoleUtil.printError("Lỗi hệ thống: " + e.getMessage());
                ConsoleUtil.pause();
            }
        }
    }

    private List<ClassSubject> printAssignedCourses(int teacherId) {
        List<ClassSubject> list = subjectService.getClassSubjectsByTeacher(teacherId);
        String[] headers = {"ID Học phần", "Học kỳ", "Mã Lớp", "Tên Lớp", "Mã Môn", "Tên Môn Học", "Số TC", "Trạng thái sổ"};
        List<String[]> rows = new ArrayList<>();
        for (ClassSubject cs : list) {
            rows.add(new String[]{
                    String.valueOf(cs.getId()),
                    cs.getSemester(),
                    cs.getClassCode(),
                    cs.getClassName(),
                    cs.getSubjectCode(),
                    cs.getSubjectName(),
                    String.valueOf(cs.getCredits()),
                    cs.isLocked() ? "ĐÃ KHÓA SỔ" : "Đang mở"
            });
        }
        ConsoleUtil.printTable(headers, rows);
        return list;
    }

    private ClassSubject selectOwnCourse(Teacher teacher) {
        List<ClassSubject> courses = printAssignedCourses(teacher.getId());
        if (courses.isEmpty()) {
            throw new ValidationException("Hiện tại bạn chưa được phân công giảng dạy lớp học phần nào.");
        }
        int csId = ConsoleUtil.readInt("Nhập [ID Học phần] cần thao tác", 1, Integer.MAX_VALUE);
        return courses.stream()
                .filter(c -> c.getId() == csId)
                .findFirst()
                .orElseThrow(() -> new ValidationException("ID Học phần không thuộc danh sách giảng dạy của bạn."));
    }

    // =========================================================================
    // F. QUẢN LÝ ĐIỂM (NHẬP ĐIỂM, TÍNH TỔNG KẾT + XẾP LOẠI, KHÓA SỔ, XUẤT CSV)
    // =========================================================================
    private void enterOrUpdateScoresUI(Teacher teacher) {
        ClassSubject cs = selectOwnCourse(teacher);

        while (true) {
            List<Score> scores = scoreService.getScoresByClassSubject(cs.getId());
            ConsoleUtil.printSubHeader(String.format("BẢNG ĐIỂM LỚP %s — MÔN: %s (%s) — HK: %s [%s]",
                    cs.getClassCode(), cs.getSubjectName(), cs.getSubjectCode(), cs.getSemester(),
                    cs.isLocked() ? "ĐÃ KHÓA SỔ" : "ĐANG MỞ"));
            printScoreTable(scores);

            if (cs.isLocked()) {
                ConsoleUtil.printWarning("Sổ điểm học phần này đã bị khóa! Bạn chỉ có thể xem.");
                ConsoleUtil.pause();
                return;
            }

            System.out.println("  [1] Nhập/Sửa điểm cho 1 Sinh viên theo ID SV");
            System.out.println("  [2] Nhập điểm liên tiếp cho toàn bộ danh sách lớp");
            System.out.println("  [0] Quay lại");
            int sub = ConsoleUtil.readInt("Chọn thao tác", 0, 2);
            if (sub == 0) return;

            if (sub == 1) {
                int studentId = ConsoleUtil.readInt("Nhập ID Sinh viên cần nhập/sửa điểm", 1, Integer.MAX_VALUE);
                Score target = scores.stream()
                        .filter(s -> s.getStudentId() == studentId)
                        .findFirst()
                        .orElseThrow(() -> new ValidationException("Sinh viên ID=" + studentId + " không thuộc lớp " + cs.getClassCode()));
                promptScoreForStudent(cs.getId(), target);
            } else if (sub == 2) {
                for (Score target : scores) {
                    System.out.println("\n--------------------------------------------------");
                    promptScoreForStudent(cs.getId(), target);
                }
                ConsoleUtil.printSuccess("Đã hoàn tất nhập điểm theo danh sách lớp!");
            }
        }
    }

    private void promptScoreForStudent(int classSubjectId, Score current) {
        ConsoleUtil.printInfo("Nhập điểm cho SV: " + current.getStudentCode() + " - " + current.getStudentName());
        Double att = ConsoleUtil.readOptionalScore("Điểm Chuyên cần (10%)", current.getAttendanceScore());
        Double mid = ConsoleUtil.readOptionalScore("Điểm Giữa kỳ (30%)", current.getMidtermScore());
        Double fin = ConsoleUtil.readOptionalScore("Điểm Cuối kỳ (60%)", current.getFinalScore());

        Score saved = scoreService.saveOrUpdateScore(current.getStudentId(), classSubjectId, att, mid, fin);
        if (saved.getTotalScore() != null) {
            ConsoleUtil.printSuccess(String.format("Đã lưu! Điểm tổng kết: %.2f | Điểm chữ: %s (%s)",
                    saved.getTotalScore(), saved.getGrade().name(), saved.getGrade().getDescription()));
        } else {
            ConsoleUtil.printSuccess("Đã lưu điểm thành phần (chưa đủ 3 cột để tính tổng kết).");
        }
    }

    private void lockCourseGradebookUI(Teacher teacher) {
        ClassSubject cs = selectOwnCourse(teacher);
        if (cs.isLocked()) {
            ConsoleUtil.printWarning("Học phần này đã được khóa sổ từ trước.");
            ConsoleUtil.pause();
            return;
        }
        if (ConsoleUtil.confirm("CẢNH BÁO: Sau khi khóa sổ điểm môn '" + cs.getSubjectName() + "' của lớp " + cs.getClassCode() + ", bạn sẽ KHÔNG THỂ sửa điểm nữa. Xác nhận khóa?")) {
            scoreService.lockCourseScores(cs.getId());
            ConsoleUtil.printSuccess("Đã khóa sổ điểm thành công!");
        }
        ConsoleUtil.pause();
    }

    private void exportCourseScoresUI(Teacher teacher) {
        ClassSubject cs = selectOwnCourse(teacher);
        String defaultName = String.format("scores_%s_%s_%s.csv", cs.getClassCode(), cs.getSubjectCode(), cs.getSemester());
        String fileName = ConsoleUtil.readStringWithDefault("Tên file CSV", defaultName);
        String path = scoreService.exportClassSubjectScoresToCsv(cs.getId(), fileName);
        ConsoleUtil.printSuccess("Đã xuất bảng điểm CSV tại: " + path);
        ConsoleUtil.pause();
    }

    private void printScoreTable(List<Score> scores) {
        String[] headers = {"ID SV", "Mã SV", "Họ và Tên", "Chuyên cần (10%)", "Giữa kỳ (30%)", "Cuối kỳ (60%)", "Tổng kết", "Điểm chữ", "Kết quả", "Sổ điểm"};
        List<String[]> rows = new ArrayList<>();
        for (Score sc : scores) {
            String status = sc.getGrade() == null ? "Chưa đủ điểm" : (sc.getGrade().isPassed() ? "ĐẠT" : "RỚT");
            rows.add(new String[]{
                    String.valueOf(sc.getStudentId()),
                    sc.getStudentCode(),
                    sc.getStudentName(),
                    sc.getAttendanceScore() != null ? String.format("%.2f", sc.getAttendanceScore()) : "-",
                    sc.getMidtermScore() != null ? String.format("%.2f", sc.getMidtermScore()) : "-",
                    sc.getFinalScore() != null ? String.format("%.2f", sc.getFinalScore()) : "-",
                    sc.getTotalScore() != null ? String.format("%.2f", sc.getTotalScore()) : "-",
                    sc.getGrade() != null ? sc.getGrade().name() : "-",
                    status,
                    sc.isLocked() ? "Khóa" : "Mở"
            });
        }
        ConsoleUtil.printTable(headers, rows);
    }

    // =========================================================================
    // G. XEM BÁO CÁO LỚP (DANH SÁCH SV + GPA, TOP 5 CAO/THẤP, TỈ LỆ ĐẠT/RỚT)
    // =========================================================================
    private void viewClassReportMenu(Teacher teacher) {
        String[] subOptions = {
                "Xem Danh sách SV + GPA của lớp & Top 5 SV GPA Cao nhất / Thấp nhất",
                "Xem Thống kê Tỉ lệ Đạt / Rớt theo Lớp học phần giảng dạy"
        };
        while (true) {
            ConsoleUtil.printMenu("BÁO CÁO LỚP HỌC DÀNH CHO GIẢNG VIÊN", subOptions);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, subOptions.length);
            if (choice == 0) return;
            try {
                if (choice == 1) {
                    List<SchoolClass> allClasses = classService.getAllClasses();
                    String[] cHeaders = {"ID Lớp", "Mã Lớp", "Tên Lớp", "Niên khóa", "GV Chủ nhiệm", "Sĩ số"};
                    List<String[]> cRows = new ArrayList<>();
                    for (SchoolClass c : allClasses) {
                        cRows.add(new String[]{
                                String.valueOf(c.getId()), c.getClassCode(), c.getName(), c.getAcademicYear(),
                                c.getHomeroomTeacherName() != null ? c.getHomeroomTeacherName() : "-", String.valueOf(c.getStudentCount())
                        });
                    }
                    ConsoleUtil.printTable(cHeaders, cRows);

                    int classId = ConsoleUtil.readInt("Nhập ID Lớp cần xem báo cáo GPA", 1, Integer.MAX_VALUE);
                    SchoolClass selectedClass = classService.getById(classId);

                    ConsoleUtil.printSubHeader("1. DANH SÁCH SINH VIÊN & GPA LỚP " + selectedClass.getClassCode());
                    List<Student> students = studentService.getStudentsByClass(classId);
                    String[] sHeaders = {"Mã SV", "Họ và Tên", "Giới tính", "Ngày sinh", "GPA (Hệ 10)", "GPA (Hệ 4)"};
                    List<String[]> sRows = new ArrayList<>();
                    for (Student s : students) {
                        sRows.add(new String[]{
                                s.getStudentCode(), s.getFullName(), s.getGender().getVietnameseLabel(),
                                s.getDob().toString(),
                                s.getGpa10() != null ? String.format("%.2f", s.getGpa10()) : "-",
                                s.getGpa4() != null ? String.format("%.2f", s.getGpa4()) : "-"
                        });
                    }
                    ConsoleUtil.printTable(sHeaders, sRows);

                    ConsoleUtil.printSubHeader("2. TOP 5 SINH VIÊN GPA CAO NHẤT LỚP " + selectedClass.getClassCode());
                    printTopGpaTable(reportService.getTop5ClassGpa(classId, true));

                    ConsoleUtil.printSubHeader("3. TOP 5 SINH VIÊN GPA THẤP NHẤT LỚP " + selectedClass.getClassCode());
                    printTopGpaTable(reportService.getTop5ClassGpa(classId, false));

                } else if (choice == 2) {
                    ClassSubject cs = selectOwnCourse(teacher);
                    String[] summary = reportService.getCoursePassFailSummary(cs.getId());
                    ConsoleUtil.printSubHeader("THỐNG KÊ ĐẠT / RỚT HỌC PHẦN: " + cs.getSubjectName() + " — LỚP " + cs.getClassCode());
                    String[] headers = {"Tổng SV lớp", "Số SV đã có điểm", "Điểm TB môn", "Số SV ĐẠT (>=4.0)", "Số SV RỚT (<4.0)", "Phân bố điểm chữ"};
                    List<String[]> rows = new ArrayList<>();
                    rows.add(summary);
                    ConsoleUtil.printTable(headers, rows);
                }
            } catch (Exception e) {
                ConsoleUtil.printError(e.getMessage());
            }
            ConsoleUtil.pause();
        }
    }

    private void printTopGpaTable(List<StudentGpaDTO> list) {
        String[] headers = {"Hạng", "Mã SV", "Họ và Tên", "Tín chỉ", "GPA (Hệ 10)", "GPA (Hệ 4)", "Xếp loại"};
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            StudentGpaDTO d = list.get(i);
            rows.add(new String[]{
                    "#" + (i + 1), d.getStudentCode(), d.getFullName(),
                    String.valueOf(d.getTotalCredits()),
                    String.format("%.2f", d.getGpa10()),
                    String.format("%.2f", d.getGpa4()),
                    d.getAcademicRank()
            });
        }
        ConsoleUtil.printTable(headers, rows);
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
