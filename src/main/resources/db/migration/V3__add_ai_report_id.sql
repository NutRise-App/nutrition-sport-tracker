ALTER TABLE reports
    ADD COLUMN report_id VARCHAR(36) NULL;

ALTER TABLE reports
    ADD CONSTRAINT uq_reports_report_id UNIQUE (report_id);