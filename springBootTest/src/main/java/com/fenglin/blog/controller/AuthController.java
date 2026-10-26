package com.fenglin.blog.controller;

import com.fenglin.blog.common.R;
import com.fenglin.blog.dto.AuthModels.LoginRequest;
import com.fenglin.blog.dto.AuthModels.RegisterRequest;
import com.fenglin.blog.dto.AuthModels.ResetPasswordRequest;
import com.fenglin.blog.dto.AuthModels.UserVO;
import com.fenglin.blog.dto.AuthModels.VerifyIdentityRequest;
import com.fenglin.blog.dto.SocialModels.ProfileUpdateRequest;
import com.fenglin.blog.security.RequireAuth;
import com.fenglin.blog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok("注册成功", userService.register(request));
    }

    @PostMapping("/login")
    public R<UserVO> login(@Valid @RequestBody LoginRequest request) {
        return R.ok("登录成功", userService.login(request));
    }

    @GetMapping("/check-username")
    public R<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        return R.ok(Map.of("available", userService.isUsernameAvailable(username)));
    }

    @PostMapping("/forgot/verify")
    public R<Map<String, String>> verifyIdentity(@Valid @RequestBody VerifyIdentityRequest request) {
        return R.ok("身份验证通过", Map.of("token", userService.createResetToken(request)));
    }

    @PostMapping("/forgot/reset")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return R.ok("密码已重置，请使用新密码登录", null);
    }

    @RequireAuth
    @GetMapping("/me")
    public R<UserVO> me() {
        return R.ok(userService.currentUser());
    }

    @RequireAuth
    @PutMapping("/profile")
    public R<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return R.ok("资料已更新", userService.updateProfile(request));
    }

    @RequireAuth
    @PostMapping("/logout")
    public R<Void> logout() {
        userService.logout();
        return R.ok("已退出登录", null);
    }

    @GetMapping("/ping")
    public R<Map<String, String>> ping() {
        return R.ok(Map.of("status", "UP", "service", "blog-api"));
    }
}
