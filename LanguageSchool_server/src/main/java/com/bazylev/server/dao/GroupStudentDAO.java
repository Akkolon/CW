package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.enums.EnrollmentStatus;
import com.bazylev.server.models.entities.GroupStudent;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GroupStudentDAO extends BaseDAO<GroupStudent> {

    @Override
    protected String getTableName() {
        return "group_students";
    }

    @Override
    protected GroupStudent mapRow(ResultSet rs) throws SQLException {
        Date enrollmentDate = rs.getDate("enrollment_date");
        return new GroupStudent(
                rs.getInt("id"),
                rs.getInt("group_id"),
                rs.getInt("student_id"),
                enrollmentDate != null ? enrollmentDate.toLocalDate() : null,
                EnrollmentStatus.valueOf(rs.getString("status"))
        );
    }

    public int save(GroupStudent gs) {
        String sql = "INSERT INTO group_students (group_id, student_id, enrollment_date, status) VALUES (?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setInt(1, gs.getGroupId());
                stmt.setInt(2, gs.getStudentId());
                stmt.setDate(3, gs.getEnrollmentDate() != null ? Date.valueOf(gs.getEnrollmentDate()) : null);
                stmt.setString(4, gs.getStatus().name());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения зачисления", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void updateStatus(int groupStudentId, EnrollmentStatus status) {
        String sql = "UPDATE group_students SET status = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, status.name());
                stmt.setInt(2, groupStudentId);
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления статуса зачисления", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<GroupStudent> findByGroupId(int groupId) {
        String sql = "SELECT * FROM group_students WHERE group_id = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            List<GroupStudent> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения зачислений группы", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<GroupStudent> findByStudentId(int studentId) {
        String sql = "SELECT * FROM group_students WHERE student_id = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            List<GroupStudent> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения зачислений студента", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public Optional<GroupStudent> findActiveByGroupAndStudent(int groupId, int studentId) {
        String sql = "SELECT * FROM group_students WHERE group_id = ? AND student_id = ? AND status = 'ACTIVE'";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            stmt.setInt(2, studentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска активного зачисления", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
