package com.fenglin.blog.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 访问令牌 / 重置令牌的生成与摘要。
 *
 * <p>这里用 SHA-256 而不是 BCrypt：令牌需要按摘要精确反查数据库，
 * BCrypt 每次结果不同、无法用于等值查询。令牌本身是 256 位随机数，
 * 不存在彩虹表问题，SHA-256 已足够。密码仍用 BCrypt。
 */
public final class TokenHash {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenHash() {
    }

    /** 生成 64 位十六进制随机令牌（明文，仅返回给客户端一次） */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16))
                    .append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    /** 令牌摘要，用于入库与查询 */
    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(Character.forDigit((b >> 4) & 0xf, 16))
                        .append(Character.forDigit(b & 0xf, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
