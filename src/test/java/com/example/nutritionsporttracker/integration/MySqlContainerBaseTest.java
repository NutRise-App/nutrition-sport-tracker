package com.example.nutritionsporttracker.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class MySqlContainerBaseTest {

    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(
                    DockerImageName.parse("mysql:8.0.36")
            )
                    .withDatabaseName("nutrise_integration_test")
                    .withUsername("nutrise_test")
                    .withPassword("nutrise_test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureMySqlProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                MYSQL::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                MYSQL::getUsername
        );
        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver"
        );

        registry.add(
                "spring.jpa.database-platform",
                () -> "org.hibernate.dialect.MySQLDialect"
        );
        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate"
        );

        registry.add(
                "spring.flyway.enabled",
                () -> "true"
        );
        registry.add(
                "spring.flyway.baseline-on-migrate",
                () -> "false"
        );
    }
}
