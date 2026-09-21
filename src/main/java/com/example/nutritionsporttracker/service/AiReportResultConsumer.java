package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.config.RabbitMessagingConfig;
import com.example.nutritionsporttracker.dto.ai.AiReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AiReportResultConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(AiReportResultConsumer.class);

    private final AiReportResultService resultService;
    private final AiReportEmailDeliveryService emailDeliveryService;

    public AiReportResultConsumer(
            AiReportResultService resultService,
            AiReportEmailDeliveryService emailDeliveryService
    ) {
        this.resultService = resultService;
        this.emailDeliveryService = emailDeliveryService;
    }

    @RabbitListener(
            queues = RabbitMessagingConfig.RESULT_QUEUE,
            autoStartup = "${app.ai-report.result-consumer.enabled:false}"
    )
    public void handle(AiReportResponse result) {
        boolean created = resultService.saveIfAbsent(result);

        log.info(
                "AI report result received; reportId={}, newlySaved={}",
                result.reportId(),
                created
        );

        emailDeliveryService.sendIfPending(result.reportId());
    }
}