package com.bazylev.client.models.entities;

import com.bazylev.client.enums.Role;

public class User {

    private int id;
    private String login;
    private String passwordHash;
    private Role role;
    private boolean blocked;
    private int personId;

    public User() {}

    public User(int id, String login, String passwordHash, Role role, boolean blocked, int personId) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
        this.blocked = blocked;
        this.personId = personId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public int getPersonId() { return personId; }
    public void setPersonId(int personId) { this.personId = personId; }
}
