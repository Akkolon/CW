package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.enums.GroupStatus;
import com.bazylev.server.models.entities.Group;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO extends BaseDAO<Group> {

    @Override
    protected String getTableName() {
        return "`groups`";
    }

    @Override
    protected Group mapRow(ResultSet rs) throws SQLException {
        Date startDate = rs.getDate("start_date");
        Date endDate = rs.getDate("end_date");
        return new Group(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("course_id"),
                rs.getInt("teacher_id"),
                startDate != null ? startDate.toLocalDate() : null,
                endDate != null ? endDate.toLocalDate() : null,
                rs.getInt("max_students"),
                GroupStatus.valueOf(rs.getString("status"))
        );
    }

    public int save(Group group) {
        String sql = "INSERT INTO `groups` (name, course_id, teacher_id, start_date, end_date, max_students, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setString(1, group.getName());
                stmt.setInt(2, group.getCourseId());
                stmt.setInt(3, group.getTeacherId());
                stmt.setDate(4, group.getStartDate() != null ? Date.valueOf(group.getStartDate()) : null);
                stmt.setDate(5, group.getEndDate() != null ? Date.valueOf(group.getEndDate()) : null);
                stmt.setInt(6, group.getMaxStudents());
                stmt.setString(7, group.getStatus().name());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения группы", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void update(Group group) {
        String sql = "UPDATE `groups` SET name = ?, course_id = ?, teacher_id = ?, start_date = ?, end_date = ?, max_students = ?, status = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, group.getName());
                stmt.setInt(2, group.getCourseId());
                stmt.setInt(3, group.getTeacherId());
                stmt.setDate(4, group.getStartDate() != null ? Date.valueOf(group.getStartDate()) : null);
                stmt.setDate(5, group.getEndDate() != null ? Date.valueOf(group.getEndDate()) : null);
                stmt.setInt(6, group.getMaxStudents());
                stmt.setString(7, group.getStatus().name());
                stmt.setInt(8, group.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления группы", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Group> findByTeacherId(int teacherId) {
        String sql = "SELECT * FROM `groups` WHERE teacher_id = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            ResultSet rs = stmt.executeQuery();
            List<Group> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения групп преподавателя", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public int countActiveStudents(int groupId) {
        String sql = "SELECT COUNT(*) FROM group_students WHERE group_id = ? AND status = 'ACTIVE'";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка подсчёта студентов группы", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
