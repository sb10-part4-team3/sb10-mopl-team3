package com.example.sb10_MoPl_team3.global.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.response.ErrorResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(
            AuthorizationDeniedException exception
    ) {
        ErrorResponse response = ErrorResponse.of(new BusinessException(ErrorCode.ACCESS_DENIED));
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
