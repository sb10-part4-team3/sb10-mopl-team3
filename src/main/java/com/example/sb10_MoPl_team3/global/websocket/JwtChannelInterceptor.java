package com.example.sb10_MoPl_team3.global.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if (accessor != null && StompCommand.CONNECT == accessor.getCommand()) {
            extractAccessToken(accessor);
            // TODO: JWT 검증 컴포넌트가 확정되면 추출한 토큰 검증 및 인증 정보 바인딩을 추가한다.
        }

        return message;
    }

    private String extractAccessToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            throw new MessagingException("Authorization 헤더가 필요합니다.");
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("Authorization 헤더는 Bearer 토큰 형식이어야 합니다.");
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new MessagingException("JWT 액세스 토큰이 비어 있습니다.");
        }

        return token;
    }
}
