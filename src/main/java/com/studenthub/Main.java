package com.studenthub;

import com.studenthub.config.DatabaseInitializer;
import com.studenthub.ui.MainMenu;
import com.studenthub.util.ConsoleUtil;
import com.studenthub.util.LogUtil;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        // Đảm bảo Console xuất tiếng Việt UTF-8 chuẩn trên Windows/Linux/macOS
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        LogUtil.info("=== Khởi động ứng dụng StudentHub ===");

        try {
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            ConsoleUtil.printError("Không thể khởi tạo kết nối Cơ sở dữ liệu MariaDB!");
            ConsoleUtil.printWarning("Chi tiết: " + e.getMessage());
            ConsoleUtil.printInfo("Hãy kiểm tra dịch vụ MariaDB đang chạy và thông tin trong src/main/resources/db.properties.");
            return;
        }

        MainMenu mainMenu = new MainMenu();
        mainMenu.start();

        LogUtil.info("=== Đóng ứng dụng StudentHub ===");
    }
}
