package ru.x5.markable.dev.analytics.gitlab.interactor;

import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisRequest;

public interface AnalysisInteractor {

   void startAnalysis(AnalysisRequest request);

}
