package ru.x5.markable.dev.analytics.gitlab.rest.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCommitStatsDto {
    private LocalDate date;
    private long totalCommits;
    private long totalMergeCommits;
    private long totalAddedLines;
    private long totalDeletedLines;
    private long totalTestAddedLines;
}
