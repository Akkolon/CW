package com.bazylev.client.models.entities;

import java.time.LocalDate;

public class Student {

    private int id;
    private int personId;
    private LocalDate enrollmentDate;
    private boolean active;

    public Student() {}

    public Student(int id, int personId, LocalDate enrollmentDate, boolean active) {
        this.id = id;
        this.personId = personId;
        this.enrollmentDate = enrollmentDate;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPersonId() { return personId; }
    public void setPersonId(int personId) { this.personId = personId; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
