package com.back.catchmate.global.resolver;

import com.back.catchmate.domain.auth.service.TokenProvider;
import com.back.catchmate.global.annotation.AuthUser;
import error.ErrorCode;
import error.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final TokenProvider tokenProvider;
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 파라미터에 @AuthUser 어노테이션이 붙어있고, 타입이 Long인지 확인
        return parameter.hasParameterAnnotation(AuthUser.class) 
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String token = resolveToken(request);

        if (token == null) {
            // 토큰이 없는데 @AuthUser를 쓴 경우 에러 처리 (필요에 따라 null 반환 허용 가능)
            throw new BaseException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        // 토큰 파싱하여 userId 반환 (TokenProvider 구현에 따라 예외가 발생할 수 있음)
        return tokenProvider.parseUserId(token);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
