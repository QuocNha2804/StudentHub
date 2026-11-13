package com.studenthub.util;

import com.studenthub.config.AppConfig;
import com.studenthub.exception.AppException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CSVUtil {

    private CSVUtil() {
    }

    public static String exportToCsv(String fileName, String[] headers, List<String[]> rows) {
        File dir = new File(AppConfig.getExportDir());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!fileName.toLowerCase().endsWith(".csv")) {
            fileName += ".csv";
        }
        File outFile = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(outFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // Ghi UTF-8 BOM để mở bằng Microsoft Excel hiển thị đúng tiếng Việt
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            writer.write(toCsvLine(headers));
            writer.newLine();

            for (String[] row : rows) {
                writer.write(toCsvLine(row));
                writer.newLine();
            }
            writer.flush();
            LogUtil.info("Xuất file CSV thành công: " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            LogUtil.error("Lỗi khi xuất file CSV: " + fileName, e);
            throw new AppException("Không thể ghi file CSV: " + e.getMessage(), e);
        }
    }

    public static List<String[]> readCsv(String filePath, boolean skipHeader) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new AppException("Không tìm thấy file CSV tại đường dẫn: " + filePath);
        }

        List<String[]> records = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    // Xóa ký tự BOM nếu có
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    isFirstLine = false;
                    if (skipHeader) {
                        continue;
                    }
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                records.add(parseCsvLine(line));
            }
            return records;
        } catch (IOException e) {
            LogUtil.error("Lỗi khi đọc file CSV: " + filePath, e);
            throw new AppException("Không thể đọc file CSV: " + e.getMessage(), e);
        }
    }

    private static String toCsvLine(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            String field = fields[i] == null ? "" : fields[i];
            if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                field = "\"" + field.replace("\"", "\"\"") + "\"";
            }
            sb.append(field);
        }
        return sb.toString();
    }

    private static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    current.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        tokens.add(current.toString().trim());
        return tokens.toArray(new String[0]);
    }
}
