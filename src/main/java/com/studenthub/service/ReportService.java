package com.studenthub.service;

import com.studenthub.dao.ReportDAO;
import com.studenthub.dao.ScoreDAO;
import com.studenthub.dto.StudentGpaDTO;
import com.studenthub.dto.SubjectPassRateDTO;
import com.studenthub.model.Score;
import com.studenthub.util.CSVUtil;

import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private final ReportDAO reportDAO = new ReportDAO();
    private final ScoreDAO scoreDAO = new ScoreDAO();

    public List<String[]> getStudentCountByClass() {
        return reportDAO.countStudentsByClass();
    }

    public List<String[]> getStudentCountByGender() {
        return reportDAO.countStudentsByGender();
    }

    public List<StudentGpaDTO> getTop10GpaSchoolWide() {
        return reportDAO.findTopStudentsByGpa(null, true, 10);
    }

    public List<StudentGpaDTO> getTop5ClassGpa(int classId, boolean highestFirst) {
        return reportDAO.findTopStudentsByGpa(classId, highestFirst, 5);
    }

    public List<SubjectPassRateDTO> getPassRateBySubject() {
        return reportDAO.getPassRateBySubject();
    }

    /**
     * Tính thống kê Đạt / Rớt cho 1 lớp học phần cụ thể của giảng viên.
     */
    public String[] getCoursePassFailSummary(int classSubjectId) {
        List<Score> scores = scoreDAO.findScoresByClassSubject(classSubjectId);
        int totalStudents = scores.size();
        int gradedCount = 0;
        int passed = 0;
        int failed = 0;
        int countA = 0, countB = 0, countC = 0, countD = 0, countF = 0;
        double sumTotal = 0.0;

        for (Score sc : scores) {
            if (sc.getTotalScore() != null && sc.getGrade() != null) {
                gradedCount++;
                sumTotal += sc.getTotalScore();
                if (sc.getGrade().isPassed()) {
                    passed++;
                } else {
                    failed++;
                }
                switch (sc.getGrade()) {
                    case A -> countA++;
                    case B -> countB++;
                    case C -> countC++;
                    case D -> countD++;
                    case F -> countF++;
                }
            }
        }

        double avg = gradedCount > 0 ? sumTotal / gradedCount : 0.0;
        double passRate = gradedCount > 0 ? (passed * 100.0) / gradedCount : 0.0;
        double failRate = gradedCount > 0 ? (failed * 100.0) / gradedCount : 0.0;

        return new String[]{
                String.valueOf(totalStudents),
                String.valueOf(gradedCount),
                String.format("%.2f", avg),
                String.format("%d (%.1f%%)", passed, passRate),
                String.format("%d (%.1f%%)", failed, failRate),
                String.format("A:%d | B:%d | C:%d | D:%d | F:%d", countA, countB, countC, countD, countF)
        };
    }

    public String exportTop10GpaToCsv(String fileName) {
        List<StudentGpaDTO> list = getTop10GpaSchoolWide();
        String[] headers = {"Hạng", "Mã SV", "Họ và Tên", "Lớp", "Giới tính", "Tổng Tín chỉ", "GPA (Hệ 10)", "GPA (Hệ 4)", "Xếp loại"};
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            StudentGpaDTO d = list.get(i);
            rows.add(new String[]{
                    String.valueOf(i + 1),
                    d.getStudentCode(),
                    d.getFullName(),
                    d.getClassCode(),
                    d.getGender(),
                    String.valueOf(d.getTotalCredits()),
                    String.format("%.2f", d.getGpa10()),
                    String.format("%.2f", d.getGpa4()),
                    d.getAcademicRank()
            });
        }
        return CSVUtil.exportToCsv(fileName, headers, rows);
    }

    public String exportSubjectPassRateToCsv(String fileName) {
        List<SubjectPassRateDTO> list = getPassRateBySubject();
        String[] headers = {"Mã Môn", "Tên Môn", "Số TC", "Số SV đã chấm", "Số SV Đạt", "Số SV Rớt", "Tỉ lệ Đạt (%)", "Điểm TB"};
        List<String[]> rows = new ArrayList<>();
        for (SubjectPassRateDTO d : list) {
            rows.add(new String[]{
                    d.getSubjectCode(),
                    d.getSubjectName(),
                    String.valueOf(d.getCredits()),
                    String.valueOf(d.getTotalGradedStudents()),
                    String.valueOf(d.getPassedCount()),
                    String.valueOf(d.getFailedCount()),
                    String.format("%.2f%%", d.getPassRatePercent()),
                    String.format("%.2f", d.getAverageScore())
            });
        }
        return CSVUtil.exportToCsv(fileName, headers, rows);
    }

    public String exportClassStatsToCsv(String fileName) {
        List<String[]> rows = getStudentCountByClass();
        String[] headers = {"Mã Lớp", "Tên Lớp", "Niên khóa", "GV Chủ nhiệm", "Tổng SV", "SV Nam", "SV Nữ"};
        return CSVUtil.exportToCsv(fileName, headers, rows);
    }
}
