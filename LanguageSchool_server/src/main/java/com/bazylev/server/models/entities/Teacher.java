package com.bazylev.server.models.entities;

import java.time.LocalDate;

public class Teacher {

    private int id;
    private int personId;
    private String specialization;
    private LocalDate hireDate;

    public Teacher() {}

    public Teacher(int id, int personId, String specialization, LocalDate hireDate) {
        this.id = id;
        this.personId = personId;
        this.specialization = specialization;
        this.hireDate = hireDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPersonId() { return personId; }
    public void setPersonId(int personId) { this.personId = personId; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
}
