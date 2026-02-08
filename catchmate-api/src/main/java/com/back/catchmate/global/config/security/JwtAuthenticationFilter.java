package com.back.catchmate.global.config.security;

import com.back.catchmate.orchestration.auth.AuthOrchestrator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AuthOrchestrator authOrchestrator;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 1. 토큰 파싱 및 검증 (실패 시 예외 발생)
                Long userId = authOrchestrator.getUserId(token);
                String role = authOrchestrator.getUserRole(token);

                // 2. 인증 객체 생성 (권한이 있다면 Authorities에 추가)
                // 여기서는 간단히 ROLE_USER로 고정하거나, DB에서 조회하여 설정 가능
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userId, // Principal (User ID)
                        null,   // Credentials (보통 null)
                        Collections.singletonList(new SimpleGrantedAuthority(role)) // Authorities
                );

                // 3. SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
            } catch (Exception e) {
                // 토큰이 유효하지 않은 경우, SecurityContext를 비우고 로그만 남김 (이후 EntryPoint에서 처리)
                log.warn("Invalid JWT Token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
