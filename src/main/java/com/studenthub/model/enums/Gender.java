package com.studenthub.model.enums;

public enum Gender {
    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    private final String vietnameseLabel;

    Gender(String vietnameseLabel) {
        this.vietnameseLabel = vietnameseLabel;
    }

    public String getVietnameseLabel() {
        return vietnameseLabel;
    }

    public static Gender fromString(String input) {
        if (input == null) {
            return OTHER;
        }
        String val = input.trim().toUpperCase();
        return switch (val) {
            case "MALE", "NAM", "M", "1" -> MALE;
            case "FEMALE", "NU", "NỮ", "F", "2" -> FEMALE;
            default -> OTHER;
        };
    }
}
