package com.example.sb10_MoPl_team3.global.websocket;

import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@ControllerAdvice
@Slf4j
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(Throwable.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handle(Throwable exception) {
        Throwable cause = unwrap(exception);
        ErrorResponse response;
        if (cause instanceof BusinessException businessException) {
            response = ErrorResponse.of(businessException);
        } else if (cause instanceof MethodArgumentNotValidException validationException) {
            response = ErrorResponse.of(validationException);
        } else {
            response = ErrorResponse.of(asException(cause));
        }
        log(response, cause);
        return response;
    }

    private Throwable unwrap(Throwable exception) {
        Throwable current = exception;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private Exception asException(Throwable throwable) {
        return throwable instanceof Exception exception
                ? exception
                : new RuntimeException(throwable);
    }

    private void log(ErrorResponse response, Throwable cause) {
        if (response.status() >= 500) {
            log.error(
                    "WebSocket 처리 실패: code={}, message={}, details={}",
                    response.code(), response.message(), response.details(), cause
            );
        } else {
            log.warn(
                    "WebSocket 요청 거부: code={}, message={}, details={}",
                    response.code(), response.message(), response.details()
            );
        }
    }
}
