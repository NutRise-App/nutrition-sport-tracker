CREATE INDEX `idx_user_email`
    ON `user` (`email`);

CREATE INDEX `idx_meal_logs_user_created_at`
    ON `meal_logs` (`user_id`, `created_at`);

CREATE INDEX `idx_water_intake_user_created_at`
    ON `water_intake` (`user_id`, `created_at`);

CREATE INDEX `idx_workout_log_user_created_at`
    ON `workout_log` (`user_id`, `created_at`);

CREATE INDEX `idx_reports_user_generated_at`
    ON `reports` (`user_id`, `generated_at`);
