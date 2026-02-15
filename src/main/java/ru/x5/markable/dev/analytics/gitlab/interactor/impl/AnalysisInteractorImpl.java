package ru.x5.markable.dev.analytics.gitlab.interactor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import ru.x5.markable.dev.analytics.commons.exceptions.UnprocessableEntityException;
import ru.x5.markable.dev.analytics.gitlab.exception.AnalysisException;
import ru.x5.markable.dev.analytics.gitlab.exception.RepositoryAnalysisException;
import ru.x5.markable.dev.analytics.gitlab.exception.StatisticsPersistenceException;
import ru.x5.markable.dev.analytics.gitlab.interactor.AnalysisInteractor;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisRequest;
import ru.x5.markable.dev.analytics.gitlab.service.AnalysisService;

import static ru.x5.markable.dev.analytics.gitlab.interactor.Message.ANALYZE_REPOSITORY_ERROR;
import static ru.x5.markable.dev.analytics.gitlab.interactor.Message.GIT_ERROR;
import static ru.x5.markable.dev.analytics.gitlab.interactor.Message.STATISTICS_SAVE_ERROR;

@Log4j2
@Component
@RequiredArgsConstructor
public class AnalysisInteractorImpl implements AnalysisInteractor {

    private final AnalysisService analysisService;

    @Override
    public void startAnalysis(AnalysisRequest request) {
        try {
            analysisService.startAnalysis(request);
        } catch (RepositoryAnalysisException e) {
            throw new UnprocessableEntityException(ANALYZE_REPOSITORY_ERROR, e.getRepository());
        } catch (StatisticsPersistenceException e) {
            throw new UnprocessableEntityException(STATISTICS_SAVE_ERROR);
        } catch (AnalysisException e) {
            throw new UnprocessableEntityException(GIT_ERROR);
        }
    }
}
