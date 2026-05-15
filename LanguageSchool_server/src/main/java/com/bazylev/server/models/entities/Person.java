package com.bazylev.server.models.entities;

public class Person {

    private int id;
    private Integer userId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;

    public Person() {}

    public Person(int id, Integer userId, String firstName, String lastName,
                  String middleName, String email) {
        this.id = id;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.email = email;
    }

    public String getFullName() {
        return lastName + " " + firstName + " " + middleName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
