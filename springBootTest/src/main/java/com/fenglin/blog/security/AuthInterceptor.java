package com.fenglin.blog.security;

import com.fenglin.blog.common.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequireAuth requireAuth = AnnotationUtils.findAnnotation(method.getMethod(), RequireAuth.class);
        if (requireAuth == null) {
            requireAuth = AnnotationUtils.findAnnotation(method.getBeanType(), RequireAuth.class);
        }
        String authorization = request.getHeader("Authorization");

        if (requireAuth != null) {
            CurrentUserHolder.CurrentUser currentUser = tokenService.authenticate(authorization);
            if (requireAuth.adminOnly() && !currentUser.isAdmin()) {
                throw BizException.forbidden("该操作仅管理员可执行");
            }
            CurrentUserHolder.set(currentUser);
            return true;
        }

        // 非强制登录的接口：若请求携带了有效令牌，也识别出来，用于 liked / followed / self 等个性化字段
        if (authorization != null && !authorization.isBlank()) {
            try {
                CurrentUserHolder.set(tokenService.authenticate(authorization));
            } catch (BizException ignored) {
                // 令牌无效或过期时按游客处理，不阻断请求
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }
}
