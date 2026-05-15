package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.models.entities.Person;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonDAO extends BaseDAO<Person> {

    @Override
    protected String getTableName() {
        return "persons";
    }

    @Override
    protected Person mapRow(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        return new Person(
                rs.getInt("id"),
                rs.wasNull() ? null : userId,
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("middle_name"),
                rs.getString("email")
        );
    }

    public int save(Person person) {
        String sql = "INSERT INTO persons (user_id, first_name, last_name, middle_name, email) VALUES (?, ?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                if (person.getUserId() != null) {
                    stmt.setInt(1, person.getUserId());
                } else {
                    stmt.setNull(1, java.sql.Types.INTEGER);
                }
                stmt.setString(2, person.getFirstName());
                stmt.setString(3, person.getLastName());
                stmt.setString(4, person.getMiddleName());
                stmt.setString(5, person.getEmail());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения персоны", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    /**
     * Обновляет только личные данные персоны (имя, фамилия, отчество, email).
     * user_id не трогается — он устанавливается один раз при создании
     * и не должен перезаписываться при редактировании профиля.
     */
    public void update(Person person) {
        String sql = """
                UPDATE persons
                SET first_name = ?, last_name = ?, middle_name = ?, email = ?
                WHERE id = ?
                """;
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, person.getFirstName());
                stmt.setString(2, person.getLastName());
                stmt.setString(3, person.getMiddleName());
                stmt.setString(4, person.getEmail());
                stmt.setInt(5, person.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления персоны", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    /**
     * Отдельный метод для привязки user_id к персоне —
     * вызывается только при создании пользователя.
     */
    public void setUserId(int personId, int userId) {
        String sql = "UPDATE persons SET user_id = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setInt(1, userId);
                stmt.setInt(2, personId);
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка привязки user_id к персоне", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }
}
