package com.studenthub.ui;

import com.studenthub.exception.AppException;
import com.studenthub.model.User;
import com.studenthub.service.AuthService;
import com.studenthub.util.ConsoleUtil;

public class MainMenu {
    private final AuthService authService = new AuthService();

    public void start() {
        String[] options = {
                "Đăng nhập vào hệ thống",
                "Xem tài khoản mẫu (Demo Accounts)"
        };

        while (true) {
            ConsoleUtil.printMenu("STUDENTHUB — HỆ THỐNG QUẢN LÝ SINH VIÊN (CONSOLE)", options);
            int choice = ConsoleUtil.readInt("Chọn chức năng", 0, options.length);
            switch (choice) {
                case 1 -> handleLogin();
                case 2 -> showDemoAccounts();
                case 0 -> {
                    ConsoleUtil.printSuccess("Cảm ơn bạn đã sử dụng StudentHub! Hẹn gặp lại.");
                    return;
                }
            }
        }
    }

    private void handleLogin() {
        ConsoleUtil.printSubHeader("ĐĂNG NHẬP HỆ THỐNG");
        String username = ConsoleUtil.readString("Username", true);
        String password = ConsoleUtil.readString("Password", true);

        try {
            User user = authService.login(username, password);
            ConsoleUtil.printSuccess("Đăng nhập thành công! Xin chào " + user.getUsername() + " [" + user.getRole() + "]");

            switch (user.getRole()) {
                case ADMIN -> new AdminMenu(user).show();
                case TEACHER -> new TeacherMenu(user).show();
                case STUDENT -> new StudentMenu(user).show();
            }
        } catch (AppException e) {
            ConsoleUtil.printError(e.getMessage());
            ConsoleUtil.pause();
        }
    }

    private void showDemoAccounts() {
        ConsoleUtil.printSubHeader("TÀI KHOẢN MẪU ĐƯỢC KHỞI TẠO TỰ ĐỘNG");
        String[] headers = {"Role", "Username", "Password", "Ghi chú"};
        java.util.List<String[]> rows = java.util.List.of(
                new String[]{"ADMIN", "admin", "Admin@123", "Quản trị toàn hệ thống"},
                new String[]{"TEACHER", "gv001", "123456", "TS. Nguyễn Văn An (Dạy Java & CTDL)"},
                new String[]{"TEACHER", "gv002", "123456", "ThS. Trần Thị Bình (Dạy CSDL)"},
                new String[]{"STUDENT", "SV001", "123456", "SV Lê Hoàng Nam (Lớp DHKTPM18A)"},
                new String[]{"STUDENT", "SV002", "123456", "SV Phạm Ngọc Mai (Lớp DHKTPM18A)"},
                new String[]{"STUDENT", "SV003", "123456", "SV Đỗ Minh Tuấn (Lớp DHHTTT18B)"}
        );
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }
}
