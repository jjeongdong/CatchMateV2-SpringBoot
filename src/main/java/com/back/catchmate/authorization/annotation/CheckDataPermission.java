package com.back.catchmate.authorization.annotation;

import com.back.catchmate.authorization.common.DomainFinder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE) // 다른 어노테이션 위에 붙이는 메타 어노테이션
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckDataPermission {
    Class<? extends DomainFinder<?>> finder();
}
