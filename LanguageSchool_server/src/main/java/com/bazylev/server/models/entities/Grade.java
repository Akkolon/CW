package com.bazylev.server.models.entities;

import com.bazylev.server.enums.GradeType;

import java.time.LocalDate;

public class Grade {

    private int id;
    private int groupStudentId;
    private LocalDate gradeDate;
    private double value;
    private GradeType gradeType;
    private String comment;

    public Grade() {}

    public Grade(int id, int groupStudentId, LocalDate gradeDate,
                 double value, GradeType gradeType, String comment) {
        this.id = id;
        this.groupStudentId = groupStudentId;
        this.gradeDate = gradeDate;
        this.value = value;
        this.gradeType = gradeType;
        this.comment = comment;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGroupStudentId() { return groupStudentId; }
    public void setGroupStudentId(int groupStudentId) { this.groupStudentId = groupStudentId; }

    public LocalDate getGradeDate() { return gradeDate; }
    public void setGradeDate(LocalDate gradeDate) { this.gradeDate = gradeDate; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public GradeType getGradeType() { return gradeType; }
    public void setGradeType(GradeType gradeType) { this.gradeType = gradeType; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
