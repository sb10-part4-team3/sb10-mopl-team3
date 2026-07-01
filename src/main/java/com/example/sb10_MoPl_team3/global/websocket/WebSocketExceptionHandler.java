package com.example.sb10_MoPl_team3.global.websocket;

import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import com.example.sb10_MoPl_team3.global.response.ErrorResponse;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(Throwable.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ErrorResponse handle(Throwable exception) {
        Throwable cause = unwrap(exception);
        if (cause instanceof BusinessException businessException) {
            return ErrorResponse.of(businessException);
        }
        if (cause instanceof MethodArgumentNotValidException validationException) {
            return ErrorResponse.of(validationException);
        }
        return ErrorResponse.of(asException(cause));
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
}
