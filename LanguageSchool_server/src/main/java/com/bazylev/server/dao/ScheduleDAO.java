package com.bazylev.server.dao;

import com.bazylev.server.dao.base.BaseDAO;
import com.bazylev.server.enums.DayOfWeek;
import com.bazylev.server.models.entities.Schedule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO extends BaseDAO<Schedule> {

    @Override
    protected String getTableName() {
        return "schedule";
    }

    @Override
    protected Schedule mapRow(ResultSet rs) throws SQLException {
        Time startTime = rs.getTime("start_time");
        Time endTime = rs.getTime("end_time");
        return new Schedule(
                rs.getInt("id"),
                rs.getInt("group_id"),
                DayOfWeek.valueOf(rs.getString("day_of_week")),
                startTime != null ? startTime.toLocalTime() : null,
                endTime != null ? endTime.toLocalTime() : null,
                rs.getString("room")
        );
    }

    public int save(Schedule schedule) {
        String sql = "INSERT INTO schedule (group_id, day_of_week, start_time, end_time, room) VALUES (?, ?, ?, ?, ?)";
        Connection connection = pool.getConnection();
        try {
            return executeInsert(connection, sql, stmt -> {
                stmt.setInt(1, schedule.getGroupId());
                stmt.setString(2, schedule.getDayOfWeek().name());
                stmt.setTime(3, schedule.getStartTime() != null ? Time.valueOf(schedule.getStartTime()) : null);
                stmt.setTime(4, schedule.getEndTime() != null ? Time.valueOf(schedule.getEndTime()) : null);
                stmt.setString(5, schedule.getRoom());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения слота расписания", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public void update(Schedule schedule) {
        String sql = "UPDATE schedule SET day_of_week = ?, start_time = ?, end_time = ?, room = ? WHERE id = ?";
        Connection connection = pool.getConnection();
        try {
            executeUpdate(connection, sql, stmt -> {
                stmt.setString(1, schedule.getDayOfWeek().name());
                stmt.setTime(2, schedule.getStartTime() != null ? Time.valueOf(schedule.getStartTime()) : null);
                stmt.setTime(3, schedule.getEndTime() != null ? Time.valueOf(schedule.getEndTime()) : null);
                stmt.setString(4, schedule.getRoom());
                stmt.setInt(5, schedule.getId());
            });
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления слота расписания", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Schedule> findByGroupId(int groupId) {
        String sql = "SELECT * FROM schedule WHERE group_id = ? ORDER BY FIELD(day_of_week,'MON','TUE','WED','THU','FRI','SAT','SUN'), start_time";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            List<Schedule> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения расписания группы", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public List<Schedule> findByTeacherId(int teacherId) {
        String sql = """
                SELECT s.* FROM schedule s
                JOIN `groups` g ON g.id = s.group_id
                WHERE g.teacher_id = ?
                ORDER BY FIELD(s.day_of_week,'MON','TUE','WED','THU','FRI','SAT','SUN'), s.start_time
                """;
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            ResultSet rs = stmt.executeQuery();
            List<Schedule> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения расписания преподавателя", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public boolean hasConflict(int teacherId, DayOfWeek day, java.time.LocalTime start, java.time.LocalTime end, int excludeGroupId) {
        String sql = """
                SELECT COUNT(*) FROM schedule s
                JOIN `groups` g ON g.id = s.group_id
                WHERE g.teacher_id = ?
                  AND s.day_of_week = ?
                  AND s.group_id != ?
                  AND s.start_time < ?
                  AND s.end_time > ?
                """;
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, teacherId);
            stmt.setString(2, day.name());
            stmt.setInt(3, excludeGroupId);
            stmt.setTime(4, Time.valueOf(end));
            stmt.setTime(5, Time.valueOf(start));
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки конфликта расписания", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public boolean isRoomBusy(String room, DayOfWeek day, java.time.LocalTime start, java.time.LocalTime end, int excludeGroupId) {
        String sql = """
                SELECT COUNT(*) FROM schedule
                WHERE room = ?
                  AND day_of_week = ?
                  AND group_id != ?
                  AND start_time < ?
                  AND end_time > ?
                """;
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, room);
            stmt.setString(2, day.name());
            stmt.setInt(3, excludeGroupId);
            stmt.setTime(4, Time.valueOf(end));
            stmt.setTime(5, Time.valueOf(start));
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки занятости аудитории", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

    public int countAllSlots() {
        String sql = "SELECT COUNT(*) FROM schedule";
        Connection connection = pool.getConnection();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка подсчёта слотов расписания", e);
        } finally {
            pool.releaseConnection(connection);
        }
    }

}
