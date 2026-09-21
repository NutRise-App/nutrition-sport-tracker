ALTER TABLE email_logs
    ADD COLUMN report_id VARCHAR(36) NULL;

ALTER TABLE email_logs
    ADD CONSTRAINT uq_email_logs_report_id UNIQUE (report_id);

ALTER TABLE email_logs
    MODIFY COLUMN status ENUM('PENDING', 'SENT', 'FAILED') DEFAULT NULL;