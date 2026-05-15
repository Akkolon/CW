package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.enums.AttendanceStatus;
import com.bazylev.server.models.entities.Attendance;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttendanceDAO extends BaseDAO<Attendance> {

    @Override
    protected String getTableName() {
        return "attendance";
    }

    @Override
    protected Attendance mapRow(ResultSet rs) throws SQLException {
        Date lessonDate = rs.getDate("lesson_date");
        return new Attendance(
                rs.getInt("id"),
                rs.getInt("group_student_id"),
                lessonDate != null ? lessonDate.toLocalDate() : null,
                AttendanceStatus.valueOf(rs.getString("status")),
                rs.getString("comment")
        );
    }

    public int save(Attendance attendance) {
        String sql = "INSERT INTO attendance (group_student_id, lesson_date, status, comment) VALUES (?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setInt(1, attendance.getGroupStudentId());
                stmt.setDate(2, attendance.getLessonDate() != null ? Date.valueOf(attendance.getLessonDate()) : null);
                stmt.setString(3, attendance.getStatus().name());
                stmt.setString(4, attendance.getComment());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения посещаемости", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void update(Attendance attendance) {
        String sql = "UPDATE attendance SET status = ?, comment = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, attendance.getStatus().name());
                stmt.setString(2, attendance.getComment());
                stmt.setInt(3, attendance.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления посещаемости", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Attendance> findByGroupStudentId(int groupStudentId) {
        String sql = "SELECT * FROM attendance WHERE group_student_id = ? ORDER BY lesson_date";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupStudentId);
            ResultSet rs = stmt.executeQuery();
            List<Attendance> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения посещаемости студента", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public Optional<Attendance> findByGroupStudentAndDate(int groupStudentId, LocalDate date) {
        String sql = "SELECT * FROM attendance WHERE group_student_id = ? AND lesson_date = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupStudentId);
            stmt.setDate(2, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска отметки посещаемости", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Attendance> findByGroupIdAndDateRange(int groupId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT a.* FROM attendance a
                JOIN group_students gs ON gs.id = a.group_student_id
                WHERE gs.group_id = ?
                  AND a.lesson_date BETWEEN ? AND ?
                ORDER BY a.lesson_date
                """;
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setDate(2, Date.valueOf(from));
            stmt.setDate(3, Date.valueOf(to));
            ResultSet rs = stmt.executeQuery();
            List<Attendance> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения посещаемости группы за период", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
