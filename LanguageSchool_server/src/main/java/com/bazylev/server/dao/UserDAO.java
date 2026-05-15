package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.enums.Role;
import com.bazylev.server.models.entities.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDAO extends BaseDAO<User> {

    @Override
    protected String getTableName() {
        return "users";
    }

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("login"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getBoolean("blocked"),
                rs.getInt("person_id")
        );
    }

    public int save(User user) {
        String sql = "INSERT INTO users (login, password_hash, role, blocked, person_id) VALUES (?, ?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setString(1, user.getLogin());
                stmt.setString(2, user.getPasswordHash());
                stmt.setString(3, user.getRole().name());
                stmt.setBoolean(4, user.isBlocked());
                stmt.setInt(5, user.getPersonId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения пользователя", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void update(User user) {
        String sql = "UPDATE users SET login = ?, password_hash = ?, role = ?, blocked = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, user.getLogin());
                stmt.setString(2, user.getPasswordHash());
                stmt.setString(3, user.getRole().name());
                stmt.setBoolean(4, user.isBlocked());
                stmt.setInt(5, user.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления пользователя", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public Optional<User> findByLogin(String login) {
        String sql = "SELECT * FROM users WHERE login = ?";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по логину", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void setBlocked(int userId, boolean blocked) {
        String sql = "UPDATE users SET blocked = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setBoolean(1, blocked);
                stmt.setInt(2, userId);
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка блокировки/разблокировки пользователя", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
