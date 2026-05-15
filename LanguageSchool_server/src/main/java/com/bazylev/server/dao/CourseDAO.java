package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.models.entities.Course;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CourseDAO extends BaseDAO<Course> {

    @Override
    protected String getTableName() {
        return "courses";
    }

    @Override
    protected Course mapRow(ResultSet rs) throws SQLException {
        return new Course(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("duration_hours"),
                rs.getString("level"),
                rs.getBigDecimal("price_per_month"),
                rs.getBoolean("is_active")
        );
    }

    public int save(Course course) {
        String sql = "INSERT INTO courses (name, description, duration_hours, level, price_per_month, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setString(1, course.getName());
                stmt.setString(2, course.getDescription());
                stmt.setInt(3, course.getDurationHours());
                stmt.setString(4, course.getLevel());
                stmt.setBigDecimal(5, course.getPricePerMonth());
                stmt.setBoolean(6, course.isActive());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения курса", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void update(Course course) {
        String sql = "UPDATE courses SET name = ?, description = ?, duration_hours = ?, level = ?, price_per_month = ?, is_active = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, course.getName());
                stmt.setString(2, course.getDescription());
                stmt.setInt(3, course.getDurationHours());
                stmt.setString(4, course.getLevel());
                stmt.setBigDecimal(5, course.getPricePerMonth());
                stmt.setBoolean(6, course.isActive());
                stmt.setInt(7, course.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления курса", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Course> findAllActive() {
        String sql = "SELECT * FROM courses WHERE is_active = true";
        Connection connection = pool.getConnection();
        try (var stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            List<Course> result = new java.util.ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения активных курсов", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
