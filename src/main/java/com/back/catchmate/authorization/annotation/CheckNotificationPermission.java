package com.back.catchmate.authorization.annotation;

import com.back.catchmate.authorization.finder.EnrollHostFinder;
import com.back.catchmate.authorization.finder.NotificationPermissionFinder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@CheckDataPermission(finder = NotificationPermissionFinder.class)
public @interface CheckNotificationPermission {
}
