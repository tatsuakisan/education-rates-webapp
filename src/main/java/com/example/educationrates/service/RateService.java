package com.example.educationrates.service;

import com.example.educationrates.model.RateRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class RateService {

    private static final String NCES_URL = "https://nces.ed.gov/programs/digest/d24/tables/dt24_219.10.asp";

    public List<RateRecord> getRates() {
        try {
            List<RateRecord> fromWebsite = loadFromWebsite();
            if (!fromWebsite.isEmpty()) {
                return fromWebsite;
            }
        } catch (Exception ignored) {
        }
        return loadFromExcel();
    }

    public List<String> getWorkbookDebugInfo() {
        List<String> info = new ArrayList<>();
        List<Path> candidates = findWorkbookCandidates();

        if (candidates.isEmpty()) {
            info.add("No workbook found.");
            return info;
        }

        for (Path path : candidates) {
            info.add("Workbook: " + path.toAbsolutePath());
            try (InputStream inputStream = Files.newInputStream(path);
                 Workbook workbook = new XSSFWorkbook(inputStream)) {
                for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                    Sheet sheet = workbook.getSheetAt(sheetIndex);
                    Row headerRow = sheet.getRow(0);
                    if (headerRow == null) {
                        info.add(" - Sheet: " + sheet.getSheetName() + " (no header row found)");
                        continue;
                    }
                    List<String> headers = new ArrayList<>();
                    for (int i = headerRow.getFirstCellNum(); i <= headerRow.getLastCellNum(); i++) {
                        Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        if (cell != null && !cell.toString().isBlank()) {
                            headers.add(cell.toString());
                        }
                    }
                    info.add(" - Sheet: " + sheet.getSheetName() + " | Headers: " + headers);
                }
            } catch (Exception e) {
                info.add(" - ERROR: " + e.getMessage());
            }
        }
        return info;
    }

    private List<RateRecord> loadFromWebsite() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NCES_URL))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "text/html,application/xhtml+xml")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return List.of();
        }
        return parseWebsiteHtml(response.body());
    }

    private List<RateRecord> parseWebsiteHtml(String html) {
        Document doc = Jsoup.parse(html);
        Elements tables = doc.select("table");

        for (Element table : tables) {
            HeaderMatch headerMatch = findTargetHeaderRow(table);
            if (headerMatch == null) {
                continue;
            }

            Map<Integer, Aggregator> grouped = new LinkedHashMap<>();

            Elements rows = table.select("tr");
            for (int rowIndex = headerMatch.rowIndex + 1; rowIndex < rows.size(); rowIndex++) {
                Element row = rows.get(rowIndex);
                List<String> cells = extractCells(row);
                if (cells.size() < 11) {
                    continue;
                }

                Integer year = parseYear(cells.get(0));
                if (year == null) {
                    continue;
                }

                // Col 1  (B): Total Graduates
                // Col 15 (P): Population 17 years old^3
                // Col 16 (Q): Total graduates as ratio of 17-year-old population^4
                Double studentCount       = cells.size() > 1  ? parseNumber(cells.get(1))  : null;
                Double publicSchoolCount  = cells.size() > 4  ? parseNumber(cells.get(4))  : null;
                Double privateSchoolCount = cells.size() > 5  ? parseNumber(cells.get(5))  : null;
                Double attendanceRate     = cells.size() > 7  ? parseNumber(cells.get(7))  : null;
                Double populationValue    = cells.size() > 15 ? parseNumber(cells.get(15)) : null;
                Double graduationRatio    = cells.size() > 16 ? parseNumber(cells.get(16)) : null;
                Double graduationCount    = calculateGraduatedCount(populationValue, graduationRatio);

                Aggregator agg = grouped.computeIfAbsent(year, k -> new Aggregator());
                if (studentCount != null)       agg.studentCountSum  += studentCount;
                if (publicSchoolCount != null)  agg.publicSchoolSum  += publicSchoolCount;
                if (privateSchoolCount != null) agg.privateSchoolSum += privateSchoolCount;
                if (attendanceRate != null)     agg.attendanceSum    += attendanceRate;
                if (graduationCount != null)    agg.graduationSum    += graduationCount;
                if (populationValue != null)    agg.populationSum    += populationValue;
                agg.count++;
            }

            if (!grouped.isEmpty()) {
                return grouped.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> {
                            Aggregator a = entry.getValue();
                            int studentCount  = a.count == 0 ? 0 : (int) Math.round(a.studentCountSum / a.count);
                            int publicCount   = a.count == 0 ? 0 : (int) Math.round(a.publicSchoolSum / a.count);
                            int privateCount  = a.count == 0 ? 0 : (int) Math.round(a.privateSchoolSum / a.count);
                            double attendance = a.count == 0 ? 0 : a.attendanceSum / a.count;
                            double graduation = a.count == 0 ? 0 : a.graduationSum / a.count;
                            double population = a.count == 0 ? 0 : a.populationSum / a.count;
                            return new RateRecord(entry.getKey(), studentCount, attendance, graduation, population, 0, 0, publicCount, privateCount);
                        })
                        .toList();
            }
        }

        return List.of();
    }

    private HeaderMatch findTargetHeaderRow(Element table) {
        Elements rows = table.select("tr");
        for (int i = 0; i < rows.size(); i++) {
            List<String> cells = extractCells(rows.get(i));
            String normalizedRow = cells.stream()
                    .map(this::normalizeHeader)
                    .reduce("", (a, b) -> a + "|" + b);
            if (normalizedRow.contains("schoolyear") && normalizedRow.contains("population17")) {
                return new HeaderMatch(i, cells);
            }
        }
        return null;
    }

    private List<String> extractCells(Element row) {
        List<String> result = new ArrayList<>();
        for (Element cell : row.select("th, td")) {
            result.add(cell.text().trim());
        }
        return result;
    }

    private String getCell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) return null;
        return cells.get(index);
    }

    private Integer findHeaderIndex(Map<String, Integer> headers, String... names) {
        for (String name : names) {
            Integer index = headers.get(normalizeHeader(name));
            if (index != null) return index;
        }
        for (Map.Entry<String, Integer> entry : headers.entrySet()) {
            String key = entry.getKey();
            for (String name : names) {
                String normalized = normalizeHeader(name);
                if (normalized.length() > 4 && key.contains(normalized)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private Integer parseYear(String value) {
        if (value == null || value.isBlank()) return null;
        Matcher matcher = Pattern.compile("(19|20)\\d{2}").matcher(value);
        if (matcher.find()) return Integer.parseInt(matcher.group());
        return null;
    }

    private Double parseNumber(String value) {
        if (value == null || value.isBlank()) return null;
        String cleaned = value
                .replace(",", "")
                .replace("$", "")
                .replace("%", "")
                .replace("−", "-")
                .trim();
        if (cleaned.isEmpty() || cleaned.equals("—") || cleaned.equalsIgnoreCase("NA") || cleaned.equalsIgnoreCase("n/a")) {
            return null;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    private Double calculateGraduatedCount(Double population, Double graduationRatio) {
        if (population == null || graduationRatio == null) return null;
        double ratio = graduationRatio;
        if (ratio > 0 && ratio <= 1.0)    return population * ratio;
        if (ratio > 1.0 && ratio <= 100.0) return population * (ratio / 100.0);
        return population * (ratio / 100.0);
    }

    private List<RateRecord> loadFromExcel() {
        for (Path path : findWorkbookCandidates()) {
            try (InputStream inputStream = Files.newInputStream(path);
                 Workbook workbook = new XSSFWorkbook(inputStream)) {
                List<RateRecord> records = readWorkbook(workbook);
                if (!records.isEmpty()) return records;
            } catch (Exception e) {
                System.err.println("Failed to read Excel file: " + path + " -> " + e.getMessage());
            }
        }
        return List.of();
    }

    private List<Path> findWorkbookCandidates() {
        List<Path> unique = new ArrayList<>();
        List<Path> roots = List.of(
                Paths.get(System.getProperty("user.dir")).toAbsolutePath(),
                Paths.get(System.getProperty("user.home")).toAbsolutePath(),
                Paths.get(System.getProperty("user.dir")).resolve("src/main/resources").toAbsolutePath()
        );
        for (Path root : roots) {
            if (Files.isRegularFile(root) && isExcelFile(root)) {
                unique.add(root);
            } else if (Files.isDirectory(root)) {
                try (Stream<Path> paths = Files.walk(root)) {
                    paths.filter(Files::isRegularFile).filter(this::isExcelFile).forEach(unique::add);
                } catch (IOException ignored) {
                }
            }
        }
        return unique;
    }

    private boolean isExcelFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".xlsx") || name.endsWith(".xlsm");
    }

    private List<RateRecord> readWorkbook(Workbook workbook) {
        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            int headerRowIndex = findHeaderRowIndex(sheet);
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) continue;

            Map<Integer, Aggregator> grouped = new LinkedHashMap<>();

            for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                Integer year = readInt(row, 0);
                if (year == null) continue;

                // Col 1  (B): Total Graduates
                // Col 15 (P): Population 17 years old^3
                // Col 16 (Q): Total graduates as ratio of 17-year-old population^4
                Double studentCount       = readDouble(row, 1);
                Double publicSchoolCount  = readDouble(row, 4);
                Double privateSchoolCount = readDouble(row, 5);
                Double attendanceRate     = readDouble(row, 7);
                Double populationValue    = readDouble(row, 15);
                Double graduationRatio    = readDouble(row, 16);
                Double graduationCount    = calculateGraduatedCount(populationValue, graduationRatio);

                Aggregator agg = grouped.computeIfAbsent(year, k -> new Aggregator());
                if (studentCount != null)       agg.studentCountSum  += studentCount;
                if (publicSchoolCount != null)  agg.publicSchoolSum  += publicSchoolCount;
                if (privateSchoolCount != null) agg.privateSchoolSum += privateSchoolCount;
                if (attendanceRate != null)     agg.attendanceSum    += attendanceRate;
                if (graduationCount != null)    agg.graduationSum    += graduationCount;
                if (populationValue != null)    agg.populationSum    += populationValue;
                agg.count++;
            }

            if (!grouped.isEmpty()) {
                return grouped.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> {
                            Aggregator a = entry.getValue();
                            int studentCount  = a.count == 0 ? 0 : (int) Math.round(a.studentCountSum / a.count);
                            int publicCount   = a.count == 0 ? 0 : (int) Math.round(a.publicSchoolSum / a.count);
                            int privateCount  = a.count == 0 ? 0 : (int) Math.round(a.privateSchoolSum / a.count);
                            double attendance = a.count == 0 ? 0 : a.attendanceSum / a.count;
                            double graduation = a.count == 0 ? 0 : a.graduationSum / a.count;
                            double population = a.count == 0 ? 0 : a.populationSum / a.count;
                            return new RateRecord(entry.getKey(), studentCount, attendance, graduation, population, 0, 0, publicCount, privateCount);
                        })
                        .toList();
            }
        }
        return List.of();
    }

    private int findHeaderRowIndex(Sheet sheet) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String text = "";
            for (int i = row.getFirstCellNum(); i <= row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell != null) text += normalizeHeader(cell.toString());
            }
            if (text.contains("schoolyear") && text.contains("population17")) {
                return rowIndex;
            }
        }
        return 0;
    }

    private Integer readInt(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            return (int) cell.getNumericCellValue();
        } catch (Exception ignored) {
            try {
                return Integer.parseInt(cell.toString().trim());
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private Double readDouble(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            return cell.getNumericCellValue();
        } catch (Exception ignored) {
            try {
                return Double.parseDouble(cell.toString().trim().replace(",", ""));
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private String normalizeHeader(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\\\[0-9]+\\\\", " ")
                .replaceAll("[^a-z0-9]+", "");
    }

    private static class Aggregator {
        double studentCountSum = 0;
        double publicSchoolSum = 0;
        double privateSchoolSum = 0;
        double attendanceSum = 0;
        double graduationSum = 0;
        double populationSum = 0;
        int count = 0;
    }

    private static class HeaderMatch {
        private final int rowIndex;
        private final List<String> cells;
        private HeaderMatch(int rowIndex, List<String> cells) {
            this.rowIndex = rowIndex;
            this.cells = cells;
        }
    }
}