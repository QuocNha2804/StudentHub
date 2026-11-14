package com.studenthub.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {
    private int id;
    private String teacherCode;
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private Integer userId;
    private String username; // Transient field when joined with users
}
