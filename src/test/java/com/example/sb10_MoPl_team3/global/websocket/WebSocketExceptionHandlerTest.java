package com.example.sb10_MoPl_team3.global.websocket;

import com.example.sb10_MoPl_team3.global.enums.ErrorCode;
import com.example.sb10_MoPl_team3.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.annotation.SendToUser;

import java.lang.reflect.Method;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketExceptionHandlerTest {

    private final WebSocketExceptionHandler handler = new WebSocketExceptionHandler();

    @Test
    @DisplayName("비동기 CompletionException의 BusinessException 원인을 풀어 오류 응답으로 변환한다")
    void handle_unwrapsCompletionException() {
        var response = handler.handle(new CompletionException(
                new BusinessException(ErrorCode.CONVERSATION_NOT_FOUND)));

        assertThat(response.code()).isEqualTo("CONVERSATION_NOT_FOUND");
        assertThat(response.status()).isEqualTo(404);
    }

    @Test
    @DisplayName("중첩된 비동기 예외도 실제 BusinessException까지 해제한다")
    void handle_unwrapsNestedAsyncException() {
        var response = handler.handle(new CompletionException(
                new ExecutionException(new BusinessException(ErrorCode.ACCESS_DENIED))));

        assertThat(response.code()).isEqualTo("ACCESS_DENIED");
        assertThat(response.status()).isEqualTo(403);
    }

    @Test
    @DisplayName("알 수 없는 WebSocket 예외는 서버 오류 응답으로 변환한다")
    void handle_unknownException() {
        var response = handler.handle(new IllegalStateException("unknown"));

        assertThat(response.code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.status()).isEqualTo(500);
    }

    @Test
    @DisplayName("Exception이 아닌 Throwable도 서버 오류 응답으로 안전하게 변환한다")
    void handle_nonExceptionThrowable() {
        var response = handler.handle(new AssertionError("fatal"));

        assertThat(response.code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.status()).isEqualTo(500);
    }

    @Test
    @DisplayName("WebSocket 오류는 현재 사용자 전용 오류 큐로 전송한다")
    void handle_sendsToUserErrorQueue() throws Exception {
        Method method = WebSocketExceptionHandler.class.getMethod("handle", Throwable.class);
        SendToUser annotation = method.getAnnotation(SendToUser.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.destinations()).containsExactly("/queue/errors");
        assertThat(annotation.broadcast()).isFalse();
    }
}
