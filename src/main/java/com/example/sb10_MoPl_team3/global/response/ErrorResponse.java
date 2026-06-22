package com.example.sb10_MoPl_team3.global.response;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

public record ErrorResponse(
    Instant timestamp,
    String code,
    String message,
    Map<String, Object> details,
    int status
) {

    public static ErrorResponse of(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return new ErrorResponse(
            Instant.now(),
            errorCode.name(),
            errorCode.getMessage(),
            exception.getDetails(),
            errorCode.getStatus().value()
        );
    }

    public static ErrorResponse of(MethodArgumentNotValidException exception) {
        Map<String, Object> details = new HashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
            details.putIfAbsent(
                error.getField(),
                Objects.equals(error.getCode(), "typeMismatch")
                    ? error.getRejectedValue()
                    : error.getDefaultMessage()
            )
        );

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return new ErrorResponse(
            Instant.now(),
            errorCode.name(),
            errorCode.getMessage(),
            details,
            errorCode.getStatus().value()
        );
    }

    public static ErrorResponse of(
        MethodArgumentTypeMismatchException exception,
        Map<String, Object> details
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER_TYPE;
        return new ErrorResponse(
            Instant.now(),
            errorCode.name(),
            errorCode.getMessage(),
            details == null ? Map.of() : Map.copyOf(details),
            errorCode.getStatus().value()
        );
    }

    public static ErrorResponse of(Exception exception) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return new ErrorResponse(
            Instant.now(),
            errorCode.name(),
            errorCode.getMessage(),
            Map.of(),
            errorCode.getStatus().value()
        );
    }
}
