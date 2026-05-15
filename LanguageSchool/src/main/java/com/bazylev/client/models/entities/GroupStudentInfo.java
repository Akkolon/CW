package com.bazylev.client.models.entities;

public class GroupStudentInfo {

    private int    id;
    private int    groupId;
    private int    studentId;
    private String fullName;

    public GroupStudentInfo() {}

    public int    getId()        { return id; }
    public int    getGroupId()   { return groupId; }
    public int    getStudentId() { return studentId; }
    public String getFullName()  { return fullName; }

    public void setId(int id)               { this.id = id; }
    public void setGroupId(int groupId)     { this.groupId = groupId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public void setFullName(String name)    { this.fullName = name; }

    @Override
    public String toString() { return fullName; }
}