package com.studenthub.util;

import com.studenthub.config.AppConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LogUtil {
    private static final Logger LOGGER = Logger.getLogger("StudentHubLogger");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            LOGGER.setUseParentHandlers(false);
            String logPath = AppConfig.getLogFile();
            File logFile = new File(logPath);
            if (logFile.getParentFile() != null && !logFile.getParentFile().exists()) {
                logFile.getParentFile().mkdirs();
            }

            FileHandler fileHandler = new FileHandler(logPath, true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    String timestamp = LocalDateTime.now().format(FORMATTER);
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("[%s] [%s] %s%n",
                            timestamp,
                            record.getLevel().getName(),
                            formatMessage(record)));
                    if (record.getThrown() != null) {
                        sb.append("Exception: ").append(record.getThrown().toString()).append(System.lineSeparator());
                        for (StackTraceElement element : record.getThrown().getStackTrace()) {
                            sb.append("\tat ").append(element.toString()).append(System.lineSeparator());
                        }
                    }
                    return sb.toString();
                }
            });
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("[WARN] Không thể khởi tạo file log: " + e.getMessage());
        }
    }

    private LogUtil() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warning(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    public static void error(String message) {
        LOGGER.severe(message);
    }
}
