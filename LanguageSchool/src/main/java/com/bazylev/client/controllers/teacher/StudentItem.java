package com.bazylev.client.controllers.teacher;

public record StudentItem(int gsId, int studentId, String name) {
    @Override
    public String toString() { return name; }
}