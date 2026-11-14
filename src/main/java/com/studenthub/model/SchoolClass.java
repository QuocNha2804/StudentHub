package com.studenthub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass {
    private int id;
    private String classCode;
    private String name;
    private String academicYear;
    private Integer homeroomTeacherId;
    private String homeroomTeacherName; // Transient field when joined with teachers
    private int studentCount;           // Transient field when counting students
}
