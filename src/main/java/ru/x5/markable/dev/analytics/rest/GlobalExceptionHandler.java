package ru.x5.markable.dev.analytics.rest;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.x5.markable.dev.analytics.rest.dto.CustomErrorResponse;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

//    @ExceptionHandler(InvalidPasswordException.class)
//    public ResponseEntity<CustomErrorResponse> catchInvalidPasswordException(InvalidPasswordException ex) {
//        log.error(ex.getMessage(), ex);
//        return new ResponseEntity<>(new CustomErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage()),
//                HttpStatus.BAD_REQUEST);
//    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomErrorResponse> handleGeneralException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(
                new CustomErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Произошла ошибка на сервере."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
