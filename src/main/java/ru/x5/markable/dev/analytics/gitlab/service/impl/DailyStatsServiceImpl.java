package ru.x5.markable.dev.analytics.gitlab.service.impl;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.x5.markable.dev.analytics.gitlab.config.GitProperties;
import ru.x5.markable.dev.analytics.gitlab.git.GitClient;
import ru.x5.markable.dev.analytics.gitlab.model.AuthorAggregate;
import ru.x5.markable.dev.analytics.gitlab.model.CommitDetail;
import ru.x5.markable.dev.analytics.gitlab.persistence.entity.DailyAuthorStats;
import ru.x5.markable.dev.analytics.gitlab.persistence.repository.DailyAuthorStatsRepository;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AuthorSummaryDto;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.AuthorWeeklySummaryDto;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.DailyCommitStatsDto;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.DailyUserStatsDto;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.PeriodSummaryDto;
import ru.x5.markable.dev.analytics.gitlab.rest.dto.WeeklyCommitStatsDto;
import ru.x5.markable.dev.analytics.gitlab.service.DailyStatsService;
import ru.x5.markable.dev.analytics.gitlab.service.ExportTrackerService;

@Service
@Log4j2
@RequiredArgsConstructor
public class DailyStatsServiceImpl implements DailyStatsService {

    private final GitClient gitClient;
    private final GitProperties gitProperties;
    private final DailyAuthorStatsRepository dailyStatsRepository;
    private final Executor analysisExecutor;
    private final ExportTrackerService exportTrackerService;

    private static final LocalDateTime DEFAULT_START_DATE = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Запускается каждый день в 01:00
     * Собирает статистику с даты последней выгрузки до текущего момента
     */
    @Override
    @Scheduled(cron = "0 0 1 * * ?") // каждый день в 01:00
    @Transactional
    public void collectDailyStats() {
        log.info("Starting daily stats collection...");

        LocalDateTime lastExport = exportTrackerService.getLastExportTime()
                .orElse(DEFAULT_START_DATE);

        LocalDateTime now = LocalDateTime.now();

        // Начинаем собирать СРАЗУ ПОСЛЕ последней выгрузки
        LocalDateTime startFrom = lastExport.plusSeconds(1);

        if (startFrom.isAfter(now)) {
            log.info("All stats are up to date. Last export: {}", lastExport);
            return;
        }

        log.info("Collecting stats from {} to {}", startFrom, now);
        collectStatsForPeriod(startFrom, now);
    }

    /**
     * Собрать статистику за период с учетом времени
     */
    @Override
    @Transactional
    public void collectStatsForPeriod(LocalDateTime start, LocalDateTime end) {
        log.info("Starting collection for period: {} - {}", start, end);

        try {
            // Собираем статистику с детальными данными (включая даты коммитов)
            List<CommitDetail> commitDetails = collectCommitDetails(start, end);

            // Группируем по дням и пользователям
            Map<LocalDate, Map<String, AuthorAggregate>> dailyStats = groupCommitsByDay(commitDetails);

            // Сохраняем в БД
            saveDailyStats(dailyStats);

            exportTrackerService.markExportSuccess(end);
            log.info("Successfully collected stats from {} to {}", start, end);

        } catch (Exception e) {
            exportTrackerService.markExportFailed(start, end, e.getMessage());
            log.error("Failed to collect stats from {} to {}", start, end, e);
        }
    }

