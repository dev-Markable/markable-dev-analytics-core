package ru.x5.markable.dev.analytics.gitlab.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ExportTrackerService {

    Optional<LocalDateTime> getLastExportTime();

    void markExportSuccess(LocalDateTime exportedUntil);

    void markExportFailed(LocalDateTime start, LocalDateTime end, String errorMessage);

}
