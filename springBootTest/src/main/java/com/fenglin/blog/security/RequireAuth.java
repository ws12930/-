package com.fenglin.blog.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口必须登录才能访问。
 *
 * <p>由 {@link AuthInterceptor} 检查：令牌缺失或无效时抛 401。
 * 未标注该注解的接口为公开接口，但仍会尝试解析 Authorization 头，
 * 以便返回 liked / followed / self 等个性化字段。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
}
