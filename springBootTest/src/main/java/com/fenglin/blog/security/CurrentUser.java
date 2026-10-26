package com.fenglin.blog.security;

/** 当前请求的登录用户（游客时为 null，由 {@link CurrentUserHolder} 持有） */
public record CurrentUser(Long userId, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
