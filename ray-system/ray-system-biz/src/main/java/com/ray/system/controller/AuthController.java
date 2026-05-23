package com.ray.system.controller;

import com.ray.common.core.domain.R;
import com.ray.system.api.domain.LoginRequest;
import com.ray.system.api.domain.LoginVO;
import com.ray.system.api.domain.RefreshVO;
import com.ray.system.api.domain.UserInfoVO;
import com.ray.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证管理
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginRequest request,
                            HttpServletRequest httpRequest,
                            HttpServletResponse httpResponse) {
        return R.ok(authService.login(request, httpRequest, httpResponse));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return R.ok();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public R<RefreshVO> refresh(HttpServletRequest httpRequest,
                                HttpServletResponse httpResponse) {
        return R.ok(authService.refresh(httpRequest, httpResponse));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userinfo")
    public R<UserInfoVO> getUserInfo() {
        return R.ok(authService.getUserInfo());
    }
}
