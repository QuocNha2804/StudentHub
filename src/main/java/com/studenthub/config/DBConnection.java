package com.studenthub.config;

import com.studenthub.exception.AppException;
import com.studenthub.util.LogUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LogUtil.error("Không tìm thấy MariaDB JDBC Driver", e);
            throw new ExceptionInInitializerError("Không tìm thấy MariaDB JDBC Driver: " + e.getMessage());
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    AppConfig.getDbUrl(),
                    AppConfig.getDbUsername(),
                    AppConfig.getDbPassword()
            );
        } catch (SQLException e) {
            LogUtil.error("Lỗi kết nối Cơ sở dữ liệu MariaDB", e);
            throw new AppException("Không thể kết nối tới MariaDB (" + AppConfig.getDbUrl() + "). Vui lòng kiểm tra MariaDB Server và file db.properties. Chi tiết: " + e.getMessage(), e);
        }
    }

    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }
}
