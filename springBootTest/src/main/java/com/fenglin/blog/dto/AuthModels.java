package com.fenglin.blog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthModels {

    private AuthModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegisterRequest(
            @NotBlank(message = "请输入用户名")
            @Size(min = 3, max = 20, message = "用户名需 3-20 个字符")
            String username,
            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 32, message = "密码需 6-32 位")
            String password,
            @NotBlank(message = "请再次输入密码")
            String confirmPassword,
            @NotBlank(message = "请输入邮箱，用于找回密码")
            @Email(message = "邮箱格式不正确")
            @Size(max = 100, message = "邮箱不能超过 100 个字符")
            String email,
            @Size(max = 50, message = "昵称不能超过 50 个字符")
            String nickname) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginRequest(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入密码") String password,
            Boolean remember) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerifyIdentityRequest(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入注册时填写的邮箱")
            @Email(message = "邮箱格式不正确") String email) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResetPasswordRequest(
            @NotBlank(message = "重置凭证缺失，请重新验证身份") String token,
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 32, message = "密码需 6-32 位") String newPassword,
            @NotBlank(message = "请再次输入新密码") String confirmPassword) {
    }

    public record UserVO(
            Long id,
            String username,
            String nickname,
            String email,
            String avatar,
            String signature,
            String faction,
            String role,
            LocalDateTime createdAt,
            String token,
            LocalDateTime tokenExpiresAt) {
    }
}
