package com.example.educationrates.controller;

import com.example.educationrates.model.RateRecord;
import com.example.educationrates.service.RateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class DashboardController {

    private final RateService rateService;

    public DashboardController(RateService rateService) {
        this.rateService = rateService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "Education Rates Dashboard");
        return "index";
    }

    @GetMapping("/api/rates")
    @ResponseBody
    public List<RateRecord> rates() {
        try {
            return rateService.getRates();
        } catch (Exception e) {
            return List.of();
        }
    }

    @GetMapping("/api/debug/workbooks")
    @ResponseBody
    public List<String> debugWorkbooks() {
        try {
            return rateService.getWorkbookDebugInfo();
        } catch (Exception e) {
            return List.of("Debug endpoint failed: " + e.getMessage());
        }
    }
}