package com.bazylev.server.models.entities;

import java.math.BigDecimal;

public class Course {

    private int id;
    private String name;
    private String description;
    private int durationHours;
    private String level;
    private BigDecimal pricePerMonth;
    private boolean active;

    public Course() {}

    public Course(int id, String name, String description, int durationHours,
                  String level, BigDecimal pricePerMonth, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.durationHours = durationHours;
        this.level = level;
        this.pricePerMonth = pricePerMonth;
        this.active = active;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public BigDecimal getPricePerMonth() { return pricePerMonth; }
    public void setPricePerMonth(BigDecimal pricePerMonth) { this.pricePerMonth = pricePerMonth; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
