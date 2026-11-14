package com.studenthub.service;

import com.studenthub.dao.ClassDAO;
import com.studenthub.dao.TeacherDAO;
import com.studenthub.exception.DuplicateResourceException;
import com.studenthub.exception.NotFoundException;
import com.studenthub.model.SchoolClass;
import com.studenthub.util.LogUtil;
import com.studenthub.util.Validator;

import java.util.List;

public class ClassService {
    private final ClassDAO classDAO = new ClassDAO();
    private final TeacherDAO teacherDAO = new TeacherDAO();

    public List<SchoolClass> getAllClasses() {
        return classDAO.findAll();
    }

    public List<SchoolClass> getClassesByHomeroomTeacher(int teacherId) {
        return classDAO.findByHomeroomTeacherId(teacherId);
    }

    public SchoolClass getById(int id) {
        return classDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học có ID = " + id));
    }

    public SchoolClass getByCode(String classCode) {
        return classDAO.findByCode(classCode.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học có mã: " + classCode));
    }

    public SchoolClass createClass(String classCode, String name, String academicYear, Integer homeroomTeacherId) {
        Validator.validateCode(classCode, "Mã lớp");
        Validator.requireNonBlank(name, "Tên lớp");
        Validator.requireNonBlank(academicYear, "Niên khóa");

        String normalizedCode = classCode.trim().toUpperCase();
        if (classDAO.findByCode(normalizedCode).isPresent()) {
            throw new DuplicateResourceException("Mã lớp '" + normalizedCode + "' đã tồn tại.");
        }
        if (homeroomTeacherId != null && teacherDAO.findById(homeroomTeacherId).isEmpty()) {
            throw new NotFoundException("Không tìm thấy giảng viên chủ nhiệm có ID = " + homeroomTeacherId);
        }

        SchoolClass sc = SchoolClass.builder()
                .classCode(normalizedCode)
                .name(name.trim())
                .academicYear(academicYear.trim())
                .homeroomTeacherId(homeroomTeacherId)
                .build();

        int id = classDAO.insert(sc);
        sc.setId(id);
        LogUtil.info("Tạo lớp học mới: " + normalizedCode);
        return sc;
    }

    public void updateClass(int id, String classCode, String name, String academicYear, Integer homeroomTeacherId) {
        SchoolClass existing = getById(id);
        Validator.validateCode(classCode, "Mã lớp");
        Validator.requireNonBlank(name, "Tên lớp");
        Validator.requireNonBlank(academicYear, "Niên khóa");

        String normalizedCode = classCode.trim().toUpperCase();
        classDAO.findByCode(normalizedCode).ifPresent(other -> {
            if (other.getId() != id) {
                throw new DuplicateResourceException("Mã lớp '" + normalizedCode + "' đã tồn tại.");
            }
        });

        if (homeroomTeacherId != null && teacherDAO.findById(homeroomTeacherId).isEmpty()) {
            throw new NotFoundException("Không tìm thấy giảng viên chủ nhiệm có ID = " + homeroomTeacherId);
        }

        existing.setClassCode(normalizedCode);
        existing.setName(name.trim());
        existing.setAcademicYear(academicYear.trim());
        existing.setHomeroomTeacherId(homeroomTeacherId);

        classDAO.update(existing);
        LogUtil.info("Cập nhật lớp học ID=" + id + " (" + normalizedCode + ")");
    }

    public void deleteClass(int id) {
        SchoolClass existing = getById(id);
        classDAO.deleteById(id);
        LogUtil.info("Xóa lớp học: " + existing.getClassCode());
    }
}
