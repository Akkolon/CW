package com.bazylev.server.models.entities;

import com.bazylev.server.enums.EnrollmentStatus;

import java.time.LocalDate;

public class GroupStudent {

    private int id;
    private int groupId;
    private int studentId;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;

    public GroupStudent() {}

    public GroupStudent(int id, int groupId, int studentId,
                        LocalDate enrollmentDate, EnrollmentStatus status) {
        this.id = id;
        this.groupId = groupId;
        this.studentId = studentId;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
}
