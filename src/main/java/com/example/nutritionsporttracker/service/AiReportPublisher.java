
package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.dto.ai.AiReportRequest;
import com.example.nutritionsporttracker.config.RabbitMessagingConfig;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AiReportPublisher {

    private final RabbitTemplate rabbitTemplate;

    public AiReportPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(AiReportRequest request) {
        String reportId = request.reportId().toString();
        CorrelationData correlationData = new CorrelationData(reportId);

        rabbitTemplate.convertAndSend(
                RabbitMessagingConfig.EXCHANGE,
                RabbitMessagingConfig.REQUEST_ROUTING_KEY,
                request,
                message -> {
                    message.getMessageProperties().setDeliveryMode(
                            MessageDeliveryMode.PERSISTENT
                    );
                    message.getMessageProperties().setMessageId(reportId);
                    message.getMessageProperties().setCorrelationId(reportId);
                    return message;
                },
                correlationData
        );

        try {
            CorrelationData.Confirm confirm = correlationData
                    .getFuture()
                    .get(10, TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ mesajı onaylamadı: " + confirm.getReason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "RabbitMQ mesajı herhangi bir kuyruğa yönlendiremedi"
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "RabbitMQ onayı beklenirken işlem kesildi", e
            );
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException(
                    "RabbitMQ yayın onayı alınamadı", e
            );
        }
    }
}