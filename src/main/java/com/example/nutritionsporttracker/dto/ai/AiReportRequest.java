
package com.example.nutritionsporttracker.dto.ai;

import java.time.LocalDate;
import java.util.UUID;

public record AiReportRequest(
        UUID reportId,
        Long userId,
        LocalDate periodStart,
        LocalDate periodEnd,
        int loggedDays,
        String goal,
        WeeklyStats weeklyStats
) {
}