    @Override
    public PeriodSummaryDto getPeriodSummary() {
        log.info("Fetching summary for all available data");

        // Получаем все записи
        List<DailyAuthorStats> allStats = dailyStatsRepository.findAll();

        if (allStats.isEmpty()) {
            return PeriodSummaryDto.builder()
                    .totalCommits(0)
                    .totalMergeCommits(0)
                    .totalAddedLines(0)
                    .totalDeletedLines(0)
                    .totalTestAddedLines(0)
                    .uniqueAuthors(0)
                    .topAuthors(new LinkedHashMap<>())
                    .build();
        }

        // Находим минимальную и максимальную дату
        LocalDate minDate = allStats.stream()
                .map(DailyAuthorStats::getDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate maxDate = allStats.stream()
                .map(DailyAuthorStats::getDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        // Группируем по пользователям
        Map<String, AuthorSummaryDto> authorMap = new HashMap<>();
        long totalCommits = 0;
        long totalMergeCommits = 0;
        long totalAdded = 0;
        long totalDeleted = 0;
        long totalTestAdded = 0;

        for (DailyAuthorStats stat : allStats) {
            totalCommits += stat.getCommits();
            totalMergeCommits += stat.getMergeCommits() != null ? stat.getMergeCommits() : 0;
            totalAdded += stat.getAddedLines();
            totalDeleted += stat.getDeletedLines();
            totalTestAdded += stat.getTestAddedLines() != null ? stat.getTestAddedLines() : 0;

            authorMap.compute(stat.getEmail(), (email, summary) -> {
                if (summary == null) {
                    summary = AuthorSummaryDto.builder()
                            .email(email)
                            .commits(0)
                            .addedLines(0)
                            .deletedLines(0)
                            .build();
                }
                summary.setCommits(summary.getCommits() + stat.getCommits());
                summary.setAddedLines(summary.getAddedLines() + stat.getAddedLines());
                summary.setDeletedLines(summary.getDeletedLines() + stat.getDeletedLines());
                return summary;
            });
        }

        // Сортируем пользователей по количеству коммитов и берем топ-10
        Map<String, AuthorSummaryDto> topAuthors = authorMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().getCommits(), e1.getValue().getCommits()))
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return PeriodSummaryDto.builder()
                .totalCommits(totalCommits)
                .totalMergeCommits(totalMergeCommits)
                .totalAddedLines(totalAdded)
                .totalDeletedLines(totalDeleted)
                .totalTestAddedLines(totalTestAdded)
                .uniqueAuthors(authorMap.size())
                .topAuthors(topAuthors)
                .dateFrom(minDate)
                .dateTo(maxDate)
                .build();
    }

    @Override
    public List<WeeklyCommitStatsDto> getWeeklyCommits() {
        log.info("Fetching weekly commits statistics");

        List<DailyAuthorStats> allStats = dailyStatsRepository.findAll();

        if (allStats.isEmpty()) {
            return Collections.emptyList();
        }

        // Группируем по неделям
        Map<Integer, Map<String, List<DailyAuthorStats>>> weeklyData = new TreeMap<>();

        for (DailyAuthorStats stat : allStats) {
            LocalDate date = stat.getDate();
            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            int year = date.getYear();
            int weekKey = year * 100 + week; // уникальный ключ: год + неделя

            weeklyData.computeIfAbsent(weekKey, k -> new HashMap<>());
            weeklyData.get(weekKey).computeIfAbsent(stat.getEmail(), k -> new ArrayList<>())
                    .add(stat);
        }

        List<WeeklyCommitStatsDto> result = new ArrayList<>();

        for (Map.Entry<Integer, Map<String, List<DailyAuthorStats>>> entry : weeklyData.entrySet()) {
            int weekKey = entry.getKey();
            int year = weekKey / 100;
            int week = weekKey % 100;
            Map<String, List<DailyAuthorStats>> userData = entry.getValue();

            // Находим начало и конец недели
            LocalDate weekStart = LocalDate.of(year, 1, 1)
                    .with(WeekFields.ISO.weekOfWeekBasedYear(), week)
                    .with(WeekFields.ISO.dayOfWeek(), 1); // понедельник
            LocalDate weekEnd = weekStart.plusDays(6); // воскресенье

            // Агрегируем данные за неделю
            long totalCommits = 0;
            long totalMergeCommits = 0;
            long totalAdded = 0;
            long totalDeleted = 0;
            long totalTestAdded = 0;

            // 👇 Используем AuthorWeeklySummaryDto вместо AuthorSummaryDto
            Map<String, AuthorWeeklySummaryDto> authors = new HashMap<>();

            for (Map.Entry<String, List<DailyAuthorStats>> userEntry : userData.entrySet()) {
                String email = userEntry.getKey();
                long userCommits = 0;
                long userMergeCommits = 0;
                long userAdded = 0;
                long userDeleted = 0;
                long userTestAdded = 0;

                for (DailyAuthorStats stat : userEntry.getValue()) {
                    userCommits += stat.getCommits();
                    userMergeCommits += stat.getMergeCommits() != null ? stat.getMergeCommits() : 0;
                    userAdded += stat.getAddedLines();
                    userDeleted += stat.getDeletedLines();
                    userTestAdded += stat.getTestAddedLines() != null ? stat.getTestAddedLines() : 0;

                    totalCommits += stat.getCommits();
                    totalMergeCommits += stat.getMergeCommits() != null ? stat.getMergeCommits() : 0;
                    totalAdded += stat.getAddedLines();
                    totalDeleted += stat.getDeletedLines();
                    totalTestAdded += stat.getTestAddedLines() != null ? stat.getTestAddedLines() : 0;
                }

                // 👇 Используем AuthorWeeklySummaryDto
                authors.put(email, AuthorWeeklySummaryDto.builder()
                        .email(email)
                        .commits(userCommits)
                        .mergeCommits(userMergeCommits)
                        .addedLines(userAdded)
                        .deletedLines(userDeleted)
                        .testAddedLines(userTestAdded)
                        .build());
            }

            // Сохраняем всех авторов без ограничения по количеству
            Map<String, AuthorWeeklySummaryDto> allAuthors = authors.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue().getCommits(), e1.getValue().getCommits()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));

