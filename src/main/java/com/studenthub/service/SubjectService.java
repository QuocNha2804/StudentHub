package com.studenthub.service;

import com.studenthub.dao.ClassDAO;
import com.studenthub.dao.ClassSubjectDAO;
import com.studenthub.dao.SubjectDAO;
import com.studenthub.dao.TeacherDAO;
import com.studenthub.exception.DuplicateResourceException;
import com.studenthub.exception.NotFoundException;
import com.studenthub.model.ClassSubject;
import com.studenthub.model.Subject;
import com.studenthub.util.LogUtil;
import com.studenthub.util.Validator;

import java.util.List;

public class SubjectService {
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final ClassSubjectDAO classSubjectDAO = new ClassSubjectDAO();
    private final ClassDAO classDAO = new ClassDAO();
    private final TeacherDAO teacherDAO = new TeacherDAO();

    public List<Subject> getAllSubjects() {
        return subjectDAO.findAll();
    }

    public Subject getById(int id) {
        return subjectDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học có ID = " + id));
    }

    public Subject createSubject(String subjectCode, String name, int credits, String department) {
        Validator.validateCode(subjectCode, "Mã môn học");
        Validator.requireNonBlank(name, "Tên môn học");
        Validator.validateCredits(credits);
        Validator.requireNonBlank(department, "Khoa phụ trách");

        String normalizedCode = subjectCode.trim().toUpperCase();
        if (subjectDAO.findByCode(normalizedCode).isPresent()) {
            throw new DuplicateResourceException("Mã môn học '" + normalizedCode + "' đã tồn tại.");
        }

        Subject subject = Subject.builder()
                .subjectCode(normalizedCode)
                .name(name.trim())
                .credits(credits)
                .department(department.trim())
                .build();
        int id = subjectDAO.insert(subject);
        subject.setId(id);
        LogUtil.info("Thêm môn học mới: " + normalizedCode + " - " + name);
        return subject;
    }

    public void updateSubject(int id, String subjectCode, String name, int credits, String department) {
        Subject existing = getById(id);
        Validator.validateCode(subjectCode, "Mã môn học");
        Validator.requireNonBlank(name, "Tên môn học");
        Validator.validateCredits(credits);
        Validator.requireNonBlank(department, "Khoa phụ trách");

        String normalizedCode = subjectCode.trim().toUpperCase();
        subjectDAO.findByCode(normalizedCode).ifPresent(other -> {
            if (other.getId() != id) {
                throw new DuplicateResourceException("Mã môn học '" + normalizedCode + "' đã tồn tại.");
            }
        });

        existing.setSubjectCode(normalizedCode);
        existing.setName(name.trim());
        existing.setCredits(credits);
        existing.setDepartment(department.trim());

        subjectDAO.update(existing);
        LogUtil.info("Cập nhật môn học ID=" + id + " (" + normalizedCode + ")");
    }

    public void deleteSubject(int id) {
        Subject existing = getById(id);
        subjectDAO.deleteById(id);
        LogUtil.info("Xóa môn học: " + existing.getSubjectCode());
    }

    public List<ClassSubject> getAllClassSubjects() {
        return classSubjectDAO.findAll();
    }

    public List<ClassSubject> getClassSubjectsByTeacher(int teacherId) {
        return classSubjectDAO.findByTeacherId(teacherId);
    }

    public ClassSubject getClassSubjectById(int id) {
        return classSubjectDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học phần có ID = " + id));
    }

    public void assignTeacherToSubject(int classId, int subjectId, int teacherId, String semester) {
        Validator.requireNonBlank(semester, "Học kỳ (VD: 2026-1)");
        if (classDAO.findById(classId).isEmpty()) {
            throw new NotFoundException("Không tìm thấy lớp học ID = " + classId);
        }
        if (subjectDAO.findById(subjectId).isEmpty()) {
            throw new NotFoundException("Không tìm thấy môn học ID = " + subjectId);
        }
        if (teacherDAO.findById(teacherId).isEmpty()) {
            throw new NotFoundException("Không tìm thấy giảng viên ID = " + teacherId);
        }
        if (classSubjectDAO.exists(classId, subjectId, semester.trim())) {
            throw new DuplicateResourceException("Lớp học này đã được phân công môn học trong học kỳ " + semester + " rồi.");
        }

        classSubjectDAO.insert(classId, subjectId, teacherId, semester.trim());
        LogUtil.info(String.format("Phân công giảng dạy: classId=%d, subjectId=%d, teacherId=%d, semester=%s",
                classId, subjectId, teacherId, semester));
    }
}
