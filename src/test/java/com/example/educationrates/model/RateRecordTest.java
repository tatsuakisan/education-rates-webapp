package com.example.educationrates.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateRecordTest {

    @Test
    void constructor_setsAllFields() {
        RateRecord record = new RateRecord(2023, 500, 92.5, 87.3);

        assertEquals(2023, record.getYear());
        assertEquals(500, record.getStudentCount());
        assertEquals(92.5, record.getAttendanceRate());
        assertEquals(87.3, record.getGraduationRate());
    }

    @Test
    void getYear_returnsYear() {
        RateRecord record = new RateRecord(2020, 0, 0.0, 0.0);
        assertEquals(2020, record.getYear());
    }

    @Test
    void getStudentCount_returnsStudentCount() {
        RateRecord record = new RateRecord(0, 1200, 0.0, 0.0);
        assertEquals(1200, record.getStudentCount());
    }

    @Test
    void getAttendanceRate_returnsAttendanceRate() {
        RateRecord record = new RateRecord(0, 0, 95.0, 0.0);
        assertEquals(95.0, record.getAttendanceRate());
    }

    @Test
    void getGraduationRate_returnsGraduationRate() {
        RateRecord record = new RateRecord(0, 0, 0.0, 88.0);
        assertEquals(88.0, record.getGraduationRate());
    }

    @Test
    void constructor_allowsZeroValues() {
        RateRecord record = new RateRecord(0, 0, 0.0, 0.0);
        assertEquals(0, record.getYear());
        assertEquals(0, record.getStudentCount());
        assertEquals(0.0, record.getAttendanceRate());
        assertEquals(0.0, record.getGraduationRate());
    }
}
