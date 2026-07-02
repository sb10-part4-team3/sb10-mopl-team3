package com.example.sb10_MoPl_team3.global.security.jwt;

import com.example.sb10_MoPl_team3.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_SESSION_ID = "sid";

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String generateAccessToken(UUID userId, UserRole role, UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenExpiration());

        var builder = Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(userId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, JwtTokenType.ACCESS.name())
                .claim(CLAIM_SESSION_ID, sessionId.toString())
                .signWith(secretKey);

        return builder.compact();
    }

    public JwtClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.issuer())
                .clock(() -> Date.from(Instant.now(clock)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        JwtTokenType type = JwtTokenType.valueOf(claims.get(CLAIM_TYPE, String.class));
        if (type != JwtTokenType.ACCESS) {
            throw new IllegalArgumentException("Token type must be ACCESS");
        }

        String sessionId = Objects.requireNonNull(
                claims.get(CLAIM_SESSION_ID, String.class),
                "sessionId must not be null"
        );

        return new JwtClaims(
                UUID.fromString(claims.getSubject()),
                UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)),
                type,
                UUID.fromString(sessionId),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }
}
