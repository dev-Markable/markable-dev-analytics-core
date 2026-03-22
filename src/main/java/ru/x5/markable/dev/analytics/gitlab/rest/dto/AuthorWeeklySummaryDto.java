package ru.x5.markable.dev.analytics.gitlab.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorWeeklySummaryDto {
    private String email;
    private long commits;
    private long mergeCommits;
    private long addedLines;
    private long deletedLines;
    private long testAddedLines;
}
