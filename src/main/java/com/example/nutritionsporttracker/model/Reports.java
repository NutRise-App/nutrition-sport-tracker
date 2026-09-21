package com.example.nutritionsporttracker.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Reports {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Lob
    private String reportText;

    @Column(name = "report_id", length = 36, unique = true)
    private String reportId;

    private Double caloriesIn;
    private Double caloriesOut;
    private Double avgProtein;
    private Double avgCarbs;
    private Double avgFat;
    private Double avgWater;

    private LocalDateTime generatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getReportText() {
        return reportText;
    }

    public void setReportText(String reportText) {
        this.reportText = reportText;
    }

    public Double getCaloriesIn() {
        return caloriesIn;
    }

    public void setCaloriesIn(Double caloriesIn) {
        this.caloriesIn = caloriesIn;
    }

    public Double getCaloriesOut() {
        return caloriesOut;
    }

    public void setCaloriesOut(Double caloriesOut) {
        this.caloriesOut = caloriesOut;
    }

    public Double getAvgProtein() {
        return avgProtein;
    }

    public void setAvgProtein(Double avgProtein) {
        this.avgProtein = avgProtein;
    }

    public Double getAvgCarbs() {
        return avgCarbs;
    }

    public void setAvgCarbs(Double avgCarbs) {
        this.avgCarbs = avgCarbs;
    }

    public Double getAvgFat() {
        return avgFat;
    }

    public void setAvgFat(Double avgFat) {
        this.avgFat = avgFat;
    }

    public Double getAvgWater() {
        return avgWater;
    }

    public void setAvgWater(Double avgWater) {
        this.avgWater = avgWater;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
}
