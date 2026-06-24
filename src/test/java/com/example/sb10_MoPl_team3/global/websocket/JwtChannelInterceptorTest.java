package com.example.sb10_MoPl_team3.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.sb10_MoPl_team3.global.security.jwt.JwtProperties;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import com.example.sb10_MoPl_team3.user.enums.UserRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtChannelInterceptorTest {

    private static final String TEST_JWT_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtcHJvdmlkZXItdGVzdC1tdXN0LWJlLWxvbmc=";

    private final JwtProvider jwtProvider = new JwtProvider(
            new JwtProperties(TEST_JWT_SECRET, Duration.ofHours(1), "mopl-test"),
            Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC)
    );
    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtProvider);
    private final MessageChannel channel = mock(MessageChannel.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CONNECT 요청의 JWT가 유효하면 STOMP user에 인증 정보를 바인딩하고 전송 완료 후 SecurityContext를 비운다")
    void preSend_connectWithValidAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtProvider.generateAccessToken(userId, UserRole.USER, UUID.randomUUID());
        StompHeaderAccessor accessor = connectAccessor("Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, channel);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userId);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        assertThat(accessor.getUser()).isEqualTo(authentication);

        interceptor.afterSendCompletion(message, channel, true, null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(accessor.getUser()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("CONNECT 요청의 JWT가 유효하지 않으면 인증 정보를 비우고 연결을 거부한다")
    void preSend_connectWithInvalidAccessToken() {
        StompHeaderAccessor accessor = connectAccessor("Bearer invalid-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessagingException.class)
                .hasMessage("웹소켓 인증에 실패했습니다.");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    @DisplayName("STOMP 메시지가 아니면 인증 처리를 하지 않고 메시지를 그대로 반환한다")
    void preSend_withoutStompAccessor() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("CONNECT가 아닌 STOMP 요청이면 인증 처리를 하지 않고 메시지를 그대로 반환한다")
    void preSend_notConnectCommand() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 연결을 거부한다")
    void preSend_connectWithoutAuthorizationHeader() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertAuthenticationFailure(message, accessor);
    }

    @Test
    @DisplayName("Authorization 헤더가 공백이면 연결을 거부한다")
    void preSend_connectWithBlankAuthorizationHeader() {
        StompHeaderAccessor accessor = connectAccessor(" ");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertAuthenticationFailure(message, accessor);
    }

    @Test
    @DisplayName("Authorization 헤더가 Bearer 형식이 아니면 연결을 거부한다")
    void preSend_connectWithInvalidAuthorizationScheme() {
        StompHeaderAccessor accessor = connectAccessor("Basic token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertAuthenticationFailure(message, accessor);
    }

    @Test
    @DisplayName("Bearer 뒤 토큰이 비어 있으면 연결을 거부한다")
    void preSend_connectWithEmptyBearerToken() {
        StompHeaderAccessor accessor = connectAccessor("Bearer ");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertAuthenticationFailure(message, accessor);
    }

    @Test
    @DisplayName("전송 완료 메시지가 STOMP 메시지가 아니면 SecurityContext를 유지한다")
    void afterSendCompletion_withoutStompAccessor() {
        Authentication authentication = existingAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        interceptor.afterSendCompletion(message, channel, true, null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("전송 완료 메시지가 CONNECT가 아니면 SecurityContext를 유지한다")
    void afterSendCompletion_notConnectCommand() {
        Authentication authentication = existingAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.afterSendCompletion(message, channel, true, null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    private StompHeaderAccessor connectAccessor(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", authorization);
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private void assertAuthenticationFailure(Message<byte[]> message, StompHeaderAccessor accessor) {
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessagingException.class)
                .hasMessage("웹소켓 인증에 실패했습니다.");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(accessor.getUser()).isNull();
    }

    private Authentication existingAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
