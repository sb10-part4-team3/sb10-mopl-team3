package com.example.sb10_MoPl_team3.global.websocket;

import com.example.sb10_MoPl_team3.global.security.jwt.JwtClaims;
import com.example.sb10_MoPl_team3.global.security.jwt.JwtProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if (accessor != null && StompCommand.CONNECT == accessor.getCommand()) {
            authenticate(accessor);
        }

        return message;
    }

    @Override
    public void afterSendCompletion(
            Message<?> message,
            MessageChannel channel,
            boolean sent,
            Exception ex
    ) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );

        if (accessor != null && StompCommand.CONNECT == accessor.getCommand()) {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(StompHeaderAccessor accessor) {
        try {
            JwtClaims claims = jwtProvider.parseAccessToken(extractAccessToken(accessor));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            claims.userId(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);
            log.debug("웹소켓 인증 성공: role={}", claims.role());
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            log.warn("웹소켓 인증 실패: reason={}", e.getClass().getSimpleName());
            throw new MessagingException("웹소켓 인증에 실패했습니다.", e);
        }
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
