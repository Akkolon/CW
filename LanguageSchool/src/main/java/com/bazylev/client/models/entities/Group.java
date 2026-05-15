package com.bazylev.client.models.entities;

import com.bazylev.client.enums.GroupStatus;

import java.time.LocalDate;

public class Group {

    private int id;
    private String name;
    private int courseId;
    private int teacherId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxStudents;
    private GroupStatus status;
    private int studentCount;

    public Group() {}

    public Group(int id, String name, int courseId, int teacherId,
                 LocalDate startDate, LocalDate endDate, int maxStudents, GroupStatus status) {
        this.id = id;
        this.name = name;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxStudents = maxStudents;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public int getTeacherId() { return teacherId; }
    public void setTeacherId(int teacherId) { this.teacherId = teacherId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getMaxStudents() { return maxStudents; }
    public void setMaxStudents(int maxStudents) { this.maxStudents = maxStudents; }

    public GroupStatus getStatus() { return status; }
    public void setStatus(GroupStatus status) { this.status = status; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
}
