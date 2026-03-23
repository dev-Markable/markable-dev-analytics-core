package ru.x5.markable.dev.analytics.gitlab.rest.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyCommitStatsDto {

    private int weekNumber;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private long totalCommits;
    private long totalMergeCommits;
    private long totalAddedLines;
    private long totalDeletedLines;
    private long totalTestAddedLines;
    private long uniqueAuthors;
    private Map<String, AuthorWeeklySummaryDto> topAuthors;
}
