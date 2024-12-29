package com.example.cantinaonline;

public class Student {
    private String id;
    private String name;
    private int restante; // Number of overdue days
    private Long daysPaid;

    public Student() {
        // Default constructor for Firestore
    }

    public Student(String id, String name, int restante, Long daysPaid) {
        this.id = id;
        this.name = name;
        this.restante = restante;
        this.daysPaid = daysPaid;
    }
    public Student(String id, String name,  Long daysPaid) {
        this.id = id;
        this.name = name;
        this.daysPaid = daysPaid;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRestante() {
        return restante;
    }

    public void setRestante(int restante) {
        this.restante = restante;
    }

    public Long getDaysPaid() {
        return daysPaid;
    }

    public void setDaysPaid(Long daysPaid) {
        this.daysPaid = daysPaid;
    }
}
