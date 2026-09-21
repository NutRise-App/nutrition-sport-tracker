package com.example.nutritionsporttracker.repository;

import com.example.nutritionsporttracker.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Optional<EmailLog> findByReportId(String reportId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE EmailLog e
               SET e.status = :nextStatus,
                   e.sentAt = :sentAt,
                   e.errorMessage = :errorMessage
             WHERE e.reportId = :reportId
               AND e.status = :currentStatus
            """)
    int changeStatus(
            @Param("reportId") String reportId,
            @Param("currentStatus") EmailLog.Status currentStatus,
            @Param("nextStatus") EmailLog.Status nextStatus,
            @Param("sentAt") LocalDateTime sentAt,
            @Param("errorMessage") String errorMessage
    );
}