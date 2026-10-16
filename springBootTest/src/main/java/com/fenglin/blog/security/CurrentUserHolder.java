package com.fenglin.blog.security;

import com.fenglin.blog.common.BizException;

public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(CurrentUser currentUser) {
        HOLDER.set(currentUser);
    }

    public static CurrentUser require() {
        CurrentUser currentUser = HOLDER.get();
        if (currentUser == null) {
            throw BizException.unauthorized("请先登录");
        }
        return currentUser;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record CurrentUser(Long userId, String username, String role, Long tokenId) {
        public boolean isAdmin() {
            return "ADMIN".equalsIgnoreCase(role);
        }
    }
}
