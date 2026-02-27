package ru.x5.markable.dev.analytics.gitlab.service;

import java.util.List;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.AuthorStats;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisRequest;

public interface AnalysisService {

    List<AuthorStats> startAnalysis(AnalysisRequest request);

}
