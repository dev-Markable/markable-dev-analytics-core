package ru.x5.markable.dev.analytics.gitlab.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

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

    // ============================================================
    // START
    // ============================================================

    @Override
    @Transactional
    public UUID startAnalysis(AnalysisRequest request) {

        log.info("Starting analysis. Period: {} - {}",
                request.getSince(), request.getUntil());

        AnalysisRun run = AnalysisRun.builder()
                .startedAt(LocalDateTime.now())
                .sinceDate(request.getSince())
                .untilDate(request.getUntil())
                .status(AnalysisStatus.RUNNING)
                .build();

        analysisRunRepository.save(run);

        executeAsync(run.getId(), request);

        return run.getId();
    }

    // ============================================================
    // ASYNC EXECUTION (ОДИН уровень async)
    // ============================================================

    @Async("analysisExecutor")
    protected void executeAsync(UUID analysisId, AnalysisRequest request) {

        long totalStart = System.currentTimeMillis();

        try {

            Map<String, AuthorAggregate> globalStats =
                    new ConcurrentHashMap<>();

            log.info("Processing {} repositories",
                    gitProperties.getRepositories().size());

            for (String repoUrl : gitProperties.getRepositories()) {
                processRepository(repoUrl, request, analysisId, globalStats);
            }

            saveAggregatedStats(analysisId, globalStats);
            markSuccess(analysisId);

            log.info("Analysis {} completed in {} ms",
                    analysisId,
                    System.currentTimeMillis() - totalStart);

        } catch (Exception e) {
            markFailed(analysisId, e.getMessage());
            log.error("Analysis {} failed", analysisId, e);
        }
    }

    // ============================================================
    // REPOSITORY PROCESSING
    // ============================================================

    private void processRepository(String repoUrl,
            AnalysisRequest request,
            UUID analysisId,
            Map<String, AuthorAggregate> globalStats) throws IOException, InterruptedException {

        String repoName = extractRepoName(repoUrl);

        long start = System.currentTimeMillis();

        log.info("Processing repository [{}]", repoName);

        Path repoPath = gitClient.prepareRepository(repoUrl);

        List<String> lines =
                gitClient.collectStats(repoPath,
                        request.getSince(),
                        request.getUntil());

        log.info("Git returned {} lines for repo {}",
                lines.size(), repoName);

        Map<String, AuthorAggregate> repoStats =
                parseGitOutput(lines);

        saveRepoStats(repoName, analysisId, repoStats);

        repoStats.forEach((email, stat) ->
                globalStats.merge(email, stat, AuthorAggregate::merge));

        log.info("Repository [{}] processed in {} ms",
                repoName,
                System.currentTimeMillis() - start);
    }

    // ============================================================
    // GIT OUTPUT PARSER (1 commit = 1 email строка)
    // ============================================================

    private Map<String, AuthorAggregate> parseGitOutput(List<String> lines) {

        Map<String, AuthorAggregate> repoStats = new HashMap<>();
        String currentEmail = null;
        boolean currentCommitIsMerge;

        for (String rawLine : lines) {

            if (rawLine == null) continue;

            String line = rawLine.trim();

            if (line.isEmpty()) continue;

            // EMAIL = новый commit
            if (!line.contains("\t") && line.contains("@")) {

                String[] parts = line.split("\\|");

                currentEmail = parts[0].trim().toLowerCase();
                currentCommitIsMerge =
                        parts.length > 1 &&
                                parts[1] != null &&
                                parts[1].trim().contains(" ");

                boolean finalCurrentCommitIsMerge = currentCommitIsMerge;

                repoStats.compute(currentEmail, (email, aggregate) -> {

                    if (aggregate == null) {
                        aggregate = new AuthorAggregate(email);
                    }

                    return aggregate.addCommit(finalCurrentCommitIsMerge);
                });

                continue;
            }

            // NUMSTAT
            if (currentEmail != null && line.contains("\t")) {

                String[] parts = line.split("\t");

                if (parts.length < 3) {
                    continue;
                }

                // бинарные файлы отображаются как "-"
                if (parts[0].equals("-") || parts[1].equals("-")) {
                    continue;
                }

                long added;
                long deleted;

                try {
                    added = Long.parseLong(parts[0]);
                    deleted = Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    continue;
                }

                String fileName = parts[2];

                boolean isTestFile = isTestFile(fileName);

                repoStats.computeIfPresent(currentEmail, (email, aggregate) ->
                        aggregate.addLines(added, deleted, isTestFile)
                );
            }
        }

        return repoStats;
    }

    private boolean isTestFile(String fileName) {

        if (fileName == null) {
            return false;
        }

        String lower = fileName.toLowerCase();

        return lower.contains("/test/")
                || lower.endsWith("test.java")
                || lower.endsWith("tests.java");
    }

    // ============================================================
    // SAVE METHODS
    // ============================================================

    @Transactional
    protected void saveRepoStats(String repoName,
            UUID analysisId,
            Map<String, AuthorAggregate> repoStats) {

        List<RepoStats> entities =
                repoStats.values().stream()
                        .map(stat -> RepoStats.builder()
                                .analysisId(analysisId)
                                .repositoryName(repoName)
                                .email(stat.email())
                                .mergeCommits(stat.mergeCommits())
                                .commits(stat.commits())
                                .addedLines(stat.added())
                                .deletedLines(stat.deleted())
                                .testAddedLines(stat.testAdded())
                                .build())
                        .toList();

        repoStatsRepository.saveAll(entities);
    }

    @Transactional
    protected void saveAggregatedStats(UUID analysisId,
            Map<String, AuthorAggregate> globalStats) {

        List<AuthorStats> entities =
                globalStats.values().stream()
                        .map(stat -> AuthorStats.builder()
                                .analysisId(analysisId)
                                .email(stat.email())
                                .mergeCommits(stat.mergeCommits())
                                .commits(stat.commits())
                                .addedLines(stat.added())
                                .deletedLines(stat.deleted())
                                .testAddedLines(stat.testAdded())
                                .build())
                        .toList();

        authorStatsRepository.saveAll(entities);
    }

    // ============================================================
    // STATUS
    // ============================================================

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

    private String extractRepoName(String repoUrl) {
        return repoUrl.substring(repoUrl.lastIndexOf("/") + 1)
                .replace(".git", "");
    }
}