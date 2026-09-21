package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.dto.ai.AiReportResponse;
import com.example.nutritionsporttracker.model.EmailLog;
import com.example.nutritionsporttracker.model.Reports;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.repository.EmailLogRepository;
import com.example.nutritionsporttracker.repository.ReportRepository;
import com.example.nutritionsporttracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
public class AiReportResultService {

    private static final ZoneId REPORT_ZONE =
            ZoneId.of("Europe/Istanbul");

    private final ReportRepository reportRepository;
    private final EmailLogRepository emailLogRepository;
    private final UserRepository userRepository;

    public AiReportResultService(
            ReportRepository reportRepository,
            EmailLogRepository emailLogRepository,
            UserRepository userRepository
    ) {
        this.reportRepository = reportRepository;
        this.emailLogRepository = emailLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean saveIfAbsent(AiReportResponse result) {
        Objects.requireNonNull(result, "AI report result is required");
        Objects.requireNonNull(result.reportId(), "reportId is required");
        Objects.requireNonNull(result.userId(), "userId is required");
        Objects.requireNonNull(
                result.recommendations(),
                "recommendations are required"
        );

        String reportId = result.reportId().toString();

        // Aynı sonuç yeniden gelirse ikinci rapor oluşturma.
        if (reportRepository.findByReportId(reportId).isPresent()) {
            return false;
        }

        User user = userRepository.findById(result.userId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Report user not found: " + result.userId()
                ));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalStateException(
                    "Report user has no email address"
            );
        }

        String reportText = """
                Haftalık Özet
                %s

                Beslenme Analizi
                %s

                Egzersiz Analizi
                %s

                Su Tüketimi Analizi
                %s

                Öneriler
                - %s
                """.formatted(
                result.summary(),
                result.nutritionAnalysis(),
                result.workoutAnalysis(),
                result.waterAnalysis(),
                String.join("\n- ", result.recommendations())
        );

        Reports report = new Reports();
        report.setReportId(reportId);
        report.setUser(user);
        report.setReportText(reportText);
        report.setGeneratedAt(LocalDateTime.now(REPORT_ZONE));
        reportRepository.save(report);

        // E-posta henüz gönderilmiyor: yalnızca gönderim işi kaydediliyor.
        EmailLog emailLog = new EmailLog();
        emailLog.setReportId(reportId);
        emailLog.setUserId(user.getId());
        emailLog.setToEmail(user.getEmail());
        emailLog.setSubject("NutRise | Haftalık AI Raporun");
        emailLog.setReportDate(LocalDate.now(REPORT_ZONE));
        emailLog.setStatus(EmailLog.Status.PENDING);
        emailLogRepository.save(emailLog);

        return true;
    }
}