package com.example.cantinaonline;

public class Student {
    private String id;
    private String name;
    private long daysPaid;

    public Student(String id, String name, long daysPaid) {
        this.id = id;
        this.name = name;
        this.daysPaid = daysPaid;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getDaysPaid() {
        return daysPaid;
    }
}

