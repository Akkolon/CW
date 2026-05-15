package com.bazylev.client.models.entities;

import com.bazylev.client.enums.AttendanceStatus;

import java.time.LocalDate;

public class Attendance {

    private int id;
    private int groupStudentId;
    private LocalDate lessonDate;
    private AttendanceStatus status;
    private String comment;

    public Attendance() {}

    public Attendance(int id, int groupStudentId, LocalDate lessonDate,
                      AttendanceStatus status, String comment) {
        this.id = id;
        this.groupStudentId = groupStudentId;
        this.lessonDate = lessonDate;
        this.status = status;
        this.comment = comment;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGroupStudentId() { return groupStudentId; }
    public void setGroupStudentId(int groupStudentId) { this.groupStudentId = groupStudentId; }

    public LocalDate getLessonDate() { return lessonDate; }
    public void setLessonDate(LocalDate lessonDate) { this.lessonDate = lessonDate; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
