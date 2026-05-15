package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.models.entities.Student;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO extends BaseDAO<Student> {

    @Override
    protected String getTableName() {
        return "students";
    }

    @Override
    protected Student mapRow(ResultSet rs) throws SQLException {
        Date enrollmentDate = rs.getDate("enrollment_date");
        return new Student(
                rs.getInt("id"),
                rs.getInt("person_id"),
                enrollmentDate != null ? enrollmentDate.toLocalDate() : null,
                "ACTIVE".equals(rs.getString("status"))
        );
    }

    public int save(Student student) {
        String sql = "INSERT INTO students (person_id, enrollment_date, status) VALUES (?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setInt(1, student.getPersonId());
                stmt.setDate(2, student.getEnrollmentDate() != null ? Date.valueOf(student.getEnrollmentDate()) : null);
                stmt.setString(3, student.isActive() ? "ACTIVE" : "INACTIVE");
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения студента", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void update(Student student) {
        String sql = "UPDATE students SET status = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, student.isActive() ? "ACTIVE" : "INACTIVE");
                stmt.setInt(2, student.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления студента", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Student> findByGroupId(int groupId) {
        String sql = """
                SELECT s.* FROM students s
                JOIN group_students gs ON gs.student_id = s.id
                WHERE gs.group_id = ? AND gs.status = 'ACTIVE'
                """;
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            List<Student> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения студентов группы", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
