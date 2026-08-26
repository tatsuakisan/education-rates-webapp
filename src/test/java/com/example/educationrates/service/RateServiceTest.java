package com.example.educationrates.service;

import com.example.educationrates.model.RateRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RateServiceTest {

    private final RateService service = new RateService();

    @Test
    void getRates_returnsNonNull() {
        // Result is either empty or populated depending on file presence; must never be null
        assertNotNull(service.getRates());
    }

    @Test
    void getRates_returnsData_whenExcelFileExistsAtCurrentDirectory() throws IOException {
        // Place a known workbook at the first candidate path so results are predictable
        Path excelPath = Path.of("highschool usa.xlsx");
        writeTestWorkbook(excelPath, 2022, 300, 90.0, 85.0);
        try {
            List<RateRecord> rates = service.getRates();
            assertFalse(rates.isEmpty());
            RateRecord record = rates.get(0);
            assertEquals(2022, record.getYear());
            assertEquals(300, record.getStudentCount());
            assertEquals(90.0, record.getAttendanceRate(), 0.01);
            assertEquals(85.0, record.getGraduationRate(), 0.01);
        } finally {
            excelPath.toFile().delete();
        }
    }

    @Test
    void getRates_aggregatesMultipleRowsForSameYear() throws IOException {
        Path excelPath = Path.of("highschool usa.xlsx");
        writeTestWorkbookMultipleRows(excelPath);
        try {
            List<RateRecord> rates = service.getRates();
            assertFalse(rates.isEmpty());
            // Both rows are for year 2021 — service should aggregate them into one record
            assertEquals(1, rates.size());
            assertEquals(2021, rates.get(0).getYear());
            // studentCount is average of 200 and 400 = 300
            assertEquals(300, rates.get(0).getStudentCount());
        } finally {
            excelPath.toFile().delete();
        }
    }

    @Test
    void getRates_sortsByYear() throws IOException {
        Path excelPath = Path.of("highschool usa.xlsx");
        writeTestWorkbookUnsortedYears(excelPath);
        try {
            List<RateRecord> rates = service.getRates();
            assertEquals(2, rates.size());
            // Results must be sorted ascending by year
            assertTrue(rates.get(0).getYear() < rates.get(1).getYear());
        } finally {
            excelPath.toFile().delete();
        }
    }

    // --- helpers ---

    private void writeTestWorkbook(Path path, int year, int students, double attendance, double graduation)
            throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Year");
            header.createCell(1).setCellValue("StudentCount");
            header.createCell(2).setCellValue("AttendanceRate");
            header.createCell(3).setCellValue("GraduationRate");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(year);
            data.createCell(1).setCellValue(students);
            data.createCell(2).setCellValue(attendance);
            data.createCell(3).setCellValue(graduation);
            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                wb.write(fos);
            }
        }
    }

    private void writeTestWorkbookMultipleRows(Path path) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Year");
            header.createCell(1).setCellValue("StudentCount");
            header.createCell(2).setCellValue("AttendanceRate");
            header.createCell(3).setCellValue("GraduationRate");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(2021);
            row1.createCell(1).setCellValue(200);
            row1.createCell(2).setCellValue(88.0);
            row1.createCell(3).setCellValue(82.0);
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(2021);
            row2.createCell(1).setCellValue(400);
            row2.createCell(2).setCellValue(92.0);
            row2.createCell(3).setCellValue(86.0);
            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                wb.write(fos);
            }
        }
    }

    private void writeTestWorkbookUnsortedYears(Path path) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Year");
            header.createCell(1).setCellValue("StudentCount");
            header.createCell(2).setCellValue("AttendanceRate");
            header.createCell(3).setCellValue("GraduationRate");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(2023);
            row1.createCell(1).setCellValue(100);
            row1.createCell(2).setCellValue(90.0);
            row1.createCell(3).setCellValue(80.0);
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(2019);
            row2.createCell(1).setCellValue(150);
            row2.createCell(2).setCellValue(85.0);
            row2.createCell(3).setCellValue(75.0);
            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                wb.write(fos);
            }
        }
    }
}

