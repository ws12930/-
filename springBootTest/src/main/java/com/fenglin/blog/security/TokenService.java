package com.fenglin.blog.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.entity.AccessToken;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.AccessTokenMapper;
import com.fenglin.blog.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

    private static final long SHORT_SESSION_HOURS = 2;
    private static final long REMEMBER_SESSION_DAYS = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccessTokenMapper accessTokenMapper;
    private final UserMapper userMapper;

    public TokenService(AccessTokenMapper accessTokenMapper, UserMapper userMapper) {
        this.accessTokenMapper = accessTokenMapper;
        this.userMapper = userMapper;
    }

    public TokenInfo issue(Long userId, boolean remember) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        LocalDateTime expireAt = remember
                ? LocalDateTime.now().plusDays(REMEMBER_SESSION_DAYS)
                : LocalDateTime.now().plusHours(SHORT_SESSION_HOURS);

        AccessToken token = new AccessToken();
        token.setUserId(userId);
        token.setTokenHash(hashValue(rawToken));
        token.setExpireAt(expireAt);
        token.setCreatedAt(LocalDateTime.now());
        accessTokenMapper.insert(token);
        return new TokenInfo(rawToken, expireAt);
    }

    public CurrentUserHolder.CurrentUser authenticate(String authorization) {
        String rawToken = parseBearerToken(authorization);
        AccessToken token = accessTokenMapper.selectOne(Wrappers.<AccessToken>lambdaQuery()
                .eq(AccessToken::getTokenHash, hashValue(rawToken))
                .last("LIMIT 1"));
        if (token == null || token.getExpireAt() == null || !token.getExpireAt().isAfter(LocalDateTime.now())) {
            if (token != null) {
                accessTokenMapper.deleteById(token.getId());
            }
            throw BizException.unauthorized("登录状态已失效，请重新登录");
        }

        User user = userMapper.selectById(token.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.unauthorized("账号不可用，请重新登录");
        }
        return new CurrentUserHolder.CurrentUser(user.getId(), user.getUsername(), user.getRole(), token.getId());
    }

    public void invalidate(Long tokenId) {
        if (tokenId != null) {
            accessTokenMapper.deleteById(tokenId);
        }
    }

    public void invalidateUserTokens(Long userId) {
        accessTokenMapper.delete(Wrappers.<AccessToken>lambdaQuery().eq(AccessToken::getUserId, userId));
    }

    public String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 Java 环境不支持 SHA-256", e);
        }
    }

    private String parseBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BizException.unauthorized("缺少登录凭证");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw BizException.unauthorized("登录凭证格式错误");
        }
        return token;
    }

    public record TokenInfo(String token, LocalDateTime expireAt) {
    }
}
