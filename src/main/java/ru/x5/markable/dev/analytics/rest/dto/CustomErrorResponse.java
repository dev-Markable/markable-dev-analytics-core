package ru.x5.markable.dev.analytics.rest.dto;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Класс, представляющий ответ об ошибке в формате JSON. Используется для структурированной обработки ошибок в
 * приложении.
 */
@Data
public class CustomErrorResponse {

    private final HttpStatusCode status;

    private final String message;

    /**
     * Конструктор для создания объекта CustomErrorResponse.
     *
     * @param status Код состояния HTTP, связанный с ошибкой.
     * @param message Сообщение об ошибке, которое будет возвращено клиенту.
     */
    public CustomErrorResponse(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
