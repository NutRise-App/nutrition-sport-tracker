package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.model.EmailLog;
import com.example.nutritionsporttracker.model.Reports;
import com.example.nutritionsporttracker.repository.EmailLogRepository;
import com.example.nutritionsporttracker.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class AiReportEmailDeliveryService {

    private static final Logger log =
            LoggerFactory.getLogger(AiReportEmailDeliveryService.class);

    private static final ZoneId REPORT_ZONE =
            ZoneId.of("Europe/Istanbul");

    private final EmailLogRepository emailLogRepository;
    private final ReportRepository reportRepository;
    private final EmailService emailService;

    public AiReportEmailDeliveryService(
            EmailLogRepository emailLogRepository,
            ReportRepository reportRepository,
            EmailService emailService
    ) {
        this.emailLogRepository = emailLogRepository;
        this.reportRepository = reportRepository;
        this.emailService = emailService;
    }

    public void sendIfPending(UUID id) {
        String reportId = id.toString();

        EmailLog emailLog = emailLogRepository.findByReportId(reportId)
                .orElseThrow(() -> new IllegalStateException(
                        "Email log not found for reportId=" + reportId
                ));

        if (emailLog.getStatus() != EmailLog.Status.PENDING) {
            log.info(
                    "Email not sent again; reportId={}, status={}",
                    reportId,
                    emailLog.getStatus()
            );
            return;
        }

        Reports report = reportRepository.findByReportId(reportId)
                .orElseThrow(() -> new IllegalStateException(
                        "Report not found for reportId=" + reportId
                ));

        if (emailLog.getToEmail() == null
                || emailLog.getToEmail().isBlank()
                || report.getReportText() == null) {
            throw new IllegalStateException(
                    "Email recipient or report text is missing for reportId="
                            + reportId
            );
        }

        // Bu geçiş ayrı bir veritabanı işleminde kalıcı olur.
        int claimed = emailLogRepository.changeStatus(
                reportId,
                EmailLog.Status.PENDING,
                EmailLog.Status.SENDING,
                null,
                null
        );

        if (claimed != 1) {
            log.info(
                    "Email already claimed by another consumer; reportId={}",
                    reportId
            );
            return;
        }

        // AI metnini HTML olarak yorumlatmadan, güvenli düz metin gibi göster.
        String html = """
                <div style="font-family: Arial, sans-serif; white-space: pre-wrap;">
                %s
                </div>
                """.formatted(HtmlUtils.htmlEscape(report.getReportText()));

        try {
            emailService.sendHtmlEmail(
                    emailLog.getToEmail(),
                    emailLog.getSubject(),
                    html
            );
        } catch (RuntimeException ex) {
            // SMTP hatasında teslimatın gerçekleşip gerçekleşmediği
            // her zaman kesin olarak bilinemeyebilir. Otomatik tekrar yok.
            emailLogRepository.changeStatus(
                    reportId,
                    EmailLog.Status.SENDING,
                    EmailLog.Status.FAILED,
                    null,
                    ex.getMessage()
            );

            log.error(
                    "AI report email needs review; reportId={}",
                    reportId,
                    ex
            );
            return;
        }

        int markedSent = emailLogRepository.changeStatus(
                reportId,
                EmailLog.Status.SENDING,
                EmailLog.Status.SENT,
                LocalDateTime.now(REPORT_ZONE),
                null
        );

        if (markedSent != 1) {
            throw new IllegalStateException(
                    "Email was sent but SENT status could not be recorded; "
                            + "manual review required for reportId=" + reportId
            );
        }

        log.info("AI report email sent; reportId={}", reportId);
    }
}