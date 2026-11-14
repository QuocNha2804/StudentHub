package com.studenthub.model;

import com.studenthub.model.enums.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Score {
    private int id;
    private int studentId;
    private String studentCode;
    private String studentName;
    private int classSubjectId;
    private String subjectCode;
    private String subjectName;
    private int credits;
    private String semester;
    private Double attendanceScore;
    private Double midtermScore;
    private Double finalScore;
    private Double totalScore;
    private Grade grade;
    private boolean locked;
    private LocalDateTime updatedAt;
}
