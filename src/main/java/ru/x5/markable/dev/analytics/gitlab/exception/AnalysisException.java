package ru.x5.markable.dev.analytics.gitlab.exception;

public abstract class AnalysisException extends RuntimeException {

    protected AnalysisException(String message) {
        super(message);
    }

    protected AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

}
