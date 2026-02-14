package com.back.catchmate.authorization.aspect;

import com.back.catchmate.authorization.annotation.PermissionId;
import com.back.catchmate.domain.common.permission.CheckDataPermission;
import com.back.catchmate.domain.common.permission.DomainFinder;
import com.back.catchmate.domain.common.permission.ResourceOwnership;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataPermissionAspect {
    private final ApplicationContext applicationContext;

    // 커스텀 어노테이션들을 모두 감지
    @Before("@annotation(com.back.catchmate.authorization.annotation.CheckBoardPermission) || " +
            "@annotation(com.back.catchmate.authorization.annotation.CheckEnrollApplicantPermission) || " +
            "@annotation(com.back.catchmate.authorization.annotation.CheckEnrollHostPermission) || " +
            "@annotation(com.back.catchmate.authorization.annotation.CheckNotificationPermission) || " +
            "@annotation(com.back.catchmate.authorization.annotation.CheckInquiryPermission)")

    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 현재 로그인한 유저 ID 가져오기
        Long currentUserId = getCurrentUserId();

        // 2. 어노테이션에서 Finder 클래스 정보 추출
        CheckDataPermission metaAnnotation = getMetaAnnotation(method);
        Class<? extends DomainFinder<?>> finderClass = metaAnnotation.finder();

        // 3. 파라미터에서 @PermissionId가 붙은 리소스 ID 값 추출
        Long resourceId = getResourceId(joinPoint, method);

        // 4. Spring Context에서 Finder Bean 찾아서 실행
        @SuppressWarnings("unchecked")
        DomainFinder<ResourceOwnership> finder = (DomainFinder<ResourceOwnership>) applicationContext.getBean(finderClass);
        ResourceOwnership resource = finder.searchById(resourceId);

        // 5. 소유권 검증
        if (!resource.getOwnershipId().equals(currentUserId)) {
            log.warn("권한 없음 - User: {}, ResourceOwner: {}", currentUserId, resource.getOwnershipId());
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    // 메서드에 붙은 어노테이션(예: @CheckBoardPermission)에서 메타 어노테이션(@CheckDataPermission) 추출
    private CheckDataPermission getMetaAnnotation(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(CheckDataPermission.class)) {
                return annotation.annotationType().getAnnotation(CheckDataPermission.class);
            }
        }
        throw new IllegalStateException("권한 검증 어노테이션 설정 오류");
    }

    /*
     * 파라미터에서 @PermissionId가 붙은 Long 타입 리소스 ID 추출
     */
    private Long getResourceId(JoinPoint joinPoint, Method method) {
        Object[] args = joinPoint.getArgs();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof PermissionId) {
                    if (args[i] instanceof Long) {
                        return (Long) args[i];
                    } else {
                        throw new IllegalStateException("@PermissionId는 Long 타입 파라미터에만 사용할 수 있습니다.");
                    }
                }
            }
        }
        throw new IllegalStateException("@PermissionId가 붙은 파라미터를 찾을 수 없습니다.");
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BaseException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        return Long.parseLong(authentication.getPrincipal().toString());
    }
}
