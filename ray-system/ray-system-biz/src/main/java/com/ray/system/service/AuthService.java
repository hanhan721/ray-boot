package com.ray.system.service;

import com.ray.system.api.domain.LoginRequest;
import com.ray.system.api.domain.LoginVO;
import com.ray.system.api.domain.RefreshVO;
import com.ray.system.api.domain.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证 Service
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param request     登录请求
     * @param httpRequest HTTP 请求
     * @param httpResponse HTTP 响应
     * @return 登录结果
     */
    LoginVO login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * Token 续期
     *
     * @param httpRequest  HTTP 请求
     * @param httpResponse HTTP 响应
     * @return 续期结果
     */
    RefreshVO refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 退出登录
     *
     * @param httpRequest  HTTP 请求
     * @param httpResponse HTTP 响应
     */
    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    UserInfoVO getUserInfo();

    /**
     * 踢掉指定用户的所有登录设备
     *
     * @param userId 用户 ID
     */
    void kickUser(Long userId);
}
