package com.example.nutritionsporttracker.config;

import com.example.nutritionsporttracker.dto.ai.AiReportResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AiReportMessageConversionTest {

    @Test
    void convertsPythonResultWithoutJavaTypeHeader() {
        String json = """
                {
                  "reportId": "550e8400-e29b-41d4-a716-446655440000",
                  "userId": 1,
                  "summary": "Haftalık özet",
                  "nutritionAnalysis": "Beslenme analizi",
                  "workoutAnalysis": "Egzersiz analizi",
                  "waterAnalysis": "Su tüketimi analizi",
                  "recommendations": [
                    "Düzenli su iç",
                    "Öğünlerini takip et"
                  ]
                }
                """;

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);

        // Spring'in @RabbitListener parametresinden çıkardığı tipi taklit eder.
        // Python mesajındaki gibi __TypeId__ başlığı eklemiyoruz.
        properties.setInferredArgumentType(AiReportResponse.class);

        Message message = new Message(
                json.getBytes(StandardCharsets.UTF_8),
                properties
        );

        Jackson2JsonMessageConverter converter =
                new Jackson2JsonMessageConverter(new ObjectMapper());

        AiReportResponse result = assertInstanceOf(
                AiReportResponse.class,
                converter.fromMessage(message)
        );

        assertEquals(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                result.reportId()
        );
        assertEquals(1L, result.userId());
        assertEquals(2, result.recommendations().size());
    }
}