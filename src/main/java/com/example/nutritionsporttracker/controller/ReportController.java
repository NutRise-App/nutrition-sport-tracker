
package com.example.nutritionsporttracker.controller;

import com.example.nutritionsporttracker.dto.ai.AiReportRequest;
import com.example.nutritionsporttracker.model.Reports;
import com.example.nutritionsporttracker.service.AiReportPublisher;
import com.example.nutritionsporttracker.service.ReportService;
import com.example.nutritionsporttracker.service.WeeklyReportRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;
    private final WeeklyReportRequestService weeklyReportRequestService;
    private final AiReportPublisher aiReportPublisher;

    public ReportController(
            ReportService reportService,
            WeeklyReportRequestService weeklyReportRequestService,
            AiReportPublisher aiReportPublisher
    ) {
        this.reportService = reportService;
        this.weeklyReportRequestService = weeklyReportRequestService;
        this.aiReportPublisher = aiReportPublisher;
    }

    // Eski senkron endpoint; geçiş tamamlanana kadar korunuyor.
    @GetMapping("/weekly")
    public ResponseEntity<Reports> weekly(Principal principal) {
        Reports report = reportService.generateWeeklyReport(
                principal.getName()
        );
        return ResponseEntity.ok(report);
    }

    // Yeni asenkron endpoint: Spring Boot -> RabbitMQ -> Python
    @PostMapping("/weekly/request")
    public ResponseEntity<Map<String, Object>> requestWeeklyReport(
            Principal principal
    ) {
        AiReportRequest request =
                weeklyReportRequestService.buildRequest(principal.getName());

        aiReportPublisher.publish(request);

        Map<String, Object> response = Map.of(
                "reportId", request.reportId(),
                "status", "QUEUED"
        );

        return ResponseEntity.accepted().body(response);
    }
}