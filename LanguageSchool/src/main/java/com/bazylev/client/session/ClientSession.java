package com.bazylev.client.session;

import com.bazylev.client.enums.Role;

public final class ClientSession {

    private static final ClientSession INSTANCE = new ClientSession();

    private String token;
    private Role   role;
    private String login;
    private int    userId;
    private int    studentId; // заполняется только для студентов

    private ClientSession() {}

    public static ClientSession getInstance() {
        return INSTANCE;
    }

    public void init(String token, Role role, String login, int userId) {
        this.token     = token;
        this.role      = role;
        this.login     = login;
        this.userId    = userId;
        this.studentId = 0;
    }

    public void init(String token, Role role, String login, int userId, int studentId) {
        this.token     = token;
        this.role      = role;
        this.login     = login;
        this.userId    = userId;
        this.studentId = studentId;
    }

    public void clear() {
        token     = null;
        role      = null;
        login     = null;
        userId    = 0;
        studentId = 0;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isBlank();
    }

    public boolean isAdmin()   { return role == Role.ADMIN; }
    public boolean isTeacher() { return role == Role.TEACHER; }
    public boolean isStudent() { return role == Role.STUDENT; }

    public String getToken()     { return token; }
    public Role   getRole()      { return role; }
    public String getLogin()     { return login; }
    public int    getUserId()    { return userId; }
    public int    getStudentId() { return studentId; }
}
