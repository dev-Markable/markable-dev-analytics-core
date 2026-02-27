package ru.x5.markable.dev.analytics.commons.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Исключение API, выбрасывается из интерактора.
 * Не выбрасывать из сервиса!
 */
@Getter
public abstract class ApiException extends RuntimeException {

    protected ApiException(String message) {
        super(message);
    }

    protected ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract HttpStatus getHttpStatus();
}
