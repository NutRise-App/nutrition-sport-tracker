package com.example.nutritionsporttracker.integration.repository;

import com.example.nutritionsporttracker.integration.MySqlContainerBaseTest;
import com.example.nutritionsporttracker.model.User;
import com.example.nutritionsporttracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryIT extends MySqlContainerBaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindUserByEmailOnMySql() {
        User user = new User();
        user.setEmail("mysql-test@example.com");
        user.setPassword("$2a$integration-test-password");
        user.setFullName("Integration Test User");
        user.setAge(24);
        user.setWeight(60.0);
        user.setHeight(165.0);
        user.setGender("FEMALE");
        user.setActivityLevel(
                User.ActivityLevel.MODERATELY_ACTIVE
        );
        user.setGoal(User.Goal.MAINTAIN_WEIGHT);

        User savedUser = userRepository.saveAndFlush(user);

        Optional<User> foundUser =
                userRepository.findByEmail(user.getEmail());

        assertNotNull(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals(
                "Integration Test User",
                foundUser.orElseThrow().getFullName()
        );
    }

    @Test
    void shouldApplyFlywayMigrationsToMySqlContainer() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = 1
                  AND version IN ('1', '2')
                """,
                Integer.class
        );

        String latestVersion = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM flyway_schema_history
                WHERE success = 1
                  AND version IS NOT NULL
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                String.class
        );

        assertEquals(2, migrationCount);
        assertEquals("2", latestVersion);
    }
}
