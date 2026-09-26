package com.back.catchmate.auth.infrastructure;

import com.back.catchmate.auth.domain.AuthTokenProvider;
import com.back.catchmate.auth.domain.OAuthProfile;
import com.back.catchmate.auth.domain.Provider;
import com.back.catchmate.auth.domain.exception.InvalidSignupTokenException;
import com.back.catchmate.auth.domain.exception.InvalidTokenException;
import com.back.catchmate.global.config.security.AccessTokenVerifier;
import com.back.catchmate.global.config.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider implements AuthTokenProvider, AccessTokenVerifier {
    private static final String BEARER = "Bearer ";
    private static final String SIGNUP_TOKEN_SUBJECT = "SignupToken";
    private static final String ID_CLAIM = "id";
    private static final String ROLE_CLAIM = "role";
    private static final String PROVIDER_CLAIM = "provider";
    private static final String PROVIDER_ID_CLAIM = "providerId";
    private static final String EMAIL_CLAIM = "email";
    private static final String PROFILE_IMAGE_URL_CLAIM = "profileImageUrl";

    private final String secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;
    private final long signupTokenExpirationMillis;
    private final String accessTokenSubject;
    private final String refreshTokenSubject;

    public JwtTokenProvider(
            @Value("${jwt.secretKey}") String secretKey,
            @Value("${jwt.access.expiration}") long accessTokenExpirationMillis,
            @Value("${jwt.refresh.expiration}") long refreshTokenExpirationMillis,
            @Value("${jwt.signup.expiration:600000}") long signupTokenExpirationMillis,
            @Value("${jwt.access.header}") String accessTokenSubject,
            @Value("${jwt.refresh.header}") String refreshTokenSubject) {
        this.secretKey = secretKey;
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
        this.signupTokenExpirationMillis = signupTokenExpirationMillis;
        this.accessTokenSubject = accessTokenSubject;
        this.refreshTokenSubject = refreshTokenSubject;
    }

    @Override
    public String createAccessToken(Long userId, String role) {
        return BEARER + createToken(userId, role, accessTokenSubject, accessTokenExpirationMillis);
    }

    @Override
    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, refreshTokenSubject, refreshTokenExpirationMillis);
    }

    @Override
    public Long getUserId(String token) {
        return parseAuthClaims(token).get(ID_CLAIM, Long.class);
    }

    @Override
    public AuthenticatedUser verify(String token) {
        Claims claims = parseAuthClaims(token);
        return new AuthenticatedUser(claims.get(ID_CLAIM, Long.class), claims.get(ROLE_CLAIM, String.class));
    }

    @Override
    public long refreshTokenTtlMillis() {
        return refreshTokenExpirationMillis;
    }

    @Override
    public String createSignupToken(OAuthProfile profile) {
        Date now = new Date();
        Claims claims = Jwts.claims();
        claims.put(PROVIDER_CLAIM, profile.provider().value());
        claims.put(PROVIDER_ID_CLAIM, profile.providerId());
        claims.put(EMAIL_CLAIM, profile.email());
        claims.put(PROFILE_IMAGE_URL_CLAIM, profile.profileImageUrl());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(SIGNUP_TOKEN_SUBJECT)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + signupTokenExpirationMillis))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public OAuthProfile parseSignupToken(String signupToken) {
        Claims claims;
        try {
            claims = parseClaims(signupToken);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("signup 토큰 파싱 실패: {}", e.getMessage());
            throw new InvalidSignupTokenException();
        }
        // access·refresh 토큰도 같은 키로 서명되므로 subject 로 signup 토큰만 받는다.
        if (!SIGNUP_TOKEN_SUBJECT.equals(claims.getSubject())) {
            throw new InvalidSignupTokenException();
        }
        return new OAuthProfile(
                Provider.of(claims.get(PROVIDER_CLAIM, String.class)),
                claims.get(PROVIDER_ID_CLAIM, String.class),
                claims.get(EMAIL_CLAIM, String.class),
                claims.get(PROFILE_IMAGE_URL_CLAIM, String.class));
    }

    private String createToken(Long userId, String role, String subject, long expirationMillis) {
        Date now = new Date();
        Claims claims = Jwts.claims();
        claims.put(ID_CLAIM, userId);
        claims.put(ROLE_CLAIM, role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMillis))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseAuthClaims(String token) {
        try {
            return parseClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 파싱 실패: {}", e.getMessage());
            throw new InvalidTokenException();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(removeBearer(token))
                .getBody();
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private String removeBearer(String token) {
        if (token != null && token.startsWith(BEARER)) {
            return token.substring(BEARER.length());
        }
        return token;
    }
}
