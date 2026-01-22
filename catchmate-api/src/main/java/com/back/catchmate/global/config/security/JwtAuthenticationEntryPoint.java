package com.back.catchmate.global.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import error.ErrorCode;
import error.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        // 유효하지 않은 토큰이나 인증 실패 시 401 응답 생성
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ErrorCode.INVALID_TOKEN.getHttpStatus().value())
                .error(ErrorCode.INVALID_TOKEN.getHttpStatus().name())
                .code(ErrorCode.INVALID_TOKEN.name())
                .message("인증에 실패하였습니다. (유효하지 않은 토큰)")
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
