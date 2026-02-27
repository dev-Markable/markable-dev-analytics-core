package ru.x5.markable.dev.analytics.commons.exceptions;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends ApiException {

    public UnprocessableEntityException(MessageTemplate messageTemplate, Object... args) {
        super(messageTemplate.getText(args));
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
