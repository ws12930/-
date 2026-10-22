package com.fenglin.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.blog.common.BizException;
import com.fenglin.blog.dto.AuthModels.LoginRequest;
import com.fenglin.blog.dto.AuthModels.RegisterRequest;
import com.fenglin.blog.dto.AuthModels.ResetPasswordRequest;
import com.fenglin.blog.dto.AuthModels.UserVO;
import com.fenglin.blog.dto.AuthModels.VerifyIdentityRequest;
import com.fenglin.blog.dto.SocialModels.ProfileUpdateRequest;
import com.fenglin.blog.dto.SocialModels.UserProfileVO;
import com.fenglin.blog.entity.AccessToken;
import com.fenglin.blog.entity.Follow;
import com.fenglin.blog.entity.Post;
import com.fenglin.blog.entity.User;
import com.fenglin.blog.mapper.AccessTokenMapper;
import com.fenglin.blog.mapper.FollowMapper;
import com.fenglin.blog.mapper.PostMapper;
import com.fenglin.blog.mapper.UserMapper;
import com.fenglin.blog.security.CurrentUser;
import com.fenglin.blog.security.CurrentUserHolder;
import com.fenglin.blog.security.TokenHash;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class UserService {

    /** 记住我 30 天，否则 7 天 */
    private static final long REMEMBER_DAYS = 30L;
    private static final long DEFAULT_DAYS = 7L;
    /** 重置令牌有效期（分钟） */
    private static final long RESET_MINUTES = 30L;

    private static final String BEARER = "Bearer ";

    private final UserMapper userMapper;
    private final AccessTokenMapper accessTokenMapper;
    private final FollowMapper followMapper;
    private final PostMapper postMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, AccessTokenMapper accessTokenMapper,
                       FollowMapper followMapper, PostMapper postMapper,
                       PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.accessTokenMapper = accessTokenMapper;
        this.followMapper = followMapper;
        this.postMapper = postMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /* ================= 注册 / 登录 ================= */

    @Transactional
    public UserVO register(RegisterRequest request) {
        String username = request.username().trim();
        if (!request.password().equals(request.confirmPassword())) {
            throw BizException.badRequest("两次输入的密码不一致");
        }
        if (countByUsername(username) > 0) {
            throw BizException.badRequest("用户名已被占用");
        }
        if (countByEmail(request.email().trim()) > 0) {
            throw BizException.badRequest("该邮箱已注册，请直接登录或换一个邮箱");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(blankToNull(request.nickname()) == null ? username : request.nickname().trim());
        user.setEmail(request.email().trim());
        user.setAvatar("ninja:0");
        user.setSignature("");
        user.setFaction("ninja");
        user.setRole("USER");
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        return toVO(user, issueToken(user, false));
    }

    @Transactional
    public UserVO login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BizException.unauthorized("用户名或密码不正确");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BizException.forbidden("账号已被禁用，请联系管理员");
        }
        boolean remember = Boolean.TRUE.equals(request.remember());
        return toVO(user, issueToken(user, remember));
    }

    public boolean isUsernameAvailable(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return countByUsername(username.trim()) == 0;
    }

    /* ================= 找回密码 ================= */

    @Transactional
    public String createResetToken(VerifyIdentityRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username().trim()));
        if (user == null) {
            throw BizException.badRequest("用户名不存在");
        }
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(request.email().trim())) {
            throw BizException.badRequest("邮箱与注册时填写的不一致");
        }
        String token = TokenHash.randomToken();
        user.setResetTokenHash(TokenHash.sha256(token));
        user.setResetTokenExpire(LocalDateTime.now().plusMinutes(RESET_MINUTES));
        userMapper.updateById(user);
        return token;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw BizException.badRequest("两次输入的新密码不一致");
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getResetTokenHash, TokenHash.sha256(request.token())));
        if (user == null) {
            throw BizException.badRequest("重置凭证无效，请重新验证身份");
        }
        if (user.getResetTokenExpire() == null
                || user.getResetTokenExpire().isBefore(LocalDateTime.now())) {
            throw BizException.badRequest("重置凭证已过期，请重新验证身份");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetTokenHash(null);
        user.setResetTokenExpire(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 密码变更即吊销该用户全部令牌，避免旧会话继续可用
        accessTokenMapper.delete(new LambdaQueryWrapper<AccessToken>()
                .eq(AccessToken::getUserId, user.getId()));
    }

    /* ================= 当前用户 ================= */

    public UserVO currentUser() {
        CurrentUser current = CurrentUserHolder.require();
        User user = userMapper.selectById(current.userId());
        if (user == null) {
            throw BizException.unauthorized("登录状态已失效，请重新登录");
        }
        return toVO(user, null);
    }

    @Transactional
    public UserVO updateProfile(ProfileUpdateRequest request) {
        User user = userMapper.selectById(CurrentUserHolder.require().userId());
        if (user == null) {
            throw BizException.unauthorized("登录状态已失效，请重新登录");
        }
        if (request.nickname() != null) {
            user.setNickname(request.nickname().trim());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar().trim());
        }
        if (request.signature() != null) {
            user.setSignature(request.signature().trim());
        }
        if (request.faction() != null) {
            user.setFaction(normalizeFaction(request.faction()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toVO(user, null);
    }

    /** 退出登录：只吊销当前请求携带的令牌 */
    @Transactional
    public void logout() {
        String header = currentAuthorization();
        if (header == null || !header.startsWith(BEARER)) {
            return;
        }
        String token = header.substring(BEARER.length()).trim();
        accessTokenMapper.delete(new LambdaQueryWrapper<AccessToken>()
                .eq(AccessToken::getTokenHash, TokenHash.sha256(token)));
    }

    /* ================= 公开资料 ================= */

    public UserProfileVO profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return toProfile(user);
    }

    public List<UserProfileVO> search(String keyword, long limit) {
        String kw = keyword == null ? "" : keyword.trim();
        // 必须包一层括号，否则 OR 会被 AND status=1 截断优先级
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .and(wrapper -> wrapper.like(User::getNickname, kw).or().like(User::getUsername, kw))
                .eq(User::getStatus, 1)
                .last("LIMIT " + Math.max(1, limit));
        List<UserProfileVO> list = new ArrayList<>();
        for (User user : userMapper.selectList(query)) {
            list.add(toProfile(user));
        }
        return list;
    }

    /** 社区页：按粉丝数倒序的活跃博主 */
    public List<UserProfileVO> community(long limit) {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
        List<UserProfileVO> list = new ArrayList<>();
        for (User user : users) {
            list.add(toProfile(user));
        }
        list.sort(Comparator.comparing(UserProfileVO::followerCount).reversed()
                .thenComparing(UserProfileVO::postCount, Comparator.reverseOrder()));
        int max = (int) Math.max(1, Math.min(limit, list.size()));
        return list.subList(0, max);
    }

    /* ================= 内部辅助 ================= */

    /** 签发访问令牌：明文只在响应中出现一次，库里只存摘要 */
    private TokenPair issueToken(User user, boolean remember) {
        String token = TokenHash.randomToken();
        LocalDateTime expireAt = LocalDateTime.now().plusDays(remember ? REMEMBER_DAYS : DEFAULT_DAYS);
        AccessToken record = new AccessToken();
        record.setUserId(user.getId());
        record.setTokenHash(TokenHash.sha256(token));
        record.setExpireAt(expireAt);
        record.setCreatedAt(LocalDateTime.now());
        accessTokenMapper.insert(record);
        return new TokenPair(token, expireAt);
    }

    private record TokenPair(String token, LocalDateTime expireAt) {
    }

    private UserProfileVO toProfile(User user) {
        CurrentUser current = CurrentUserHolder.get();
        Long myId = current == null ? null : current.userId();
        long postCount = postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getAuthorId, user.getId())
                .ne(Post::getStatus, "DRAFT"));
        long followerCount = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, user.getId()));
        long followingCount = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, user.getId()));
        boolean self = myId != null && myId.equals(user.getId());
        boolean followed = !self && myId != null && followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, myId)
                        .eq(Follow::getFolloweeId, user.getId())) > 0;
        // 互相关注：我关注了 TA，且 TA 也关注了我
        boolean mutual = followed && followMapper.selectCount(
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, user.getId())
                        .eq(Follow::getFolloweeId, myId)) > 0;
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getSignature(),
                user.getFaction(),
                user.getCreatedAt(),
                postCount,
                followerCount,
                followingCount,
                followed,
                self,
                mutual);
    }

    private UserVO toVO(User user, TokenPair tokenPair) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar(),
                user.getSignature(),
                user.getFaction(),
                user.getRole(),
                user.getCreatedAt(),
                tokenPair == null ? null : tokenPair.token(),
                tokenPair == null ? null : tokenPair.expireAt());
    }

    private long countByUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private long countByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    private String normalizeFaction(String faction) {
        String value = faction == null ? "" : faction.trim().toLowerCase();
        return "shinigami".equals(value) ? "shinigami" : "ninja";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String currentAuthorization() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getHeader("Authorization");
        }
        return null;
    }
}
