package ru.x5.markable.dev.analytics.gitlab.rest.dto;

import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodSummaryDto {
    private long totalCommits;
    private long totalMergeCommits;
    private long totalAddedLines;
    private long totalDeletedLines;
    private long totalTestAddedLines;
    private long uniqueAuthors;
    private LocalDate dateFrom; // минимальная дата в данных
    private LocalDate dateTo;   // максимальная дата в данных
    private Map<String, AuthorSummaryDto> topAuthors;
}
