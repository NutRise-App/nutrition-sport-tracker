package com.example.nutritionsporttracker.repository;

import com.example.nutritionsporttracker.model.Reports;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Reports, Long> {

    List<Reports> findByGeneratedAtAfter(LocalDateTime startDate);

    List<Reports> findByUserId(Long userId);

    List<Reports> findByUserIdAndGeneratedAtAfter(Long userId, LocalDateTime startDate);
    Optional<Reports> findByReportId(String reportId);
}
