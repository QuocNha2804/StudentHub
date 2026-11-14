package com.studenthub.dto;

import com.studenthub.model.enums.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptItemDTO {
    private String semester;
    private String subjectCode;
    private String subjectName;
    private int credits;
    private Double attendanceScore;
    private Double midtermScore;
    private Double finalScore;
    private Double totalScore;
    private Grade grade;
    private boolean locked;
}
