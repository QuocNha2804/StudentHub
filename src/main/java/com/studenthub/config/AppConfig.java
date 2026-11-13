package com.studenthub.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                PROPERTIES.load(is);
            } else {
                System.err.println("[WARN] Không tìm thấy file db.properties, sử dụng cấu hình mặc định.");
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Lỗi đọc file db.properties: " + e.getMessage());
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String val = PROPERTIES.getProperty(key);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getDbUrl() {
        return get("db.url", "jdbc:mariadb://localhost:3306/studenthub?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8");
    }

    public static String getDbUsername() {
        return get("db.username", "root");
    }

    public static String getDbPassword() {
        return get("db.password", "root");
    }

    public static int getMaxFailedAttempts() {
        return getInt("auth.max_failed_attempts", 5);
    }

    public static int getLockDurationMinutes() {
        return getInt("auth.lock_duration_minutes", 15);
    }

    public static int getPageSize() {
        return getInt("app.page_size", 10);
    }

    public static String getExportDir() {
        return get("app.export_dir", "exports");
    }

    public static String getLogFile() {
        return get("app.log_file", "logs/app.log");
    }
}
