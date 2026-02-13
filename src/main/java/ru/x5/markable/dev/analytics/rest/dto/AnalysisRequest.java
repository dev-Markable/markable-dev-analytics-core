package ru.x5.markable.dev.analytics.rest.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AnalysisRequest {
    private List<String> repositories;
    private LocalDate since;
    private LocalDate until;
}
