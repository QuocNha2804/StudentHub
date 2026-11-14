package com.studenthub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSubject {
    private int id;
    private int classId;
    private String classCode;
    private String className;
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private int credits;
    private int teacherId;
    private String teacherCode;
    private String teacherName;
    private String semester;
    private boolean locked;
}
