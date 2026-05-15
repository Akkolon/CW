package com.bazylev.server.dao.base;

import com.bazylev.server.db.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class BaseDAO<T> {

    protected ConnectionPool pool = ConnectionPool.getInstance();

    protected abstract T mapRow(ResultSet rs) throws SQLException;

    protected abstract String getTableName();

    public Optional<T> findById(int id) {
        String sql = "SELECT * FROM " + getTableName() + " WHERE id = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка findById в таблице " + getTableName(), e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<T> findAll() {
        String sql = "SELECT * FROM " + getTableName();
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            List<T> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка findAll в таблице " + getTableName(), e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM " + getTableName() + " WHERE id = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка delete в таблице " + getTableName(), e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    protected int executeInsert(Connection connection, String sql, StatementFiller filler) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            filler.fill(stmt);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
            throw new SQLException("Не удалось получить сгенерированный ID");
        }
    }

    protected void executeUpdate(Connection connection, String sql, StatementFiller filler) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            filler.fill(stmt);
            stmt.executeUpdate();
        }
    }

    @FunctionalInterface
    public interface StatementFiller {
        void fill(PreparedStatement stmt) throws SQLException;
    }
}
