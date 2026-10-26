package com.fenglin.blog.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.entity.AccessToken;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.AccessTokenMapper;
import com.fenglin.blog.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * 登录态拦截器。
 *
 * <p>对所有 {@code /api/**} 请求都会尝试解析 {@code Authorization: Bearer <token>}：
 * <ul>
 *   <li>令牌有效 —— 写入 {@link CurrentUserHolder}，公开接口也能返回个性化字段；</li>
 *   <li>令牌无效或缺失 —— 按游客继续，不阻断请求；</li>
 *   <li>仅当目标方法标注了 {@link RequireAuth} 且当前是游客时，才抛 401。</li>
 * </ul>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final AccessTokenMapper accessTokenMapper;
    private final UserMapper userMapper;

    public AuthInterceptor(AccessTokenMapper accessTokenMapper, UserMapper userMapper) {
        this.accessTokenMapper = accessTokenMapper;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CurrentUserHolder.clear();

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            if (!token.isEmpty()) {
                CurrentUser user = resolve(token);
                if (user != null) {
                    CurrentUserHolder.set(user);
                }
            }
        }

        if (handler instanceof HandlerMethod handlerMethod
                && handlerMethod.hasMethodAnnotation(RequireAuth.class)
                && CurrentUserHolder.get() == null) {
            throw BizException.unauthorized("请先登录");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }

    /** 令牌 -> 用户；任何一步不合法都返回 null（按游客处理） */
    private CurrentUser resolve(String token) {
        AccessToken record = accessTokenMapper.selectOne(
                new LambdaQueryWrapper<AccessToken>()
                        .eq(AccessToken::getTokenHash, TokenHash.sha256(token)));
        if (record == null) {
            return null;
        }
        LocalDateTime expireAt = record.getExpireAt();
        if (expireAt == null || expireAt.isBefore(LocalDateTime.now())) {
            return null;
        }
        User user = userMapper.selectById(record.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return null;
        }
        return new CurrentUser(user.getId(), user.getUsername(), user.getRole());
    }
}
