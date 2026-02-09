package com.back.catchmate.authorization.annotation;

import com.back.catchmate.authorization.finder.EnrollHostFinder;
import com.back.catchmate.domain.common.permission.CheckDataPermission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
// Finder 클래스를 직접 명시!
@CheckDataPermission(finder = EnrollHostFinder.class)
public @interface CheckEnrollHostPermission {
}