            result.add(WeeklyCommitStatsDto.builder()
                    .weekNumber(week)
                    .weekStart(weekStart)
                    .weekEnd(weekEnd)
                    .totalCommits(totalCommits)
                    .totalMergeCommits(totalMergeCommits)
                    .totalAddedLines(totalAdded)
                    .totalDeletedLines(totalDeleted)
                    .totalTestAddedLines(totalTestAdded)
                    .uniqueAuthors(authors.size())
                    .topAuthors(allAuthors)
                    .build());
        }

        // Сортируем по неделе (от старых к новым)
        result.sort(Comparator.comparing(WeeklyCommitStatsDto::getWeekStart));

        log.info("Found {} weeks of data", result.size());
        return result;
    }

    @Override
    public List<DailyCommitStatsDto> getAllDailyCommits() {
        log.info("Fetching all daily commits");

        List<DailyAuthorStats> allStats = dailyStatsRepository.findAll();

        // Группируем по датам
        Map<LocalDate, DailyCommitStatsDto> dailyMap = new LinkedHashMap<>();

        for (DailyAuthorStats stat : allStats) {
            dailyMap.compute(stat.getDate(), (date, dto) -> {
                if (dto == null) {
                    dto = DailyCommitStatsDto.builder()
                            .date(date)
                            .totalCommits(0)
                            .totalMergeCommits(0)
                            .totalAddedLines(0)
                            .totalDeletedLines(0)
                            .totalTestAddedLines(0)
                            .build();
                }
                dto.setTotalCommits(dto.getTotalCommits() + stat.getCommits());
                dto.setTotalMergeCommits(dto.getTotalMergeCommits() +
                        (stat.getMergeCommits() != null ? stat.getMergeCommits() : 0));
                dto.setTotalAddedLines(dto.getTotalAddedLines() + stat.getAddedLines());
                dto.setTotalDeletedLines(dto.getTotalDeletedLines() + stat.getDeletedLines());
                dto.setTotalTestAddedLines(dto.getTotalTestAddedLines() +
                        (stat.getTestAddedLines() != null ? stat.getTestAddedLines() : 0));
                return dto;
            });
        }

        // Сортируем по дате
        return dailyMap.values().stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DailyUserStatsDto> getAllDailyUserStats() {
        log.info("Fetching all daily user stats");

        return dailyStatsRepository.findAll().stream()
                .map(stat -> DailyUserStatsDto.builder()
                        .date(stat.getDate())
                        .email(stat.getEmail())
                        .commits(stat.getCommits())
                        .mergeCommits(stat.getMergeCommits() != null ? stat.getMergeCommits() : 0)
                        .addedLines(stat.getAddedLines())
                        .deletedLines(stat.getDeletedLines())
                        .testAddedLines(stat.getTestAddedLines() != null ? stat.getTestAddedLines() : 0)
                        .build())
                .sorted((a, b) -> {
                    int dateCompare = a.getDate().compareTo(b.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return a.getEmail().compareTo(b.getEmail());
                })
                .collect(Collectors.toList());
    }

    /**
     * Собрать детальную информацию о коммитах (с датами)
     */
    private List<CommitDetail> collectCommitDetails(LocalDateTime start, LocalDateTime end) {
        log.info("Collecting commit details between {} and {}", start, end);

        List<CompletableFuture<List<CommitDetail>>> futures = gitProperties.getRepositories()
                .stream()
                .map(repo -> CompletableFuture.supplyAsync(
                        () -> collectForRepositoryWithDetails(repo, start, end),
                        analysisExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Собрать коммиты из репозитория с датами
     */
    private List<CommitDetail> collectForRepositoryWithDetails(String repoUrl,
            LocalDateTime start,
            LocalDateTime end) {
        String repoName = extractRepoName(repoUrl);
        List<CommitDetail> repoCommits = new ArrayList<>();

        try {
            log.debug("Processing repository [{}] for period {} - {}", repoName, start, end);

            Path repoPath = gitClient.prepareRepository(repoUrl);
            List<String> lines = gitClient.collectStats(repoPath, start, end);

            if (lines.isEmpty()) {
                log.debug("No commits found for {} in this period", repoName);
                return repoCommits;
            }

            // Парсим все коммиты из git
            List<CommitDetail> allCommits = parseGitOutputWithDates(lines);

            for (CommitDetail commit : allCommits) {
                LocalDateTime commitDate = commit.getCommitDate();

                // Проверяем, что дата коммита ВНУТРИ нашего периода
                // и НЕ раньше 2026-01-01
                if (commitDate != null &&
                        !commitDate.isBefore(start) &&
                        !commitDate.isAfter(end)) {
                    repoCommits.add(commit);
                } else {
                    log.debug("Filtered out commit from {} (period: {} - {})",
                            commitDate, start, end);
                }
            }

            log.debug("Repository [{}] processed, kept {} of {} commits within period",
                    repoName, repoCommits.size(), allCommits.size());

        } catch (Exception e) {
            log.error("Failed to process repository {} for period {} - {}", repoName, start, end, e);
        }

        return repoCommits;
    }

    /**
     * Парсинг вывода git log с датами
     * Формат: email|parent|date
     */
    private List<CommitDetail> parseGitOutputWithDates(List<String> lines) {
        List<CommitDetail> commits = new ArrayList<>();
        CommitDetail currentCommit = null;

        List<String> userLines = lines.stream()
                .filter(l -> l != null && l.contains("vikto.zhigunov@x5.ru"))
                .collect(Collectors.toList());

        if (!userLines.isEmpty()) {
            log.info("=== DAILY STATS RAW LINES for vikto.zhigunov@x5.ru ===");
            userLines.forEach(line -> log.info("RAW: {}", line));
            log.info("=== END ===");
        }

        for (String rawLine : lines) {
            if (rawLine == null || rawLine.trim().isEmpty()) continue;

            String line = rawLine.trim();

            // Строка с информацией о коммите (email|parent|date)
            if (!line.contains("\t") && line.contains("@")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    String email = parts[0].trim().toLowerCase();
                    String parent = parts[1].trim();
                    String dateStr = parts[2].trim();

                    boolean isMerge = parent.contains(" ");

                    try {
                        // Парсим дату в формате ISO_OFFSET_DATE_TIME
                        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        // Сохраняем в UTC
                        LocalDateTime commitDateUTC = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

                        currentCommit = new CommitDetail();
                        currentCommit.setEmail(email);
                        currentCommit.setCommitDate(commitDateUTC);
                        currentCommit.setMerge(isMerge);
                        currentCommit.setAdded(0);
                        currentCommit.setDeleted(0);
                        currentCommit.setTestAdded(0);

                        commits.add(currentCommit);

                        log.debug("Parsed commit: {} at {} by {}",
                                isMerge ? "merge" : "regular",
                                commitDateUTC,
                                email);

                    } catch (DateTimeParseException e) {
                        log.warn("Failed to parse date: '{}' for email: {}", dateStr, email);
                        currentCommit = null;
                    }
                }
                continue;
            }

            // Строка с numstat (added\tdeleted\tfilename)
            if (currentCommit != null && line.contains("\t")) {
                String[] parts = line.split("\t");
                if (parts.length >= 3) {
                    if (parts[0].equals("-") || parts[1].equals("-")) continue;

                    try {
                        long added = Long.parseLong(parts[0]);
                        long deleted = Long.parseLong(parts[1]);
                        String fileName = parts[2];
                        boolean isTest = isTestFile(fileName);

                        currentCommit.setAdded(currentCommit.getAdded() + added);
                        currentCommit.setDeleted(currentCommit.getDeleted() + deleted);
                        if (isTest) {
                            currentCommit.setTestAdded(currentCommit.getTestAdded() + added);
                        }
                    } catch (NumberFormatException e) {
                        log.debug("Failed to parse numstat: {}", line);
                    }
                }
            }
        }

        log.info("Parsed {} commits from git output", commits.size());
        return commits;
    }

    /**
     * Группировка коммитов по дням и пользователям
     */
    private Map<LocalDate, Map<String, AuthorAggregate>> groupCommitsByDay(List<CommitDetail> commits) {
        Map<LocalDate, Map<String, AuthorAggregate>> dailyStats = new HashMap<>();

        LocalDate minAllowedDate = LocalDate.of(2026, 1, 1);
        int skippedCommits = 0;

        for (CommitDetail commit : commits) {
            LocalDate day = commit.getCommitDate().toLocalDate();

            if (day.isBefore(minAllowedDate)) {
                log.warn("⚠️ Forbidden old commit detected: {} from {}",
                        commit.getEmail(), day);
                skippedCommits++;
                continue;
            }

            dailyStats.computeIfAbsent(day, k -> new HashMap<>());
            Map<String, AuthorAggregate> dayStats = dailyStats.get(day);

            dayStats.compute(commit.getEmail(), (email, aggregate) -> {
                if (aggregate == null) {
                    aggregate = new AuthorAggregate(email);
                }
                aggregate = aggregate.addCommit(commit.isMerge());
                aggregate = aggregate.addLines(
                        commit.getAdded(),
                        commit.getDeleted(),
                        commit.getTestAdded() > 0
                );
                return aggregate;
            });
        }

        if (skippedCommits > 0) {
            log.error("🔥 CRITICAL: Skipped {} commits from before 2026-01-01", skippedCommits);
        }

        return dailyStats;
    }

    /**
     * Сохранить статистику по дням (с обновлением существующих записей, пакетная обработка)
     */
    private void saveDailyStats(Map<LocalDate, Map<String, AuthorAggregate>> dailyStats) {
        List<DailyAuthorStats> newRecords = new ArrayList<>();
        List<DailyAuthorStats> updateRecords = new ArrayList<>();

        for (Map.Entry<LocalDate, Map<String, AuthorAggregate>> dayEntry : dailyStats.entrySet()) {
            LocalDate date = dayEntry.getKey();
            Map<String, AuthorAggregate> dayStats = dayEntry.getValue();

            // Собираем все email за этот день для пакетной проверки
            List<String> emails = new ArrayList<>(dayStats.keySet());

            // Пакетная проверка существующих записей за этот день
            List<DailyAuthorStats> existingRecords = dailyStatsRepository.findByEmailInAndDate(emails, date);

            // Создаем Map для быстрого доступа к существующим записям
            Map<String, DailyAuthorStats> existingMap = existingRecords.stream()
                    .collect(Collectors.toMap(DailyAuthorStats::getEmail, Function.identity()));

            // Разделяем на новые и обновляемые
            for (Map.Entry<String, AuthorAggregate> statEntry : dayStats.entrySet()) {
                String email = statEntry.getKey();
                AuthorAggregate stat = statEntry.getValue();

                DailyAuthorStats existing = existingMap.get(email);

                if (existing != null) {
                    // Обновляем существующую запись
                    existing.setMergeCommits(stat.mergeCommits());
                    existing.setCommits(stat.commits());
                    existing.setAddedLines(stat.added());
                    existing.setDeletedLines(stat.deleted());
                    existing.setTestAddedLines(stat.testAdded());
                    existing.setLastUpdated(LocalDateTime.now());
                    updateRecords.add(existing);
                } else {
                    // Создаем новую запись
                    DailyAuthorStats newStats = DailyAuthorStats.builder()
                            .email(email)
                            .date(date)
                            .mergeCommits(stat.mergeCommits())
                            .commits(stat.commits())
                            .addedLines(stat.added())
                            .deletedLines(stat.deleted())
                            .testAddedLines(stat.testAdded())
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    newRecords.add(newStats);
                }
            }
        }

        // Пакетное сохранение
        if (CollectionUtils.isNotEmpty(newRecords)) {
            dailyStatsRepository.saveAll(newRecords);
            log.info("Saved {} new daily stats records", newRecords.size());
        }

        if (CollectionUtils.isNotEmpty(updateRecords)) {
            dailyStatsRepository.saveAll(updateRecords);
            log.info("Updated {} existing daily stats records", updateRecords.size());
        }

        log.info("Total processed: {} new, {} updated", newRecords.size(), updateRecords.size());
    }

    private boolean isTestFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.contains("/test/") ||
                lower.endsWith("test.java") ||
                lower.endsWith("tests.java");
    }

    private String extractRepoName(String repoUrl) {
        return repoUrl.substring(repoUrl.lastIndexOf("/") + 1).replace(".git", "");
    }
}
