package com.bazylev.server.services;

import com.bazylev.server.dao.GroupDAO;
import com.bazylev.server.dao.GroupStudentDAO;
import com.bazylev.server.dao.ScheduleDAO;
import com.bazylev.server.db.ConnectionPool;
import com.bazylev.server.enums.DayOfWeek;
import com.bazylev.server.enums.GroupStatus;
import com.bazylev.server.models.entities.Group;
import com.bazylev.server.models.entities.Schedule;
import com.bazylev.server.models.tcp.Response;
import com.bazylev.server.network.GsonFactory;
import com.bazylev.server.network.Session;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScheduleService {

    private static final DayOfWeek[] WORKDAYS =
            { DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED,
                    DayOfWeek.THU, DayOfWeek.FRI };

    private static final LocalTime[][] TIME_SLOTS = {
            { LocalTime.of(8,  0), LocalTime.of(9,  30) },
            { LocalTime.of(9,  45), LocalTime.of(11, 15) },
            { LocalTime.of(11, 30), LocalTime.of(13,  0) },
            { LocalTime.of(13, 30), LocalTime.of(15,  0) },
            { LocalTime.of(15, 15), LocalTime.of(16, 45) },
            { LocalTime.of(17,  0), LocalTime.of(18, 30) },
            { LocalTime.of(18, 45), LocalTime.of(20, 15) },
    };

    private static final String[] ROOMS = { "101", "102", "103", "104", "105" };

    private static final int DEFAULT_LESSONS_PER_WEEK = 2;
    private static final int MAX_LESSONS_PER_WEEK     = 5;

    private final ScheduleDAO    scheduleDAO = new ScheduleDAO();
    private final GroupDAO       groupDAO    = new GroupDAO();
    private final ConnectionPool pool        = ConnectionPool.getInstance();
    private final Gson           gson        = GsonFactory.getInstance();
    private final GroupStudentDAO groupStudentDAO = new GroupStudentDAO();

    public Response getSchedule(String data, Session session) {
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            int groupId = json.has("groupId") ? json.get("groupId").getAsInt() : -1;
            if (groupId < 0) return Response.error("Не указан groupId");
            return Response.ok(gson.toJson(scheduleDAO.findByGroupId(groupId)));
        } catch (Exception e) {
            return Response.error("Некорректный запрос: " + e.getMessage());
        }
    }

    public Response getScheduleStats() {
        List<Group> allActive = groupDAO.findAll().stream()
                .filter(g -> g.getStatus() == GroupStatus.IN_PROGRESS)
                .toList();

        int totalSlots      = WORKDAYS.length * TIME_SLOTS.length * ROOMS.length;
        int usedSlots       = scheduleDAO.countAllSlots();
        int groupsWithSched = 0;
        int groupsWithout   = 0;

        JsonArray groupStats = new JsonArray();
        for (Group g : allActive) {
            int count = scheduleDAO.findByGroupId(g.getId()).size();
            if (count > 0) groupsWithSched++;
            else           groupsWithout++;

            JsonObject item = new JsonObject();
            item.addProperty("groupId",   g.getId());
            item.addProperty("groupName", g.getName());
            item.addProperty("slots",     count);
            groupStats.add(item);
        }

        JsonObject result = new JsonObject();
        result.addProperty("totalSlots",       totalSlots);
        result.addProperty("usedSlots",        usedSlots);
        result.addProperty("freeSlots",        totalSlots - usedSlots);
        result.addProperty("activeGroups",     allActive.size());
        result.addProperty("groupsWithSched",  groupsWithSched);
        result.addProperty("groupsWithout",    groupsWithout);
        result.addProperty("daysCount",        WORKDAYS.length);
        result.addProperty("timeSlotsCount",   TIME_SLOTS.length);
        result.addProperty("roomsCount",       ROOMS.length);
        result.add("groupStats", groupStats);
        return Response.ok(result.toString());
    }

    public Response generateSchedule(String data) {
        int lessonsPerWeek = DEFAULT_LESSONS_PER_WEEK;
        boolean overwrite  = false;

        if (data != null && !data.isBlank()) {
            try {
                JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                if (json.has("lessonsPerWeek")) {
                    lessonsPerWeek = Math.min(
                            json.get("lessonsPerWeek").getAsInt(),
                            MAX_LESSONS_PER_WEEK);
                    lessonsPerWeek = Math.max(lessonsPerWeek, 1);
                }
                if (json.has("overwrite")) {
                    overwrite = json.get("overwrite").getAsBoolean();
                }
            } catch (Exception ignored) {}
        }

        List<Group> groups = new ArrayList<>(groupDAO.findAll().stream()
                .filter(g -> g.getStatus() == GroupStatus.IN_PROGRESS)
                .toList());

        // перемешиваем — случайный порядок убирает преимущество первых групп
        Collections.shuffle(groups);

        List<String> errors    = new ArrayList<>();
        List<String> succeeded = new ArrayList<>();
        List<String> skipped   = new ArrayList<>();

        Connection connection = pool.getConnection();
        try {
            connection.setAutoCommit(false);

            for (Group group : groups) {
                List<Schedule> existing = scheduleDAO.findByGroupId(group.getId());

                if (!existing.isEmpty() && !overwrite) {
                    skipped.add("Группа " + group.getName()
                            + " (id=" + group.getId() + "): уже есть расписание");
                    continue;
                }

                if (!existing.isEmpty()) {
                    for (Schedule s : existing) {
                        scheduleDAO.delete(s.getId());
                    }
                }

                List<int[]> placed = placeSlots(group, lessonsPerWeek);

                if (placed.size() == lessonsPerWeek) {
                    for (int[] s : placed) {
                        Schedule schedule = new Schedule();
                        schedule.setGroupId(group.getId());
                        schedule.setDayOfWeek(WORKDAYS[s[0]]);
                        schedule.setStartTime(TIME_SLOTS[s[1]][0]);
                        schedule.setEndTime(TIME_SLOTS[s[1]][1]);
                        schedule.setRoom(ROOMS[s[2]]);
                        scheduleDAO.save(schedule);
                    }
                    succeeded.add("Группа " + group.getName()
                            + " (id=" + group.getId() + "): "
                            + placed.size() + " занятий");
                } else {
                    errors.add("Группа " + group.getName()
                            + " (id=" + group.getId() + "): удалось разместить "
                            + placed.size() + " из " + lessonsPerWeek + " занятий");
                    // сохраняем то, что нашли, если хоть что-то есть
                    for (int[] s : placed) {
                        Schedule schedule = new Schedule();
                        schedule.setGroupId(group.getId());
                        schedule.setDayOfWeek(WORKDAYS[s[0]]);
                        schedule.setStartTime(TIME_SLOTS[s[1]][0]);
                        schedule.setEndTime(TIME_SLOTS[s[1]][1]);
                        schedule.setRoom(ROOMS[s[2]]);
                        scheduleDAO.save(schedule);
                    }
                }
            }

            connection.commit();
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { /* ignore */ }
            return Response.error("Ошибка генерации расписания: " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { /* ignore */ }
            pool.releaseConnection(connection);
        }

        JsonObject result = new JsonObject();
        result.add("succeeded", gson.toJsonTree(succeeded));
        result.add("errors",    gson.toJsonTree(errors));
        result.add("skipped",   gson.toJsonTree(skipped));
        return Response.ok(result.toString());
    }

    private List<int[]> placeSlots(Group group, int needed) {
        List<int[]> placed = new ArrayList<>();

        List<int[]> candidates = new ArrayList<>();
        for (int d = 0; d < WORKDAYS.length; d++) {
            for (int t = 0; t < TIME_SLOTS.length; t++) {
                candidates.add(new int[]{ d, t });
            }
        }
        Collections.shuffle(candidates);

        for (int[] candidate : candidates) {
            if (placed.size() == needed) break;

            int dayIdx  = candidate[0];
            int slotIdx = candidate[1];

            DayOfWeek  day   = WORKDAYS[dayIdx];
            LocalTime  start = TIME_SLOTS[slotIdx][0];
            LocalTime  end   = TIME_SLOTS[slotIdx][1];

            boolean dayAlreadyUsed = placed.stream()
                    .anyMatch(p -> p[0] == dayIdx);
            if (dayAlreadyUsed) continue;

            if (scheduleDAO.hasConflict(
                    group.getTeacherId(), day, start, end, group.getId())) {
                continue;
            }

            String room = findFreeRoom(day, start, end, group.getId());
            if (room == null) continue;

            int roomIdx = roomIndex(room);
            placed.add(new int[]{ dayIdx, slotIdx, roomIdx });
        }

        return placed;
    }

    private String findFreeRoom(DayOfWeek day, LocalTime start,
                                LocalTime end, int excludeGroupId) {
        for (String room : ROOMS) {
            if (!scheduleDAO.isRoomBusy(room, day, start, end, excludeGroupId)) {
                return room;
            }
        }
        return null;
    }

    private int roomIndex(String room) {
        for (int i = 0; i < ROOMS.length; i++) {
            if (ROOMS[i].equals(room)) return i;
        }
        return 0;
    }

    public Response updateSlot(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("Данные слота не переданы");
        }
        JsonObject json;
        try {
            json = JsonParser.parseString(data).getAsJsonObject();
        } catch (Exception e) {
            return Response.error("Некорректный формат данных");
        }

        int slotId = json.has("id") ? json.get("id").getAsInt() : -1;
        if (slotId < 0) return Response.error("Не указан ID слота расписания");

        Schedule slot = scheduleDAO.findById(slotId).orElse(null);
        if (slot == null) return Response.error("Слот расписания не найден: " + slotId);

        if (json.has("dayOfWeek")) {
            try {
                slot.setDayOfWeek(DayOfWeek.valueOf(
                        json.get("dayOfWeek").getAsString().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return Response.error("Неизвестный день недели");
            }
        }
        if (json.has("startTime"))
            slot.setStartTime(LocalTime.parse(json.get("startTime").getAsString()));
        if (json.has("endTime"))
            slot.setEndTime(LocalTime.parse(json.get("endTime").getAsString()));
        if (json.has("room"))
            slot.setRoom(json.get("room").getAsString());

        Group group = groupDAO.findById(slot.getGroupId()).orElse(null);
        if (group != null && scheduleDAO.hasConflict(
                group.getTeacherId(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime(), slot.getGroupId())) {
            return Response.error("Конфликт расписания: преподаватель занят в это время");
        }

        scheduleDAO.update(slot);
        return Response.ok(gson.toJson(slot));
    }

    public Response getScheduleForStudent(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("studentId не передан");
        }
        int studentId;
        try {
            studentId = JsonParser.parseString(data)
                    .getAsJsonObject().get("studentId").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный studentId");
        }

        List<com.bazylev.server.models.entities.GroupStudent> enrollments =
                groupStudentDAO.findByStudentId(studentId);

        List<com.bazylev.server.models.entities.Schedule> result = new java.util.ArrayList<>();
        for (com.bazylev.server.models.entities.GroupStudent gs : enrollments) {
            if (gs.getStatus() != com.bazylev.server.enums.EnrollmentStatus.ACTIVE) continue;
            result.addAll(scheduleDAO.findByGroupId(gs.getGroupId()));
        }
        return Response.ok(gson.toJson(result));
    }

    public Response deleteSlot(String data) {
        if (data == null || data.isBlank()) {
            return Response.error("ID слота не передан");
        }
        int slotId;
        try {
            slotId = JsonParser.parseString(data)
                    .getAsJsonObject().get("slotId").getAsInt();
        } catch (Exception e) {
            return Response.error("Некорректный ID слота");
        }

        if (scheduleDAO.findById(slotId).isEmpty()) {
            return Response.error("Слот расписания не найден: " + slotId);
        }

        scheduleDAO.delete(slotId);
        return Response.ok();
    }
}