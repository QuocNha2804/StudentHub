package com.studenthub.model.enums;

public enum Grade {
    A(4.0, true, "Giỏi / Xuất sắc"),
    B(3.0, true, "Khá"),
    C(2.0, true, "Trung bình"),
    D(1.0, true, "Trung bình yếu"),
    F(0.0, false, "Kém (Không đạt)");

    private final double gpa4;
    private final boolean passed;
    private final String description;

    Grade(double gpa4, boolean passed, String description) {
        this.gpa4 = gpa4;
        this.passed = passed;
        this.description = description;
    }

    public double getGpa4() {
        return gpa4;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getDescription() {
        return description;
    }

    public static Grade fromTotalScore(Double totalScore) {
        if (totalScore == null) {
            return null;
        }
        if (totalScore >= 8.5) {
            return A;
        } else if (totalScore >= 7.0) {
            return B;
        } else if (totalScore >= 5.5) {
            return C;
        } else if (totalScore >= 4.0) {
            return D;
        } else {
            return F;
        }
    }
}
