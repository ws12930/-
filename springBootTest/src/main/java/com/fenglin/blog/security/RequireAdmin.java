package com.fenglin.blog.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口（或整个 Controller）只有管理员可以访问。
 *
 * <p>由 {@link AuthInterceptor} 检查：游客抛 401，普通用户抛 403。
 * 可以标在类上（整类生效），也可以只标在某个方法上。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}
