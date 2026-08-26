package com.example.educationrates.controller;

import com.example.educationrates.model.RateRecord;
import com.example.educationrates.service.RateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RateService rateService;

    @Test
    void index_returnsIndexViewWithTitleAttribute() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("title", "Education Rates Dashboard"));
    }

    @Test
    void rates_returnsEmptyJsonArray_whenServiceReturnsEmpty() throws Exception {
        when(rateService.getRates()).thenReturn(List.of());

        mockMvc.perform(get("/api/rates"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void rates_returnsRateRecordsAsJson() throws Exception {
        RateRecord record = new RateRecord(2023, 500, 92.5, 87.3);
        when(rateService.getRates()).thenReturn(List.of(record));

        mockMvc.perform(get("/api/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2023))
                .andExpect(jsonPath("$[0].studentCount").value(500))
                .andExpect(jsonPath("$[0].attendanceRate").value(92.5))
                .andExpect(jsonPath("$[0].graduationRate").value(87.3));
    }

    @Test
    void rates_returnsMultipleRecords() throws Exception {
        List<RateRecord> records = List.of(
                new RateRecord(2021, 300, 88.0, 82.0),
                new RateRecord(2022, 400, 90.0, 85.0)
        );
        when(rateService.getRates()).thenReturn(records);

        mockMvc.perform(get("/api/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].year").value(2021))
                .andExpect(jsonPath("$[1].year").value(2022));
    }
}
