package ru.x5.markable.dev.analytics.rest.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisResponse {

    private UUID analysisId;
    private String status;
}