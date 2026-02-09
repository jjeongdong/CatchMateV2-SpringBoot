package com.back.catchmate.authorization.annotation;

import com.back.catchmate.authorization.finder.BoardPermissionFinder;
import com.back.catchmate.domain.common.permission.CheckDataPermission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@CheckDataPermission(finder = BoardPermissionFinder.class)
public @interface CheckBoardPermission {
}
