package com.fenglin.blog.common;

import java.io.Serializable;

public record R<T>(int code, String message, T data) implements Serializable {

    public static final int OK = 0;
    public static final int FAIL = 1;

    public static <T> R<T> ok() {
        return new R<>(OK, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(OK, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(OK, message, data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }
}
