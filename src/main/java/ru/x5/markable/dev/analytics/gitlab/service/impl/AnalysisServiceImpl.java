package ru.x5.markable.dev.analytics.gitlab.service.impl;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.x5.markable.dev.analytics.gitlab.config.GitProperties;
import ru.x5.markable.dev.analytics.gitlab.git.GitClient;
import ru.x5.markable.dev.analytics.gitlab.model.AuthorAggregate;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.AnalysisRun;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.AnalysisStatus;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.AuthorStats;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.RepoStats;
import ru.x5.markable.dev.analytics.gitlab.persistence.repository.AnalysisRunRepository;
import ru.x5.markable.dev.analytics.gitlab.persistence.repository.AuthorStatsRepository;
import ru.x5.markable.dev.analytics.gitlab.persistence.repository.RepoStatsRepository;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AnalysisRequest;
import ru.x5.markable.dev.analytics.gitlab.service.AnalysisService;

@Service
@Log4j2
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final GitClient gitClient;
    private final AnalysisRunRepository analysisRunRepository;
    private final AuthorStatsRepository authorStatsRepository;
    private final RepoStatsRepository repoStatsRepository;
    private final GitProperties gitProperties;

    private final Executor analysisExecutor;

    @Override
    @Transactional
    public UUID startAnalysis(AnalysisRequest request) {

        AnalysisRun run = AnalysisRun.builder()
                .startedAt(LocalDateTime.now())
                .sinceDate(request.getSince())
                .untilDate(request.getUntil())
                .status(AnalysisStatus.RUNNING)
                .build();

        analysisRunRepository.save(run);

        runAsync(run.getId(), request);

        return run.getId();
    }

    @Async("analysisExecutor")
    protected void runAsync(UUID analysisId, AnalysisRequest request) {

        try {

            Map<String, AuthorAggregate> globalStats = new ConcurrentHashMap<>();

            List<CompletableFuture<Void>> futures =
                    gitProperties.getRepositories().stream()
                            .map(repo ->
                                    CompletableFuture.runAsync(() ->
                                                    processRepository(repo, request, analysisId, globalStats),
                                            analysisExecutor))
                            .toList();

            CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            ).join();

            saveAggregatedStats(analysisId, globalStats);

            markSuccess(analysisId);

        } catch (Exception e) {
            markFailed(analysisId, e.getMessage());
            log.error(e.getMessage(), e);
        }
    }

    private void processRepository(String repoUrl,
            AnalysisRequest request,
            UUID analysisId,
            Map<String, AuthorAggregate> globalStats) {

        try {

            Path repoPath = gitClient.prepareRepository(repoUrl);

            List<String> lines =
                    gitClient.collectStats(repoPath,
                            request.getSince(),
                            request.getUntil());

            parseGitOutput(lines, repoUrl, analysisId, globalStats);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parseGitOutput(List<String> lines,
            String repoUrl,
            UUID analysisId,
            Map<String, AuthorAggregate> globalStats) {

        String currentEmail = null;

        Map<String, AuthorAggregate> repoStats = new HashMap<>();

        for (String line : lines) {

            if (line.contains("@") && !line.contains("\t")) {
                currentEmail = line.trim().toLowerCase();
                repoStats.putIfAbsent(currentEmail, new AuthorAggregate(currentEmail));
                continue;
            }

            if (line.contains("\t")) {
                String[] parts = line.split("\t");

                if (parts.length >= 2 && currentEmail != null) {

                    long added = parts[0].equals("-") ? 0 : Long.parseLong(parts[0]);
                    long deleted = parts[1].equals("-") ? 0 : Long.parseLong(parts[1]);

                    repoStats.get(currentEmail).add(added, deleted);
                }
            }
        }

        saveRepoStats(repoUrl, analysisId, repoStats);

        repoStats.forEach((email, stat) ->
                globalStats.computeIfAbsent(email, AuthorAggregate::new)
                        .merge(stat));
    }

    @Transactional
    protected void saveRepoStats(String repoUrl,
            UUID analysisId,
            Map<String, AuthorAggregate> repoStats) {

        String repoName = repoUrl.substring(repoUrl.lastIndexOf("/") + 1)
                .replace(".git", "");

        List<RepoStats> entities =
                repoStats.values().stream()
                        .map(stat -> RepoStats.builder()
                                .analysisId(analysisId)
                                .repositoryName(repoName)
                                .email(stat.email())
                                .commits(stat.commits())
                                .addedLines(stat.added())
                                .deletedLines(stat.deleted())
                                .build())
                        .toList();

        repoStatsRepository.saveAll(entities);
    }

    @Transactional
    protected void saveAggregatedStats(UUID analysisId, Map<String, AuthorAggregate> globalStats) {

        List<AuthorStats> entities =
                globalStats.values().stream()
                        .map(stat -> AuthorStats.builder()
                                .analysisId(analysisId)
                                .email(stat.email())
                                .commits(stat.commits())
                                .addedLines(stat.added())
                                .deletedLines(stat.deleted())
                                .build())
                        .toList();

        authorStatsRepository.saveAll(entities);
    }

    private void markSuccess(UUID id) {
        AnalysisRun run = analysisRunRepository.findById(id).orElseThrow();
        run.setStatus(AnalysisStatus.SUCCESS);
        run.setFinishedAt(LocalDateTime.now());
        analysisRunRepository.save(run);
    }

    private void markFailed(UUID id, String error) {
        AnalysisRun run = analysisRunRepository.findById(id).orElseThrow();
        run.setStatus(AnalysisStatus.FAILED);
        run.setErrorMessage(error);
        run.setFinishedAt(LocalDateTime.now());
        analysisRunRepository.save(run);
    }
}
