package com.back.catchmate.global.authorization.annotation;

import com.back.catchmate.global.authorization.finder.EnrollHostFinder;
import com.back.catchmate.global.authorization.finder.InquiryPermissionFinder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@CheckDataPermission(finder = InquiryPermissionFinder.class)
public @interface CheckInquiryPermission {
}
