package com.example.nutritionsporttracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenRouterClient {

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model:openai/gpt-4o-mini}")
    private String model;

    @Value("${openrouter.temperature:0.6}")
    private double temperature;

    // Opsiyonel ama OpenRouter bazen sever:
    @Value("${openrouter.http_referer:http://localhost:8080}")
    private String httpReferer;

    @Value("${openrouter.x_title:NutritionSportTracker}")
    private String xTitle;

    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public String complete(String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("OpenRouter API key yok (openrouter.api.key)");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new RuntimeException("OpenRouter prompt boş olamaz");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // ✅ Auth
        headers.setBearerAuth(apiKey.trim());

        // ✅ OpenRouter meta headers (bazı durumlarda gerekli/yararlı)
        headers.set("HTTP-Referer", httpReferer);
        headers.set("X-Title", xTitle);

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", temperature,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            String json = mapper.writeValueAsString(body);

            ResponseEntity<String> resp = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(json, headers),
                    String.class
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new RuntimeException("OpenRouter başarısız: " + resp.getStatusCode());
            }

            JsonNode root = mapper.readTree(resp.getBody());

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("OpenRouter choices boş döndü: " + root.toString());
            }

            String content = choices.get(0).path("message").path("content").asText("");

            if (content == null || content.isBlank()) {
                throw new RuntimeException("OpenRouter boş content döndü: " + root.toString());
            }

            return content;

        } catch (HttpStatusCodeException ex) {
            // ✅ Burada artık "no body" yerine gerçek body'i görürsün (varsa)
            String errBody = ex.getResponseBodyAsString();
            throw new RuntimeException("OpenRouter HTTP " + ex.getStatusCode() + " | " + errBody, ex);

        } catch (Exception e) {
            throw new RuntimeException("OpenRouter hata: " + e.getMessage(), e);
        }
    }
}