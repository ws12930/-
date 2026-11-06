package com.fenglin.blog.common;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {

    /** 封禁提示统一文案：登录、鉴权拦截、前端弹窗都从这里取，避免各写一份 */
    public static final String BANNED_MESSAGE = "该账号已被封禁。请联系管理员。";

    private final int code;
    private final HttpStatus status;

    public BizException(int code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BizException badRequest(String message) {
        return new BizException(40000, HttpStatus.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(40100, HttpStatus.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(40300, HttpStatus.FORBIDDEN, message);
    }

    /**
     * 账号被封禁（40301）。
     *
     * <p>单独给一个错误码，前端据此「清本地登录态 + 弹窗」；不能复用 40300，
     * 否则普通越权（比如非管理员访问后台）也会把用户踢下线。
     */
    public static BizException banned(String message) {
        return new BizException(40301, HttpStatus.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(40400, HttpStatus.NOT_FOUND, message);
    }
}
