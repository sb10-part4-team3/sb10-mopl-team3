package com.example.sb10_MoPl_team3.global.exception;

import com.example.sb10_MoPl_team3.global.response.ErrorResponse;
import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
        ErrorResponse response = ErrorResponse.of(exception);
        if (exception.getErrorCode().getStatus().is5xxServerError()) {
            logError(response, exception);
        } else {
            logWarn(response);
        }
        return ResponseEntity.status(response.status()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception
    ) {
        ErrorResponse response = ErrorResponse.of(exception);
        logWarn(response);
        return ResponseEntity.status(response.status()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException exception
    ) {
        Map<String, Object> details = new HashMap<>();
        details.put("parameter", exception.getName());
        details.put("value", exception.getValue());
        details.put(
            "requiredType",
            exception.getRequiredType() != null
                ? exception.getRequiredType().getSimpleName()
                : "unknown"
        );

        ErrorResponse response = ErrorResponse.of(exception, details);
        logWarn(response);
        return ResponseEntity.status(response.status()).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
        MissingServletRequestParameterException exception
    ) {
        Map<String, Object> details = new HashMap<>();
        details.put("parameter", exception.getParameterName());
        details.put("requiredType", exception.getParameterType());

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        ErrorResponse response = new ErrorResponse(
            Instant.now(),
            errorCode.name(),
            errorCode.getMessage(),
            details,
            errorCode.getStatus().value()
        );
        logWarn(response);
        return ResponseEntity.status(response.status()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        ErrorResponse response = ErrorResponse.of(exception);
        logError(response, exception);
        return ResponseEntity.status(response.status()).body(response);
    }

    private void logWarn(ErrorResponse response) {
        log.warn("[{}] {} details={}", response.code(), response.message(), response.details());
    }

    private void logError(ErrorResponse response, Exception exception) {
        log.error("[{}] {} details={}", response.code(), response.message(), response.details(), exception);
    }
}
