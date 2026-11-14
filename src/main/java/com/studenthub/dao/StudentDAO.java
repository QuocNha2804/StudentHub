package com.studenthub.dao;

import com.studenthub.config.DBConnection;
import com.studenthub.exception.AppException;
import com.studenthub.model.Student;
import com.studenthub.model.enums.Gender;
import com.studenthub.model.enums.StudentStatus;
import com.studenthub.util.LogUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentDAO {

    private static final String BASE_SELECT_WITH_GPA = """
            SELECT s.*, c.class_code, c.name AS class_name,
                   ROUND(SUM(CASE WHEN sc.total_score IS NOT NULL THEN sc.total_score * sub.credits ELSE 0 END) /
                         NULLIF(SUM(CASE WHEN sc.total_score IS NOT NULL THEN sub.credits ELSE 0 END), 0), 2) AS gpa10,
                   ROUND(SUM(CASE sc.grade
                               WHEN 'A' THEN 4.0 * sub.credits
                               WHEN 'B' THEN 3.0 * sub.credits
                               WHEN 'C' THEN 2.0 * sub.credits
                               WHEN 'D' THEN 1.0 * sub.credits
                               WHEN 'F' THEN 0.0
                               ELSE 0 END) /
                         NULLIF(SUM(CASE WHEN sc.grade IS NOT NULL THEN sub.credits ELSE 0 END), 0), 2) AS gpa4
            FROM students s
            LEFT JOIN classes c ON s.class_id = c.id
            LEFT JOIN scores sc ON s.id = sc.student_id
            LEFT JOIN class_subject cs ON sc.class_subject_id = cs.id
            LEFT JOIN subjects sub ON cs.subject_id = sub.id
            """;

    public int countActiveSearch(String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM students s
                LEFT JOIN classes c ON s.class_id = c.id
                WHERE s.deleted_at IS NULL
                """);
        boolean hasKey = keyword != null && !keyword.trim().isEmpty();
        if (hasKey) {
            sql.append(" AND (s.student_code LIKE ? OR s.full_name LIKE ? OR c.class_code LIKE ?)");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (hasKey) {
                String pattern = "%" + keyword.trim() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.countActiveSearch", e);
            throw new AppException("Lỗi đếm số lượng sinh viên.", e);
        }
    }

    public List<Student> searchAndPaginate(String keyword, String sortBy, boolean asc, int page, int pageSize) {
        StringBuilder sql = new StringBuilder(BASE_SELECT_WITH_GPA);
        sql.append(" WHERE s.deleted_at IS NULL ");

        boolean hasKey = keyword != null && !keyword.trim().isEmpty();
        if (hasKey) {
            sql.append(" AND (s.student_code LIKE ? OR s.full_name LIKE ? OR c.class_code LIKE ?) ");
        }

        sql.append(" GROUP BY s.id ");

        String orderColumn = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "name" -> "s.full_name";
            case "gpa" -> "COALESCE(gpa10, -1)";
            default -> "s.student_code";
        };
        String dir = asc ? "ASC" : "DESC";
        sql.append(" ORDER BY ").append(orderColumn).append(" ").append(dir).append(", s.id ASC ");
        sql.append(" LIMIT ? OFFSET ? ");

        List<Student> list = new ArrayList<>();
        int offset = Math.max(0, (page - 1) * pageSize);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasKey) {
                String pattern = "%" + keyword.trim() + "%";
                ps.setString(idx++, pattern);
                ps.setString(idx++, pattern);
                ps.setString(idx++, pattern);
            }
            ps.setInt(idx++, pageSize);
            ps.setInt(idx, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.searchAndPaginate", e);
            throw new AppException("Lỗi truy vấn danh sách sinh viên.", e);
        }
        return list;
    }

    public List<Student> findAllActive() {
        String sql = BASE_SELECT_WITH_GPA + " WHERE s.deleted_at IS NULL GROUP BY s.id ORDER BY s.student_code ASC";
        List<Student> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.findAllActive", e);
            throw new AppException("Lỗi lấy toàn bộ danh sách sinh viên.", e);
        }
        return list;
    }

    public List<Student> findByClassId(int classId) {
        String sql = BASE_SELECT_WITH_GPA + " WHERE s.deleted_at IS NULL AND s.class_id = ? GROUP BY s.id ORDER BY s.student_code ASC";
        List<Student> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.findByClassId", e);
            throw new AppException("Lỗi lấy danh sách sinh viên theo lớp.", e);
        }
        return list;
    }

    public Optional<Student> findById(int id) {
        String sql = BASE_SELECT_WITH_GPA + " WHERE s.id = ? AND s.deleted_at IS NULL GROUP BY s.id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.findById", e);
            throw new AppException("Lỗi tìm sinh viên theo ID.", e);
        }
        return Optional.empty();
    }

    public Optional<Student> findByCode(String studentCode) {
        String sql = BASE_SELECT_WITH_GPA + " WHERE s.student_code = ? GROUP BY s.id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.findByCode", e);
            throw new AppException("Lỗi tìm sinh viên theo mã SV.", e);
        }
        return Optional.empty();
    }

    public Optional<Student> findByEmail(String email) {
        String sql = BASE_SELECT_WITH_GPA + " WHERE s.email = ? GROUP BY s.id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.findByEmail", e);
            throw new AppException("Lỗi tìm sinh viên theo email.", e);
        }
        return Optional.empty();
    }

    public Optional<Student> findByUserId(int userId) {
        String sql = BASE_SELECT_WITH_GPA + " WHERE s.user_id = ? AND s.deleted_at IS NULL GROUP BY s.id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.findByUserId", e);
            throw new AppException("Lỗi tìm hồ sơ sinh viên theo User ID.", e);
        }
        return Optional.empty();
    }

    public int insert(Connection conn, Student student) throws SQLException {
        String sql = """
                INSERT INTO students (student_code, full_name, dob, gender, email, phone, address, class_id, user_id, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, student.getStudentCode());
            ps.setString(2, student.getFullName());
            ps.setDate(3, Date.valueOf(student.getDob()));
            ps.setString(4, student.getGender().name());
            ps.setString(5, student.getEmail());
            ps.setString(6, student.getPhone());
            ps.setString(7, student.getAddress());
            if (student.getClassId() != null) {
                ps.setInt(8, student.getClassId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            if (student.getUserId() != null) {
                ps.setInt(9, student.getUserId());
            } else {
                ps.setNull(9, Types.INTEGER);
            }
            ps.setString(10, student.getStatus() != null ? student.getStatus().name() : StudentStatus.ACTIVE.name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    student.setId(id);
                    return id;
                }
            }
        }
        return 0;
    }

    public void update(Student student) {
        String sql = """
                UPDATE students
                SET full_name = ?, dob = ?, gender = ?, email = ?, phone = ?, address = ?, class_id = ?, status = ?
                WHERE id = ? AND deleted_at IS NULL
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getFullName());
            ps.setDate(2, Date.valueOf(student.getDob()));
            ps.setString(3, student.getGender().name());
            ps.setString(4, student.getEmail());
            ps.setString(5, student.getPhone());
            ps.setString(6, student.getAddress());
            if (student.getClassId() != null) {
                ps.setInt(7, student.getClassId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, student.getStatus().name());
            ps.setInt(9, student.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.update", e);
            throw new AppException("Lỗi cập nhật thông tin sinh viên: " + e.getMessage(), e);
        }
    }

    public void updateClass(int studentId, Integer newClassId) {
        String sql = "UPDATE students SET class_id = ? WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (newClassId != null) {
                ps.setInt(1, newClassId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, studentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.updateClass", e);
            throw new AppException("Lỗi chuyển lớp cho sinh viên.", e);
        }
    }

    public void softDelete(int studentId, Integer userId) {
        String sqlStudent = "UPDATE students SET status = 'INACTIVE', deleted_at = CURRENT_TIMESTAMP WHERE id = ?";
        String sqlUser = "UPDATE users SET is_active = FALSE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(sqlStudent)) {
                ps1.setInt(1, studentId);
                ps1.executeUpdate();

                if (userId != null) {
                    try (PreparedStatement ps2 = conn.prepareStatement(sqlUser)) {
                        ps2.setInt(1, userId);
                        ps2.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                DBConnection.rollbackQuietly(conn);
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LogUtil.error("Lỗi StudentDAO.softDelete", e);
            throw new AppException("Lỗi xóa mềm sinh viên.", e);
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        int classIdVal = rs.getInt("class_id");
        Integer classId = rs.wasNull() ? null : classIdVal;

        int userIdVal = rs.getInt("user_id");
        Integer userId = rs.wasNull() ? null : userIdVal;

        double gpa10Val = rs.getDouble("gpa10");
        Double gpa10 = rs.wasNull() ? null : gpa10Val;

        double gpa4Val = rs.getDouble("gpa4");
        Double gpa4 = rs.wasNull() ? null : gpa4Val;

        Date dobDate = rs.getDate("dob");
        Timestamp deletedTs = rs.getTimestamp("deleted_at");
        Timestamp createdTs = rs.getTimestamp("created_at");

        return Student.builder()
                .id(rs.getInt("id"))
                .studentCode(rs.getString("student_code"))
                .fullName(rs.getString("full_name"))
                .dob(dobDate != null ? dobDate.toLocalDate() : null)
                .gender(Gender.valueOf(rs.getString("gender")))
                .email(rs.getString("email"))
                .phone(rs.getString("phone"))
                .address(rs.getString("address"))
                .classId(classId)
                .classCode(rs.getString("class_code"))
                .className(rs.getString("class_name"))
                .userId(userId)
                .status(StudentStatus.valueOf(rs.getString("status")))
                .deletedAt(deletedTs != null ? deletedTs.toLocalDateTime() : null)
                .createdAt(createdTs != null ? createdTs.toLocalDateTime() : null)
                .gpa10(gpa10)
                .gpa4(gpa4)
                .build();
    }
}
