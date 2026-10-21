package com.fenglin.blog.controller;

import com.fenglin.blog.common.PageResult;
import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.BlogModels.PostVO;
import com.fenglin.blog.dto.SocialModels.ConversationVO;
import com.fenglin.blog.dto.SocialModels.MessageSendRequest;
import com.fenglin.blog.dto.SocialModels.MessageVO;
import com.fenglin.blog.dto.SocialModels.UserProfileVO;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.FollowService;
import com.fenglin.blog.service.MessageService;
import com.fenglin.blog.service.PostService;
import com.fenglin.blog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SocialController {

    private final UserService userService;
    private final FollowService followService;
    private final MessageService messageService;
    private final PostService postService;

    public SocialController(UserService userService, FollowService followService,
                            MessageService messageService, PostService postService) {
        this.userService = userService;
        this.followService = followService;
        this.messageService = messageService;
        this.postService = postService;
    }

    /* ===== 用户资料 / 主页 ===== */

    @GetMapping("/users/{userId}")
    public R<UserProfileVO> profile(@PathVariable Long userId) {
        return R.ok(userService.profile(userId));
    }

    @GetMapping("/users/search")
    public R<List<UserProfileVO>> search(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "10") long limit) {
        return R.ok(userService.search(keyword, limit));
    }

    /** 博客社区：全部活跃博主（按粉丝数排序） */
    @GetMapping("/community/users")
    public R<List<UserProfileVO>> community(@RequestParam(defaultValue = "100") long limit) {
        return R.ok(userService.community(limit));
    }

    /** 某用户已发布的文章 */
    @GetMapping("/users/{userId}/posts")
    public R<PageResult<PostVO>> userPosts(@PathVariable Long userId,
                                           @RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size) {
        return R.ok(postService.listByAuthor(userId, current, size));
    }

    /* ===== 关注 ===== */

    @RequireAuth
    @PostMapping("/users/{userId}/follow")
    public R<Map<String, Object>> follow(@PathVariable Long userId) {
        var result = followService.toggle(userId);
        String message = !result.followed()
                ? "已取消关注"
                : (result.mutual() ? "已关注，你们互相关注了" : "已关注");
        return R.ok(message,
                Map.of("followed", result.followed(),
                        "followerCount", result.followerCount(),
                        "mutual", result.mutual()));
    }

    @GetMapping("/users/{userId}/following")
    public R<List<UserProfileVO>> following(@PathVariable Long userId) {
        return R.ok(followService.listFollowing(userId));
    }

    @GetMapping("/users/{userId}/followers")
    public R<List<UserProfileVO>> followers(@PathVariable Long userId) {
        return R.ok(followService.listFollowers(userId));
    }

    /* ===== 私信 ===== */

    @RequireAuth
    @GetMapping("/messages")
    public R<List<ConversationVO>> conversations() {
        return R.ok(messageService.conversations());
    }

    @RequireAuth
    @GetMapping("/messages/unread")
    public R<Map<String, Long>> unread() {
        return R.ok(Map.of("count", messageService.unreadCount()));
    }

    @RequireAuth
    @GetMapping("/messages/{peerId}")
    public R<List<MessageVO>> history(@PathVariable Long peerId,
                                      @RequestParam(defaultValue = "100") long limit) {
        return R.ok(messageService.history(peerId, limit));
    }

    @RequireAuth
    @PostMapping("/messages/{peerId}")
    public R<MessageVO> send(@PathVariable Long peerId, @Valid @RequestBody MessageSendRequest request) {
        return R.ok("消息已发送", messageService.send(peerId, request));
    }
}
