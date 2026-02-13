package ru.x5.markable.dev.analytics.gitlab.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorStatsDto {

    private String email;
    private Long commits;
    private Long addedLines;
    private Long deletedLines;
}
