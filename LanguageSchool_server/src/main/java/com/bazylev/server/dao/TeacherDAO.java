package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.models.entities.Teacher;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TeacherDAO extends BaseDAO<Teacher> {

    @Override
    protected String getTableName() {
        return "teachers";
    }

    @Override
    protected Teacher mapRow(ResultSet rs) throws SQLException {
        Date hireDate = rs.getDate("hire_date");
        return new Teacher(
                rs.getInt("id"),
                rs.getInt("person_id"),
                rs.getString("specialization"),
                hireDate != null ? hireDate.toLocalDate() : null
        );
    }

    public int save(Teacher teacher) {
        String sql = "INSERT INTO teachers (person_id, specialization, hire_date) VALUES (?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setInt(1, teacher.getPersonId());
                stmt.setString(2, teacher.getSpecialization());
                stmt.setDate(3, teacher.getHireDate() != null ? Date.valueOf(teacher.getHireDate()) : null);
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения преподавателя", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void update(Teacher teacher) {
        String sql = "UPDATE teachers SET specialization = ?, hire_date = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, teacher.getSpecialization());
                stmt.setDate(2, teacher.getHireDate() != null ? Date.valueOf(teacher.getHireDate()) : null);
                stmt.setInt(3, teacher.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления преподавателя", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
