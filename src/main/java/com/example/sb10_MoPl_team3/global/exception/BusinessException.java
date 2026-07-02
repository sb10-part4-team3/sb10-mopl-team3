package com.example.sb10_MoPl_team3.global.exception;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Map.of(), null);
    }

    public BusinessException(ErrorCode errorCode, Map<String, Object> details) {
        this(errorCode, details, null);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, Map.of(), cause);
    }

    public BusinessException(
            ErrorCode errorCode,
            Map<String, Object> details,
            Throwable cause
    ) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getMessage(), cause);
        this.errorCode = errorCode;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

}
