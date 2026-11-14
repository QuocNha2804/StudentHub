package com.studenthub.ui;

import com.studenthub.dao.ReportDAO;
import com.studenthub.dto.TranscriptItemDTO;
import com.studenthub.exception.AppException;
import com.studenthub.model.Student;
import com.studenthub.model.User;
import com.studenthub.service.AuthService;
import com.studenthub.service.ScoreService;
import com.studenthub.service.StudentService;
import com.studenthub.util.CSVUtil;
import com.studenthub.util.ConsoleUtil;

import java.util.ArrayList;
import java.util.List;

public class StudentMenu {
    private final User currentUser;
    private final AuthService authService = new AuthService();
    private final StudentService studentService = new StudentService();
    private final ScoreService scoreService = new ScoreService();

    public StudentMenu(User currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        String[] options = {
                "Xem Hồ sơ cá nhân (Profile) & GPA tổng kết",
                "Xem Lịch sử học tập & Điểm theo Học kỳ",
                "In Bảng điểm tổng hợp ra Console (Formatted) & Xuất CSV",
                "Đổi mật khẩu cá nhân"
        };

        while (true) {
            Student student;
            try {
                student = studentService.getByUserId(currentUser.getId());
            } catch (AppException e) {
                ConsoleUtil.printError(e.getMessage());
                return;
            }

            ConsoleUtil.printMenu("STUDENT PORTAL — SV: " + student.getFullName() + " (" + student.getStudentCode() + ")", options);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, options.length);
            try {
                switch (choice) {
                    case 1 -> showProfile(student);
                    case 2 -> showTranscriptBySemester(student);
                    case 3 -> printFormattedTranscript(student);
                    case 4 -> changeOwnPassword();
                    case 0 -> {
                        ConsoleUtil.printInfo("Đã đăng xuất khỏi tài khoản SINH VIÊN.");
                        return;
                    }
                }
            } catch (AppException e) {
                ConsoleUtil.printError(e.getMessage());
                ConsoleUtil.pause();
            }
        }
    }

    private void showProfile(Student s) {
        ConsoleUtil.printSubHeader("HỒ SƠ SINH VIÊN CHI TIẾT");
        String rank = s.getGpa4() != null ? ReportDAO.classifyAcademicRank(s.getGpa4()) : "Chưa xếp loại";
        String[] headers = {"Trường thông tin", "Giá trị chi tiết"};
        List<String[]> rows = List.of(
                new String[]{"Mã Sinh viên", s.getStudentCode()},
                new String[]{"Họ và Tên", s.getFullName()},
                new String[]{"Ngày sinh", s.getDob() != null ? s.getDob().toString() : "-"},
                new String[]{"Giới tính", s.getGender().getVietnameseLabel()},
                new String[]{"Email", s.getEmail()},
                new String[]{"Số điện thoại", s.getPhone()},
                new String[]{"Địa chỉ", s.getAddress()},
                new String[]{"Lớp niên chế", s.getClassCode() != null ? s.getClassCode() + " - " + s.getClassName() : "Chưa xếp lớp"},
                new String[]{"Trạng thái học tập", s.getStatus().getLabel()},
                new String[]{"GPA Tích lũy (Hệ 10)", s.getGpa10() != null ? String.format("%.2f / 10.0", s.getGpa10()) : "Chưa có điểm"},
                new String[]{"GPA Tích lũy (Hệ 4)", s.getGpa4() != null ? String.format("%.2f / 4.0", s.getGpa4()) : "Chưa có điểm"},
                new String[]{"Xếp loại học lực", rank}
        );
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void showTranscriptBySemester(Student s) {
        String sem = ConsoleUtil.readString("Nhập Học kỳ muốn xem (VD: 2026-1, hoặc nhấn Enter để xem tất cả)", false);
        List<TranscriptItemDTO> items = scoreService.getStudentTranscript(s.getId(), sem);
        ConsoleUtil.printSubHeader("KẾT QUẢ HỌC TẬP — " + (sem.isEmpty() ? "TẤT CẢ HỌC KỲ" : "HỌC KỲ " + sem));
        printTranscriptTable(items);
        ConsoleUtil.pause();
    }

    private void printFormattedTranscript(Student s) {
        List<TranscriptItemDTO> items = scoreService.getStudentTranscript(s.getId(), null);

        System.out.println("\n" + ConsoleUtil.CYAN + ConsoleUtil.BOLD + "==========================================================================================" + ConsoleUtil.RESET);
        System.out.println(ConsoleUtil.BOLD + "                         BẢNG ĐIỂM HỌC TẬP CÁ NHÂN — STUDENTHUB                           " + ConsoleUtil.RESET);
        System.out.println(ConsoleUtil.CYAN + "==========================================================================================" + ConsoleUtil.RESET);
        System.out.printf("  Mã Sinh viên : %-25s Họ và Tên : %s%n", s.getStudentCode(), s.getFullName());
        System.out.printf("  Ngày sinh    : %-25s Giới tính : %s%n", s.getDob(), s.getGender().getVietnameseLabel());
        System.out.printf("  Lớp niên chế : %-25s Trạng thái: %s%n",
                s.getClassCode() != null ? s.getClassCode() : "Chưa xếp lớp", s.getStatus().getLabel());
        System.out.println(ConsoleUtil.CYAN + "------------------------------------------------------------------------------------------" + ConsoleUtil.RESET);

        printTranscriptTable(items);

        int totalCredits = 0;
        int passedCredits = 0;
        for (TranscriptItemDTO item : items) {
            if (item.getGrade() != null) {
                totalCredits += item.getCredits();
                if (item.getGrade().isPassed()) {
                    passedCredits += item.getCredits();
                }
            }
        }

        String rank = s.getGpa4() != null ? ReportDAO.classifyAcademicRank(s.getGpa4()) : "Chưa xếp loại";
        System.out.printf("  Tổng số tín chỉ đã học : %-15d Số tín chỉ tích lũy (Đạt) : %d%n", totalCredits, passedCredits);
        System.out.printf("  GPA Tích lũy (Hệ 10)   : %-15s GPA Tích lũy (Hệ 4)       : %s%n",
                s.getGpa10() != null ? String.format("%.2f", s.getGpa10()) : "-",
                s.getGpa4() != null ? String.format("%.2f", s.getGpa4()) : "-");
        System.out.printf("  Xếp loại học lực       : %s%s%s%n", ConsoleUtil.YELLOW + ConsoleUtil.BOLD, rank, ConsoleUtil.RESET);
        System.out.println(ConsoleUtil.CYAN + "==========================================================================================" + ConsoleUtil.RESET);

        if (ConsoleUtil.confirm("Bạn có muốn xuất bảng điểm cá nhân ra file CSV không?")) {
            String[] headers = {"Học kỳ", "Mã Môn", "Tên Môn Học", "Số TC", "Chuyên cần (10%)", "Giữa kỳ (30%)", "Cuối kỳ (60%)", "Tổng kết (10)", "Điểm chữ", "Điểm hệ 4", "Kết quả"};
            List<String[]> rows = new ArrayList<>();
            for (TranscriptItemDTO item : items) {
                rows.add(new String[]{
                        item.getSemester(),
                        item.getSubjectCode(),
                        item.getSubjectName(),
                        String.valueOf(item.getCredits()),
                        fmt(item.getAttendanceScore()),
                        fmt(item.getMidtermScore()),
                        fmt(item.getFinalScore()),
                        fmt(item.getTotalScore()),
                        item.getGrade() != null ? item.getGrade().name() : "-",
                        item.getGrade() != null ? String.format("%.1f", item.getGrade().getGpa4()) : "-",
                        item.getGrade() == null ? "Chưa có điểm" : (item.getGrade().isPassed() ? "ĐẠT" : "RỚT")
                });
            }
            String path = CSVUtil.exportToCsv("transcript_" + s.getStudentCode() + ".csv", headers, rows);
            ConsoleUtil.printSuccess("Đã xuất bảng điểm cá nhân tại: " + path);
        }
        ConsoleUtil.pause();
    }

    private void printTranscriptTable(List<TranscriptItemDTO> items) {
        String[] headers = {"Học kỳ", "Mã Môn", "Tên Môn Học", "Số TC", "CC (10%)", "GK (30%)", "CK (60%)", "Tổng kết", "Điểm chữ", "Hệ 4", "Kết quả"};
        List<String[]> rows = new ArrayList<>();
        for (TranscriptItemDTO item : items) {
            rows.add(new String[]{
                    item.getSemester(),
                    item.getSubjectCode(),
                    item.getSubjectName(),
                    String.valueOf(item.getCredits()),
                    fmt(item.getAttendanceScore()),
                    fmt(item.getMidtermScore()),
                    fmt(item.getFinalScore()),
                    fmt(item.getTotalScore()),
                    item.getGrade() != null ? item.getGrade().name() : "-",
                    item.getGrade() != null ? String.format("%.1f", item.getGrade().getGpa4()) : "-",
                    item.getGrade() == null ? "Chưa chấm" : (item.getGrade().isPassed() ? "ĐẠT" : "RỚT")
            });
        }
        ConsoleUtil.printTable(headers, rows);
    }

    private String fmt(Double v) {
        return v == null ? "-" : String.format("%.2f", v);
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
