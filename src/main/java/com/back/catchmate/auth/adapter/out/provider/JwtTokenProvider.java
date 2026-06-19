package com.back.catchmate.auth.adapter.out.provider;

import com.back.catchmate.auth.application.port.out.external.TokenProvider;
import com.back.catchmate.auth.application.dto.SignupTokenPayload;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
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
    @Value("${jwt.signup.expiration:600000}")
    private Long signupTokenExpirationPeriod;
    @Value("${jwt.access.header}")
    private String ACCESS_TOKEN_SUBJECT;
    @Value("${jwt.refresh.header}")
    private String REFRESH_TOKEN_SUBJECT;

    private static final String SIGNUP_TOKEN_SUBJECT = "SignupToken";
    private static final String ID_CLAIM = "id";
    private static final String BEARER = "Bearer ";
    private static final String ROLE_CLAIM = "role";
    private static final String PROVIDER_CLAIM = "provider";
    private static final String PROVIDER_ID_CLAIM = "providerId";
    private static final String EMAIL_CLAIM = "email";
    private static final String PROFILE_IMAGE_URL_CLAIM = "profileImageUrl";

    @Override
    public String createAccessToken(Long userId, String role) {
        return BEARER + createToken(userId, ACCESS_TOKEN_SUBJECT, accessTokenExpirationPeriod, role);
    }

    @Override
    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, REFRESH_TOKEN_SUBJECT, refreshTokenExpirationPeriod, role);
    }

    @Override
    public Long getUserId(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(ID_CLAIM, Long.class);
        } catch (Exception e) {
            log.error("JWT Parsing Error: {}", e.getMessage());
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public String getUserRole(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get(ROLE_CLAIM, String.class);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public Long getRefreshTokenExpirationTime() {
        return refreshTokenExpirationPeriod;
    }

    @Override
    public String createSignupToken(SignupTokenPayload payload) {
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + signupTokenExpirationPeriod);

        Claims claims = Jwts.claims();
        claims.put(PROVIDER_CLAIM, payload.provider());
        claims.put(PROVIDER_ID_CLAIM, payload.providerId());
        claims.put(EMAIL_CLAIM, payload.email());
        claims.put(PROFILE_IMAGE_URL_CLAIM, payload.profileImageUrl());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(SIGNUP_TOKEN_SUBJECT)
                .setIssuedAt(now)
                .setExpiration(expirationTime)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public SignupTokenPayload parseSignupToken(String signupToken) {
        try {
            Claims claims = parseClaims(signupToken);
            if (!SIGNUP_TOKEN_SUBJECT.equals(claims.getSubject())) {
                throw new BaseException(ErrorCode.INVALID_SIGNUP_TOKEN);
            }
            return new SignupTokenPayload(
                    claims.get(PROVIDER_CLAIM, String.class),
                    claims.get(PROVIDER_ID_CLAIM, String.class),
                    claims.get(EMAIL_CLAIM, String.class),
                    claims.get(PROFILE_IMAGE_URL_CLAIM, String.class)
            );
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Signup Token Parsing Error: {}", e.getMessage());
            throw new BaseException(ErrorCode.INVALID_SIGNUP_TOKEN);
        }
    }

    private String createToken(Long userId, String tokenSubject, Long expirationPeriod, String authority) {
        Date now = new Date();
        Date expirationTime = new Date(now.getTime() + expirationPeriod);

        Claims claims = Jwts.claims();
        claims.put(ID_CLAIM, userId);
        claims.put(ROLE_CLAIM, authority);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(tokenSubject)
                .setIssuedAt(now)
                .setExpiration(expirationTime)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(removeBearer(token))
                .getBody();
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
}
