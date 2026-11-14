package com.studenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPassRateDTO {
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private int credits;
    private int totalGradedStudents;
    private int passedCount;
    private int failedCount;
    private double passRatePercent;
    private double averageScore;
}
