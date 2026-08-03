CREATE TABLE `user` (
    `age` int DEFAULT NULL,
    `calories_burned` double DEFAULT NULL,
    `carbs` double DEFAULT NULL,
    `daily_calories` double DEFAULT NULL,
    `exercise_done` int DEFAULT NULL,
    `fat` double DEFAULT NULL,
    `height` double DEFAULT NULL,
    `protein` double DEFAULT NULL,
    `water_intake` double DEFAULT NULL,
    `weight` double DEFAULT NULL,
    `created_at` datetime(6) DEFAULT NULL,
    `id` bigint NOT NULL AUTO_INCREMENT,
    `email` varchar(255) DEFAULT NULL,
    `full_name` varchar(255) DEFAULT NULL,
    `gender` varchar(255) DEFAULT NULL,
    `password` varchar(255) DEFAULT NULL,
    `activity_level`
        enum(
            'LIGHTLY_ACTIVE',
            'MODERATELY_ACTIVE',
            'SEDENTARY',
            'SUPER_ACTIVE',
            'VERY_ACTIVE'
        ) DEFAULT NULL,
    `goal`
        enum(
            'MAINTAIN_WEIGHT',
            'WEIGHT_GAIN',
            'WEIGHT_LOSS'
        ) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `cron_job_log` (
    `executed_at` datetime(6) DEFAULT NULL,
    `id` bigint NOT NULL AUTO_INCREMENT,
    `job_name` varchar(255) DEFAULT NULL,
    `status` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `email_logs` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `error_message` longtext,
    `report_date` date DEFAULT NULL,
    `sent_at` datetime(6) DEFAULT NULL,
    `status` enum('FAILED', 'SENT') DEFAULT NULL,
    `subject` varchar(255) DEFAULT NULL,
    `to_email` varchar(255) DEFAULT NULL,
    `user_id` bigint DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `motivations` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `author` varchar(255) DEFAULT NULL,
    `created_at` datetime(6) DEFAULT NULL,
    `is_active` bit(1) NOT NULL,
    `text` tinytext NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `daily_summary` (
    `date` date DEFAULT NULL,
    `total_burned` double NOT NULL,
    `total_calories` double NOT NULL,
    `total_water` double NOT NULL,
    `created_at` datetime(6) DEFAULT NULL,
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `ai_generated_at` datetime(6) DEFAULT NULL,
    `ai_report_text` longtext,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK5wic0guw42knrmx46ggknn8e8`
        (`user_id`, `date`),
    CONSTRAINT `FK9ukfji8valc9anh5wjyf8kox5`
        FOREIGN KEY (`user_id`)
        REFERENCES `user` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `meal_logs` (
    `calories` double NOT NULL,
    `carbs` double NOT NULL,
    `fat` double NOT NULL,
    `protein` double NOT NULL,
    `created_at` datetime(6) DEFAULT NULL,
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `food_name` varchar(255) DEFAULT NULL,
    `meal_time`
        enum('BREAKFAST', 'DINNER', 'LUNCH', 'SNACK')
        DEFAULT NULL,
    `grams` double NOT NULL,
    `source_food_id` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FK1oxi2h6ofwjsk9jj98e2scf0s` (`user_id`),
    CONSTRAINT `FK1oxi2h6ofwjsk9jj98e2scf0s`
        FOREIGN KEY (`user_id`)
        REFERENCES `user` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `recommendations` (
    `generated_at` datetime(6) DEFAULT NULL,
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint DEFAULT NULL,
    `recommendation_text` longtext,
    PRIMARY KEY (`id`),
    KEY `FKrk8vp22pgvt4gnr4qos5uxrha` (`user_id`),
    CONSTRAINT `FKrk8vp22pgvt4gnr4qos5uxrha`
        FOREIGN KEY (`user_id`)
        REFERENCES `user` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reports` (
    `avg_carbs` double DEFAULT NULL,
    `avg_fat` double DEFAULT NULL,
    `avg_protein` double DEFAULT NULL,
    `avg_water` double DEFAULT NULL,
    `calories_in` double DEFAULT NULL,
    `calories_out` double DEFAULT NULL,
    `generated_at` datetime(6) DEFAULT NULL,
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint DEFAULT NULL,
    `report_text` longtext,
    PRIMARY KEY (`id`),
    KEY `FKjxewlj6fgj7let57fxgt63q1q` (`user_id`),
    CONSTRAINT `FKjxewlj6fgj7let57fxgt63q1q`
        FOREIGN KEY (`user_id`)
        REFERENCES `user` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `water_intake` (
    `amount_ml` int DEFAULT NULL,
    `created_at` datetime(6) DEFAULT NULL,
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FK6bm9kx1oo67xw38uy3eb6mxtk` (`user_id`),
    CONSTRAINT `FK6bm9kx1oo67xw38uy3eb6mxtk`
        FOREIGN KEY (`user_id`)
        REFERENCES `user` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `workout_log` (
    `calories_burned` double DEFAULT NULL,
    `duration_minutes` int DEFAULT NULL,
    `created_at` datetime(6) DEFAULT NULL,
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint DEFAULT NULL,
    `exercise_name` varchar(255) DEFAULT NULL,
    `exercise_type`
        enum(
            'BALANCE',
            'CARDIO',
            'FLEXIBILITY',
            'OTHER',
            'STRENGTH'
        ) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FKndmovfc73uwi5vavbkvrc1rgq` (`user_id`),
    CONSTRAINT `FKndmovfc73uwi5vavbkvrc1rgq`
        FOREIGN KEY (`user_id`)
        REFERENCES `user` (`id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
