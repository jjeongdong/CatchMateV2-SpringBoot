package com.back.catchmate.authorization.annotation;

import com.back.catchmate.authorization.finder.NotificationPermissionFinder;
import com.back.catchmate.domain.common.permission.CheckDataPermission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@CheckDataPermission(finder = NotificationPermissionFinder.class)
public @interface CheckNotificationPermission {
}
