package ru.x5.markable.dev.analytics.gitlab.exception;

public class StatisticsPersistenceException extends AnalysisException {

    public StatisticsPersistenceException(Throwable cause) {
        super("Failed to persist statistics", cause);
    }
}
