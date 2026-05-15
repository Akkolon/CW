package com.bazylev.server.enums;

public enum RequestType {

    // --- Аутентификация ---
    LOGIN,
    LOGOUT,

    // --- Пользователи (Admin) ---
    CREATE_USER,
    UPDATE_USER,
    DELETE_USER,
    BLOCK_USER,
    GET_ALL_USERS,
    GET_ACTION_LOG,

    // --- Персоны ---
    GET_ALL_PERSONS,

    // --- Справочник курсов ---
    GET_ALL_COURSES,
    CREATE_COURSE,
    UPDATE_COURSE,
    DELETE_COURSE,

    // --- Преподаватели ---
    GET_ALL_TEACHERS,
    CREATE_TEACHER,
    UPDATE_TEACHER,
    DELETE_TEACHER,

    // --- Студенты ---
    GET_ALL_STUDENTS,
    CREATE_STUDENT,
    REGISTER_STUDENT,
    UPDATE_STUDENT,
    GET_STUDENT_HISTORY,
    DELETE_STUDENT,
    GET_STUDENTS_BY_GROUP,

    // --- Группы ---
    GET_ALL_GROUPS,
    CREATE_GROUP,
    UPDATE_GROUP,

    // --- Зачисление в группу ---
    ENROLL_STUDENT,
    DROP_STUDENT,

    // --- Расписание ---
    GET_SCHEDULE,
    GENERATE_SCHEDULE,
    UPDATE_SCHEDULE_SLOT,
    GET_SCHEDULE_STATS,
    DELETE_SCHEDULE_SLOT,

    // --- Посещаемость ---
    MARK_ATTENDANCE,
    GET_ATTENDANCE,
    UPDATE_ATTENDANCE,
    DELETE_ATTENDANCE,

    // --- Успеваемость ---
    SET_GRADE,
    GET_GRADES,
    GET_AVERAGE_GRADE,
    UPDATE_GRADE,
    DELETE_GRADE,

    // --- Финансы ---
    REGISTER_PAYMENT,
    GET_PAYMENTS,
    GET_DEBT,
    GET_DEBTORS,
    GET_GROUP_PROFITABILITY,

    // --- Отчёты ---
    GENERATE_REPORT,

    // --- Сертификаты ---
    GENERATE_CERTIFICATE
}
