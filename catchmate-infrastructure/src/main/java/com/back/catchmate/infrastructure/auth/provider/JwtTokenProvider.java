package com.back.catchmate.infrastructure.auth.provider;

import com.back.catchmate.domain.auth.port.TokenProvider;
import com.back.catchmate.domain.user.model.Authority;
import error.ErrorCode;
import error.exception.BaseException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {
    @Value("${jwt.secretKey}")
    private String secretKey;
    @Value("${jwt.access.expiration}")
    private Long accessTokenExpirationPeriod;
    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenExpirationPeriod;
    @Value("${jwt.access.header}")
    private String ACCESS_TOKEN_SUBJECT;
    @Value("${jwt.refresh.header}")
    private String REFRESH_TOKEN_SUBJECT;

    private static final String ID_CLAIM = "id";
    private static final String BEARER = "Bearer ";
    private static final String ROLE_CLAIM = "role";

    @Override
    public String createAccessToken(Long userId, Authority role) {
        return createToken(userId, ACCESS_TOKEN_SUBJECT, accessTokenExpirationPeriod, role);
    }

    @Override
    public String createRefreshToken(Long userId, Authority role) {
        return createToken(userId, REFRESH_TOKEN_SUBJECT, refreshTokenExpirationPeriod, role);
    }

    @Override
    public Long parseUserId(String token) {
        try {
            token = removeBearer(token);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey()) // Key 객체 사용
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get(ID_CLAIM, Long.class);
        } catch (Exception e) {
            log.error("JWT Parsing Error: {}", e.getMessage());
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public Long getRefreshTokenExpiration() {
        return refreshTokenExpirationPeriod;
    }

    // 내부 헬퍼 메서드
    private String createToken(Long userId, String tokenSubject, Long expirationPeriod, Authority authority) {
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + expirationPeriod);

        Claims claims = Jwts.claims();
        claims.put(ID_CLAIM, userId);
        claims.put(ROLE_CLAIM, authority);

        return BEARER + Jwts.builder()
                .setSubject(tokenSubject)
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expirationTime)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String removeBearer(String token) {
        if (token != null && token.startsWith(BEARER)) {
            return token.substring(BEARER.length());
        }
        return token;
    }

    @Override
    public String getRole(String token) {
        try {
            token = removeBearer(token);
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get(ROLE_CLAIM, String.class);
        } catch (Exception e) {
            return Authority.ROLE_USER.name(); // 기본값 설정
        }
    }
}
