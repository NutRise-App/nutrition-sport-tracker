
package com.example.nutritionsporttracker.dto.ai;

public record WeeklyStats(
        Double averageDailyCaloriesIn,
        Double averageDailyCaloriesBurned,
        Double averageDailyProteinG,
        Double averageDailyCarbsG,
        Double averageDailyFatG,
        Double averageDailyWaterMl,
        Integer totalWorkoutMinutes
) {
}