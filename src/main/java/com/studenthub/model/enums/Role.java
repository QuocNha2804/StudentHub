package com.studenthub.model.enums;

public enum Role {
    ADMIN,
    TEACHER,
    STUDENT;

    public static Role fromString(String value) {
        for (Role r : values()) {
            if (r.name().equalsIgnoreCase(value.trim())) {
                return r;
            }
        }
        throw new IllegalArgumentException("Role không hợp lệ: " + value + " (Chỉ chấp nhận ADMIN, TEACHER, STUDENT)");
    }
}
