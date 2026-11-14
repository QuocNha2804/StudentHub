package com.studenthub.service;

import com.studenthub.dao.ClassSubjectDAO;
import com.studenthub.dao.ScoreDAO;
import com.studenthub.dto.TranscriptItemDTO;
import com.studenthub.exception.NotFoundException;
import com.studenthub.exception.ValidationException;
import com.studenthub.model.ClassSubject;
import com.studenthub.model.Score;
import com.studenthub.model.enums.Grade;
import com.studenthub.util.CSVUtil;
import com.studenthub.util.LogUtil;
import com.studenthub.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScoreService {
    private final ScoreDAO scoreDAO = new ScoreDAO();
    private final ClassSubjectDAO classSubjectDAO = new ClassSubjectDAO();

    public List<Score> getScoresByClassSubject(int classSubjectId) {
        return scoreDAO.findScoresByClassSubject(classSubjectId);
    }

    /**
     * Nhập / Sửa điểm cho 1 sinh viên trong lớp học phần.
     * Công thức: Chuyên cần (10%) + Giữa kỳ (30%) + Cuối kỳ (60%).
     */
    public Score saveOrUpdateScore(int studentId, int classSubjectId, Double attendance, Double midterm, Double finalExam) {
        ClassSubject cs = classSubjectDAO.findById(classSubjectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học phần ID = " + classSubjectId));

        if (cs.isLocked()) {
            throw new ValidationException("Bảng điểm của học phần này đã bị khóa sổ! Không thể chỉnh sửa.");
        }

        Optional<Score> existingOpt = scoreDAO.findByStudentAndClassSubject(studentId, classSubjectId);
        if (existingOpt.isPresent() && existingOpt.get().isLocked()) {
            throw new ValidationException("Điểm của sinh viên này đã bị khóa sổ! Không thể chỉnh sửa.");
        }

        Validator.validateScore(attendance, "Chuyên cần (10%)");
        Validator.validateScore(midterm, "Giữa kỳ (30%)");
        Validator.validateScore(finalExam, "Cuối kỳ (60%)");

        Double totalScore = null;
        Grade grade = null;
        if (attendance != null && midterm != null && finalExam != null) {
            double rawTotal = attendance * 0.10 + midterm * 0.30 + finalExam * 0.60;
            totalScore = Math.round(rawTotal * 100.0) / 100.0;
            grade = Grade.fromTotalScore(totalScore);
        }

        Score score = Score.builder()
                .studentId(studentId)
                .classSubjectId(classSubjectId)
                .attendanceScore(attendance)
                .midtermScore(midterm)
                .finalScore(finalExam)
                .totalScore(totalScore)
                .grade(grade)
                .locked(false)
                .build();

        scoreDAO.upsertScore(score);
        LogUtil.info(String.format("Cập nhật điểm: studentId=%d, classSubjectId=%d -> Total=%s, Grade=%s",
                studentId, classSubjectId, totalScore, grade));
        return score;
    }

    public void lockCourseScores(int classSubjectId) {
        ClassSubject cs = classSubjectDAO.findById(classSubjectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học phần ID = " + classSubjectId));
        if (cs.isLocked()) {
            throw new ValidationException("Học phần này đã được khóa sổ từ trước.");
        }
        classSubjectDAO.updateLockStatus(classSubjectId, true);
        LogUtil.info("Đã khóa sổ điểm lớp học phần ID=" + classSubjectId + " (" + cs.getClassCode() + " - " + cs.getSubjectCode() + ")");
    }

    public void unlockCourseScoresByAdmin(int classSubjectId) {
        ClassSubject cs = classSubjectDAO.findById(classSubjectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học phần ID = " + classSubjectId));
        classSubjectDAO.updateLockStatus(classSubjectId, false);
        LogUtil.info("Admin đã mở khóa sổ điểm lớp học phần ID=" + classSubjectId + " (" + cs.getClassCode() + " - " + cs.getSubjectCode() + ")");
    }

    public List<TranscriptItemDTO> getStudentTranscript(int studentId, String semesterFilter) {
        return scoreDAO.findTranscriptByStudent(studentId, semesterFilter);
    }

    public String exportClassSubjectScoresToCsv(int classSubjectId, String fileName) {
        ClassSubject cs = classSubjectDAO.findById(classSubjectId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học phần ID = " + classSubjectId));
        List<Score> scores = scoreDAO.findScoresByClassSubject(classSubjectId);

        String[] headers = {"Mã SV", "Họ và Tên", "Lớp", "Mã Môn", "Tên Môn", "Học Kỳ", "Chuyên cần (10%)", "Giữa kỳ (30%)", "Cuối kỳ (60%)", "Tổng kết", "Điểm chữ", "Kết quả", "Trạng thái sổ"};
        List<String[]> rows = new ArrayList<>();
        for (Score sc : scores) {
            String result = sc.getGrade() == null ? "Chưa đủ điểm" : (sc.getGrade().isPassed() ? "ĐẠT" : "RỚT");
            rows.add(new String[]{
                    sc.getStudentCode(),
                    sc.getStudentName(),
                    cs.getClassCode(),
                    cs.getSubjectCode(),
                    cs.getSubjectName(),
                    cs.getSemester(),
                    formatScore(sc.getAttendanceScore()),
                    formatScore(sc.getMidtermScore()),
                    formatScore(sc.getFinalScore()),
                    formatScore(sc.getTotalScore()),
                    sc.getGrade() != null ? sc.getGrade().name() : "-",
                    result,
                    sc.isLocked() ? "Đã khóa" : "Mở"
            });
        }
        return CSVUtil.exportToCsv(fileName, headers, rows);
    }

    private String formatScore(Double val) {
        return val == null ? "-" : String.format("%.2f", val);
    }
}
