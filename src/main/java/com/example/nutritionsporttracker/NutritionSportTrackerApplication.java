package com.example.nutritionsporttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@PropertySources({
        @PropertySource("classpath:application.yml"),
        @PropertySource(value = "classpath:application-dev.yml", ignoreResourceNotFound = true)
})

public class NutritionSportTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutritionSportTrackerApplication.class, args);
    }

}
