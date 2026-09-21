package com.example.nutritionsporttracker.dto.ai;

import java.util.List;
import java.util.UUID;

public record AiReportResponse(
        UUID reportId,
        Long userId,
        String summary,
        String nutritionAnalysis,
        String workoutAnalysis,
        String waterAnalysis,
        List<String> recommendations
) {
}