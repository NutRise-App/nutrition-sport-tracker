package com.example.nutritionsporttracker.integration.repository;

import com.example.nutritionsporttracker.integration.MySqlContainerBaseTest;
import com.example.nutritionsporttracker.model.MealLog;
import com.example.nutritionsporttracker.model.MealTimeType;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.repository.MealLogRepository;
import com.example.nutritionsporttracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MealLogRepositoryIT extends MySqlContainerBaseTest {

    @Autowired
    private MealLogRepository mealLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindMealLogByUserAndDateRange() {
        User user = createAndSaveUser();

        MealLog mealLog = new MealLog();
        mealLog.setUser(user);
        mealLog.setFoodName("Oatmeal");
        mealLog.setMealTime(MealTimeType.BREAKFAST);
        mealLog.setGrams(100.0);
        mealLog.setCalories(350.0);
        mealLog.setProtein(12.0);
        mealLog.setCarbs(60.0);
        mealLog.setFat(7.0);
        mealLog.setSourceFoodId("integration-food-1");

        MealLog savedMeal =
                mealLogRepository.saveAndFlush(mealLog);

        LocalDateTime createdAt = savedMeal.getCreatedAt();

        List<MealLog> results =
                mealLogRepository.findByUserIdAndCreatedAtBetween(
                        user.getId(),
                        createdAt.minusMinutes(1),
                        createdAt.plusMinutes(1)
                );

        assertNotNull(savedMeal.getId());
        assertEquals(1, results.size());
        assertEquals(
                "Oatmeal",
                results.get(0).getFoodName()
        );
        assertEquals(
                user.getId(),
                results.get(0).getUser().getId()
        );
    }

    @Test
    void shouldRejectMealLogWithUnknownUserId() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO meal_logs (
                            calories,
                            carbs,
                            fat,
                            protein,
                            grams,
                            user_id,
                            food_name
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        100.0,
                        10.0,
                        5.0,
                        5.0,
                        100.0,
                        999999999L,
                        "Invalid foreign key meal"
                )
        );
    }

    private User createAndSaveUser() {
        User user = new User();
        user.setEmail("meal-it@example.com");
        user.setPassword("$2a$integration-test-password");
        user.setFullName("Meal Integration User");

        return userRepository.saveAndFlush(user);
    }
}
