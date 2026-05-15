package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.enums.GradeType;
import com.bazylev.server.models.entities.Grade;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

public class GradeDAO extends BaseDAO<Grade> {

    @Override
    protected String getTableName() {
        return "grades";
    }

    @Override
    protected Grade mapRow(ResultSet rs) throws SQLException {
        Date gradeDate = rs.getDate("grade_date");
        String gradeValue = rs.getString("grade_value");
        double value = 0;
        if (gradeValue != null && !gradeValue.isBlank()) {
            try {
                value = Double.parseDouble(gradeValue);
            } catch (NumberFormatException ignored) {}
        }
        return new Grade(
                rs.getInt("id"),
                rs.getInt("group_student_id"),
                gradeDate != null ? gradeDate.toLocalDate() : null,
                value,
                GradeType.valueOf(rs.getString("grade_type")),
                null
        );
    }

    public int save(Grade grade) {
        String sql = "INSERT INTO grades (group_student_id, grade_date, grade_value, grade_type) VALUES (?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setInt(1, grade.getGroupStudentId());
                stmt.setDate(2, grade.getGradeDate() != null ? Date.valueOf(grade.getGradeDate()) : null);
                stmt.setString(3, String.valueOf(grade.getValue()));
                stmt.setString(4, grade.getGradeType().name());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения оценки", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Grade> findByGroupStudentId(int groupStudentId) {
        String sql = "SELECT * FROM grades WHERE group_student_id = ? ORDER BY grade_date";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupStudentId);
            ResultSet rs = stmt.executeQuery();
            List<Grade> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения оценок студента", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public OptionalDouble calcAverageByGroupStudent(int groupStudentId) {
        String sql = "SELECT AVG(CAST(grade_value AS DECIMAL(5,2))) FROM grades WHERE group_student_id = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupStudentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double avg = rs.getDouble(1);
                return rs.wasNull() ? OptionalDouble.empty() : OptionalDouble.of(avg);
            }
            return OptionalDouble.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка расчёта среднего балла", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Grade> findByGroupIdAndDateRange(int groupId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT g.* FROM grades g
                JOIN group_students gs ON gs.id = g.group_student_id
                WHERE gs.group_id = ?
                  AND g.grade_date BETWEEN ? AND ?
                ORDER BY g.grade_date
                """;
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setDate(2, Date.valueOf(from));
            stmt.setDate(3, Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            List<Grade> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения оценок группы за период", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void update(Grade grade) {
        String sql = """
            UPDATE grades
            SET grade_date = ?, grade_value = ?, grade_type = ?
            WHERE id = ?
            """;
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setDate(1, grade.getGradeDate() != null
                        ? java.sql.Date.valueOf(grade.getGradeDate()) : null);
                stmt.setString(2, String.valueOf(grade.getValue()));
                stmt.setString(3, grade.getGradeType().name());
                stmt.setInt(4, grade.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления оценки", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
