package ru.x5.markable.dev.analytics.gitlab.service;

import java.time.LocalDateTime;
import java.util.List;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.DailyCommitStatsDto;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.DailyUserStatsDto;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.PeriodSummaryDto;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.WeeklyCommitStatsDto;

public interface DailyStatsService {

    void collectDailyStats();

    void collectStatsForPeriod(LocalDateTime start, LocalDateTime end);

    List<DailyCommitStatsDto> getAllDailyCommits();
    List<DailyUserStatsDto> getAllDailyUserStats();
    PeriodSummaryDto getPeriodSummary();

    List<WeeklyCommitStatsDto> getWeeklyCommits();

}
