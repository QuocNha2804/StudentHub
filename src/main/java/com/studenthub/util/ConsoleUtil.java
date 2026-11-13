package com.studenthub.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public final class ConsoleUtil {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String BOLD = "\u001B[1m";

    private static final Scanner SCANNER = new Scanner(System.in, StandardCharsets.UTF_8);

    private ConsoleUtil() {
    }

    public static void printHeader(String title) {
        String line = "═".repeat(Math.max(56, title.length() + 10));
        System.out.println("\n" + CYAN + BOLD + "╔" + line + "╗" + RESET);
        int padding = (line.length() - title.length()) / 2;
        String centered = " ".repeat(Math.max(0, padding)) + title + " ".repeat(Math.max(0, line.length() - padding - title.length()));
        System.out.println(CYAN + BOLD + "║" + centered + "║" + RESET);
        System.out.println(CYAN + BOLD + "╚" + line + "╝" + RESET);
    }

    public static void printSubHeader(String title) {
        System.out.println("\n" + PURPLE + BOLD + "── " + title + " ──" + RESET);
    }

    public static void printMenu(String title, String[] options) {
        printHeader(title);
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %s[%d]%s %s%n", YELLOW + BOLD, (i + 1), RESET, options[i]);
        }
        System.out.printf("  %s[0]%s Quay lại / Thoát%n", RED + BOLD, RESET);
        System.out.println(CYAN + "─".repeat(58) + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "✔ [THÀNH CÔNG] " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + "✖ [LỖI] " + message + RESET);
    }

    public static void printWarning(String message) {
        System.out.println(YELLOW + "⚠ [CẢNH BÁO] " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(BLUE + "ℹ " + message + RESET);
    }

    public static String readString(String prompt, boolean required) {
        while (true) {
            System.out.print(BOLD + prompt + ": " + RESET);
            String input = SCANNER.nextLine().trim();
            if (required && input.isEmpty()) {
                printError("Trường này không được để trống. Vui lòng nhập lại!");
                continue;
            }
            return input;
        }
    }

    public static String readStringWithDefault(String prompt, String defaultValue) {
        System.out.printf("%s%s [%s]: %s", BOLD, prompt, defaultValue == null ? "" : defaultValue, RESET);
        String input = SCANNER.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(BOLD + prompt + ": " + RESET);
            String input = SCANNER.nextLine().trim();
            try {
                int val = Integer.parseInt(input);
                if (val < min || val > max) {
                    printError(String.format("Vui lòng nhập số nguyên từ %d đến %d.", min, max));
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                printError("Dữ liệu nhập vào phải là số nguyên hợp lệ!");
            }
        }
    }

    public static Integer readOptionalInt(String prompt) {
        while (true) {
            System.out.print(BOLD + prompt + " (Enter để bỏ qua): " + RESET);
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                printError("Vui lòng nhập số nguyên hợp lệ hoặc nhấn Enter để bỏ qua!");
            }
        }
    }

    public static double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(BOLD + prompt + ": " + RESET);
            String input = SCANNER.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val < min || val > max) {
                    printError(String.format("Vui lòng nhập số thực từ %.1f đến %.1f.", min, max));
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                printError("Dữ liệu nhập vào phải là số thực hợp lệ!");
            }
        }
    }

    public static Double readOptionalScore(String prompt, Double currentVal) {
        String hint = currentVal == null ? "Enter để bỏ qua" : "Hiện tại: " + currentVal + ", Enter để giữ nguyên";
        while (true) {
            System.out.printf("%s%s (%s): %s", BOLD, prompt, hint, RESET);
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return currentVal;
            }
            try {
                double val = Double.parseDouble(input);
                if (val < 0.0 || val > 10.0) {
                    printError("Điểm phải nằm trong khoảng 0.0 đến 10.0!");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                printError("Điểm nhập vào không hợp lệ!");
            }
        }
    }

    public static boolean confirm(String prompt) {
        System.out.print(YELLOW + BOLD + prompt + " (y/n): " + RESET);
        String input = SCANNER.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }

    public static void pause() {
        System.out.print("\n" + CYAN + "Nhấn Enter để tiếp tục..." + RESET);
        SCANNER.nextLine();
    }

    public static void printTable(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < cols && i < row.length; i++) {
                String cell = row[i] == null ? "-" : row[i];
                widths[i] = Math.max(widths[i], cell.length());
            }
        }

        StringBuilder sep = new StringBuilder("+");
        StringBuilder format = new StringBuilder("|");
        for (int w : widths) {
            sep.append("-".repeat(w + 2)).append("+");
            format.append(" %-").append(w).append("s |");
        }
        format.append("%n");

        System.out.println(CYAN + sep + RESET);
        System.out.printf(BOLD + format.toString() + RESET, (Object[]) headers);
        System.out.println(CYAN + sep + RESET);

        if (rows.isEmpty()) {
            int totalWidth = Math.max(10, sep.length() - 4);
            System.out.printf("| %-" + totalWidth + "s |%n", "Không có dữ liệu.");
        } else {
            for (String[] row : rows) {
                Object[] safeRow = new Object[cols];
                for (int i = 0; i < cols; i++) {
                    safeRow[i] = (i < row.length && row[i] != null) ? row[i] : "-";
                }
                System.out.printf(format.toString(), safeRow);
            }
        }
        System.out.println(CYAN + sep + RESET);
    }
}
