package ru.x5.markable.dev.analytics.gitlab.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.LastExportTracker;
import ru.x5.markable.dev.analytics.gitlab.persistence.repository.LastExportTrackerRepository;
import ru.x5.markable.dev.analytics.gitlab.service.ExportTrackerService;

@Service
@Log4j2
@RequiredArgsConstructor
public class ExportTrackerServiceImpl implements ExportTrackerService {

    private final LastExportTrackerRepository trackerRepository;

    private static final String DAILY_STATS_TYPE = "DAILY_STATS";

    /**
     * Получить время последней выгрузки
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> getLastExportTime() {
        return trackerRepository.findLastExportTimeByType(DAILY_STATS_TYPE);
    }

    /**
     * Отметить успешную выгрузку
     */
    @Override
    @Transactional
    public void markExportSuccess(LocalDateTime exportedUntil) {
        LocalDateTime now = LocalDateTime.now();

        LastExportTracker tracker = trackerRepository.findByExportType(DAILY_STATS_TYPE)
                .orElse(LastExportTracker.builder()
                        .exportType(DAILY_STATS_TYPE)
                        .createdAt(now)
                        .build());

        tracker.setLastExportTime(exportedUntil);
        tracker.setStatus("SUCCESS");
        tracker.setErrorMessage(null);
        tracker.setUpdatedAt(now);

        trackerRepository.save(tracker);
        log.info("Export marked as successful until: {}", exportedUntil);
    }

    /**
     * Отметить неудачную выгрузку
     */
    @Override
    @Transactional
    public void markExportFailed(LocalDateTime start, LocalDateTime end, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();

        LastExportTracker tracker = trackerRepository.findByExportType(DAILY_STATS_TYPE)
                .orElse(LastExportTracker.builder()
                        .exportType(DAILY_STATS_TYPE)
                        .createdAt(now)
                        .build());

        tracker.setStatus("FAILED");
        tracker.setErrorMessage(String.format("Period %s - %s: %s", start, end, errorMessage));
        tracker.setUpdatedAt(now);

        trackerRepository.save(tracker);
        log.error("Export failed for period {} - {}. Error: {}", start, end, errorMessage);
    }

}
