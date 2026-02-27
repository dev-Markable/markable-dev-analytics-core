package ru.x5.markable.dev.analytics.gitlab.interactor;

import java.util.List;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.AuthorStats;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisRequest;

public interface AnalysisInteractor {

   List<AuthorStats> startAnalysis(AnalysisRequest request);

}
