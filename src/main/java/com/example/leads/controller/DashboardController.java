package com.example.leads.controller;

import com.example.leads.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final int COLD_AFTER_DAYS = 7;
    private static final int COLD_LIST_LIMIT = 10;

    private final ReportService reportService;

    public DashboardController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("kpi", reportService.kpis());
        model.addAttribute("funnel", reportService.funnel());
        model.addAttribute("sources", reportService.conversionBySource());
        model.addAttribute("workload", reportService.workload());
        model.addAttribute("ageing", reportService.ageing());
        model.addAttribute("cold", reportService.staleLeads(COLD_AFTER_DAYS, COLD_LIST_LIMIT));
        model.addAttribute("coldDays", COLD_AFTER_DAYS);
        return "dashboard";
    }
}