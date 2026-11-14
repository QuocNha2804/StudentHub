package com.studenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGpaDTO {
    private int studentId;
    private String studentCode;
    private String fullName;
    private String classCode;
    private String gender;
    private int totalCredits;
    private double gpa10;
    private double gpa4;
    private String academicRank;
}
