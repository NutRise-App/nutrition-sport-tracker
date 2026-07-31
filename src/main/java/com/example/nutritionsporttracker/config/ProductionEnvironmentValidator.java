package com.example.nutritionsporttracker.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionEnvironmentValidator {

    private static final List<String> REQUIRED_VARIABLES = List.of(
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_USERNAME",
            "SPRING_DATASOURCE_PASSWORD",
            "JWT_SECRET",
            "MAIL_USERNAME",
            "MAIL_PASSWORD",
            "OPENROUTER_API_KEY",
            "USDA_API_KEY",
            "CORS_ALLOWED_ORIGIN_PATTERNS"
    );

    private static final Set<String> INSECURE_VALUES = Set.of(
            "CHANGE_ME",
            "dev-disabled",
            "test-disabled",
            "test_key_placeholder",
            "GENERATE_A_STRONG_RANDOM_SECRET"
    );

    @Bean
    static BeanFactoryPostProcessor validateProductionEnvironment(
            Environment environment
    ) {
        return beanFactory -> {
            List<String> problems = new ArrayList<>();

            for (String variable : REQUIRED_VARIABLES) {
                String value = environment.getProperty(variable);

                if (!StringUtils.hasText(value)) {
                    problems.add(variable + " is missing");
                    continue;
                }

                if (INSECURE_VALUES.contains(value.trim())) {
                    problems.add(variable + " contains an insecure placeholder");
                }
            }

            String jwtSecret = environment.getProperty("JWT_SECRET");

            if (StringUtils.hasText(jwtSecret) && jwtSecret.length() < 32) {
                problems.add("JWT_SECRET must contain at least 32 characters");
            }

            String corsOrigins =
                    environment.getProperty("CORS_ALLOWED_ORIGIN_PATTERNS");

            if (StringUtils.hasText(corsOrigins)) {
                List<String> origins = List.of(corsOrigins.split(","))
                        .stream()
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList();

                if (origins.contains("*")) {
                    problems.add(
                            "CORS_ALLOWED_ORIGIN_PATTERNS cannot contain a global wildcard"
                    );
                }

                boolean containsLocalhost = origins.stream().anyMatch(origin ->
                        origin.contains("localhost")
                                || origin.contains("127.0.0.1")
                );

                if (containsLocalhost) {
                    problems.add(
                            "Production CORS configuration cannot contain localhost"
                    );
                }
            }

            if (!problems.isEmpty()) {
                throw new IllegalStateException(
                        "Invalid production configuration: "
                                + String.join("; ", problems)
                );
            }
        };
    }
}
