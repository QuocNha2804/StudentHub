package com.studenthub.model;

import com.studenthub.model.enums.Gender;
import com.studenthub.model.enums.StudentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private int id;
    private String studentCode;
    private String fullName;
    private LocalDate dob;
    private Gender gender;
    private String email;
    private String phone;
    private String address;
    private Integer classId;
    private String classCode;       // Transient field from JOIN classes
    private String className;       // Transient field from JOIN classes
    private Integer userId;
    private StudentStatus status;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private Double gpa10;           // Transient field for GPA sorting/reporting
    private Double gpa4;            // Transient field for 4-scale GPA
}
