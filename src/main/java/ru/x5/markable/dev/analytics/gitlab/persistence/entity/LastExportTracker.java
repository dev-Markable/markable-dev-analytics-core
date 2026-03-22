package ru.x5.markable.dev.analytics.gitlab.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "last_export_tracker")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastExportTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "export_type", nullable = false, unique = true)
    private String exportType; // например: "DAILY_STATS"

    @Column(name = "last_export_time", nullable = false)
    private LocalDateTime lastExportTime;

    @Column(name = "status")
    private String status; // SUCCESS, FAILED

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}