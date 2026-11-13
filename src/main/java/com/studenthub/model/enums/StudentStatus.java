package com.studenthub.model.enums;

public enum StudentStatus {
    ACTIVE("Đang học"),
    INACTIVE("Nghỉ học / Đã xóa"),
    GRADUATED("Đã tốt nghiệp");

    private final String label;

    StudentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
