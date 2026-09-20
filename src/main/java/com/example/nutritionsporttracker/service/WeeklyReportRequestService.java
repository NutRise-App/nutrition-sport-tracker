
package com.example.nutritionsporttracker.service;

import com.example.nutritionsporttracker.dto.ai.AiReportRequest;
import com.example.nutritionsporttracker.dto.ai.WeeklyStats;
import com.example.nutritionsporttracker.model.MealLog;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.model.WaterIntake;
import com.example.nutritionsporttracker.model.WorkoutLog;
import com.example.nutritionsporttracker.repository.MealLogRepository;
import com.example.nutritionsporttracker.repository.UserRepository;
import com.example.nutritionsporttracker.repository.WaterIntakeRepository;
import com.example.nutritionsporttracker.repository.WorkoutLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WeeklyReportRequestService {

    private static final ZoneId REPORT_ZONE =
            ZoneId.of("Europe/Istanbul");

    private final UserRepository userRepository;
    private final MealLogRepository mealLogRepository;
    private final WaterIntakeRepository waterIntakeRepository;
    private final WorkoutLogRepository workoutLogRepository;

    public WeeklyReportRequestService(
            UserRepository userRepository,
            MealLogRepository mealLogRepository,
            WaterIntakeRepository waterIntakeRepository,
            WorkoutLogRepository workoutLogRepository
    ) {
        this.userRepository = userRepository;
        this.mealLogRepository = mealLogRepository;
        this.waterIntakeRepository = waterIntakeRepository;
        this.workoutLogRepository = workoutLogRepository;
    }

    public AiReportRequest buildRequest(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Kullanıcı bulunamadı")
                );

        // Bugünü dahil etmeden, tamamlanmış son 7 gün.
        LocalDate periodEnd = LocalDate.now(REPORT_ZONE).minusDays(1);
        LocalDate periodStart = periodEnd.minusDays(6);

        LocalDateTime start = periodStart.atStartOfDay();
        LocalDateTime end = periodEnd.plusDays(1)
                .atStartOfDay()
                .minusNanos(1);

        Long userId = user.getId();

        List<MealLog> meals =
                mealLogRepository.findByUserIdAndCreatedAtBetween(
                        userId, start, end
                );

        List<WaterIntake> waterLogs =
                waterIntakeRepository.findByUserIdAndCreatedAtBetween(
                        userId, start, end
                );

        List<WorkoutLog> workouts =
                workoutLogRepository.findByUserIdAndCreatedAtBetween(
                        userId, start, end
                );

        Set<LocalDate> loggedDates = new HashSet<>();
        Set<LocalDate> mealDates = new HashSet<>();
        Set<LocalDate> waterDates = new HashSet<>();
        Set<LocalDate> workoutDates = new HashSet<>();

        for (MealLog meal : meals) {
            LocalDate date = meal.getCreatedAt().toLocalDate();
            mealDates.add(date);
            loggedDates.add(date);
        }

        for (WaterIntake water : waterLogs) {
            LocalDate date = water.getCreatedAt().toLocalDate();
            waterDates.add(date);
            loggedDates.add(date);
        }

        for (WorkoutLog workout : workouts) {
            LocalDate date = workout.getCreatedAt().toLocalDate();
            workoutDates.add(date);
            loggedDates.add(date);
        }

        if (loggedDates.isEmpty()) {
            throw new IllegalStateException(
                    "Son 7 günde rapor oluşturulacak kayıt bulunamadı"
            );
        }

        Double averageCaloriesIn = mealDates.isEmpty()
                ? null
                : meals.stream()
                        .mapToDouble(MealLog::getCalories)
                        .sum() / mealDates.size();

        Double averageProtein = mealDates.isEmpty()
                ? null
                : meals.stream()
                        .mapToDouble(MealLog::getProtein)
                        .sum() / mealDates.size();

        Double averageCarbs = mealDates.isEmpty()
                ? null
                : meals.stream()
                        .mapToDouble(MealLog::getCarbs)
                        .sum() / mealDates.size();

        Double averageFat = mealDates.isEmpty()
                ? null
                : meals.stream()
                        .mapToDouble(MealLog::getFat)
                        .sum() / mealDates.size();

        Double averageWaterMl = waterDates.isEmpty()
                ? null
                : waterLogs.stream()
                        .mapToDouble(WaterIntake::getAmountMl)
                        .sum() / waterDates.size();

        Double averageCaloriesBurned = workoutDates.isEmpty()
                ? null
                : workouts.stream()
                        .mapToDouble(workout ->
                                workout.getCaloriesBurned() == null
                                        ? 0.0
                                        : workout.getCaloriesBurned()
                        )
                        .sum() / workoutDates.size();

        Integer totalWorkoutMinutes = workouts.isEmpty()
                ? null
                : Math.toIntExact(
                        workouts.stream()
                                .mapToLong(workout ->
                                        workout.getDurationMinutes() == null
                                                ? 0L
                                                : workout.getDurationMinutes()
                                )
                                .sum()
                );

        WeeklyStats stats = new WeeklyStats(
                averageCaloriesIn,
                averageCaloriesBurned,
                averageProtein,
                averageCarbs,
                averageFat,
                averageWaterMl,
                totalWorkoutMinutes
        );

        String goal = user.getGoal() == null
                ? null
                : String.valueOf(user.getGoal());

        return new AiReportRequest(
                UUID.randomUUID(),
                userId,
                periodStart,
                periodEnd,
                loggedDates.size(),
                goal,
                stats
        );
    }
}