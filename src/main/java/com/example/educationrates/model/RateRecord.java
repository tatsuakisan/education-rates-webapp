package com.example.educationrates.model;

public class RateRecord {
    private final int year;
    private final int studentCount;
    private final double attendanceRate;
    private final double graduationRate;
    private final double population;
    private final int maleCount;
    private final int femaleCount;
    private final int publicSchoolCount;
    private final int privateSchoolCount;

    public RateRecord(int year, int studentCount, double attendanceRate, double graduationRate) {
        this(year, studentCount, attendanceRate, graduationRate, 0.0, 0, 0, 0, 0);
    }

    public RateRecord(
            int year,
            int studentCount,
            double attendanceRate,
            double graduationRate,
            double population,
            int maleCount,
            int femaleCount,
            int publicSchoolCount,
            int privateSchoolCount) {

        this.year = year;
        this.studentCount = studentCount;
        this.attendanceRate = attendanceRate;
        this.graduationRate = graduationRate;
        this.population = population;
        this.maleCount = maleCount;
        this.femaleCount = femaleCount;
        this.publicSchoolCount = publicSchoolCount;
        this.privateSchoolCount = privateSchoolCount;
    }

    public int getYear() {
        return year;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public double getGraduationRate() {
        return graduationRate;
    }

    public double getPopulation() {
        return population;
    }

    public int getMaleCount() {
        return maleCount;
    }

    public int getFemaleCount() {
        return femaleCount;
    }

    public int getPublicSchoolCount() {
        return publicSchoolCount;
    }

    public int getPrivateSchoolCount() {
        return privateSchoolCount;
    }
}