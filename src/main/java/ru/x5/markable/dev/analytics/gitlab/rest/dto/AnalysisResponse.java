package ru.x5.markable.dev.analytics.gitlab.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisResponse {

    private String email;
    long mergeCommits;
    long commits;
    long added;
    long deleted;
    long testAdded;
}