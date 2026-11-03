package com.ray.system.service.impl;

import com.ray.common.core.constant.ResultCode;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.core.util.IpUtils;
import com.ray.common.core.util.UserAgentUtils;
import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.domain.LoginUser;
import com.ray.common.security.service.TokenService;
import com.ray.common.security.util.SecurityUtils;
import com.ray.system.api.domain.LoginRequest;
import com.ray.system.api.domain.LoginVO;
import com.ray.system.api.domain.RefreshVO;
import com.ray.system.api.domain.UserInfoVO;
import com.ray.system.event.LoginLogEvent;
import com.ray.system.security.AuthorizationSessionCoordinator;
import com.ray.system.service.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 认证 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$OL.cIpk1mSCW/8.JxafI9emBwBLGsjBS5m5q83Nzz14k8VioLmO2q";

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final LoginAttemptService loginAttemptService;
    private final ApplicationEventPublisher eventPublisher;
    private final PermissionService permissionService;
    private final AuthorizationSessionCoordinator sessionCoordinator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        var username = request.username();
        var clientIp = this.getClientIp(httpRequest);

        // 1. 检查登录失败次数
        loginAttemptService.checkAllowed(username);

        // 2. 查询用户
        var userAgent = httpRequest.getHeader("User-Agent");
        var user = userService.getByUsername(username);
        // 两个分支执行相同的匹配流程，超长密码也不能通过 encode/matches 的差异枚举用户。
        var passwordHash = user != null ? user.getPassword() : DUMMY_PASSWORD_HASH;
        var passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (user == null || !passwordMatches) {
            loginAttemptService.recordFailure(username);
            this.publishLoginLog(username, clientIp, userAgent, false, "用户名或密码错误");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        sessionCoordinator.lockForIssuance();

        // 3. 与角色更新共用用户行锁，并重新检查 BCrypt 运算期间可能发生的禁用或密码重置。
        user = userService.getByIdForUpdate(user.getId());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !Objects.equals(passwordHash, user.getPassword())) {
            loginAttemptService.recordFailure(username);
            this.publishLoginLog(username, clientIp, userAgent, false, "账号不存在、已被禁用或凭据已变更");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 4. 在共享协调锁和用户行锁内加载授权并签发会话。
        var loginUser = new LoginUser(user.getId(), user.getUsername(), Set.of(), LocalDateTime.now(), clientIp);
        var browser = UserAgentUtils.parseBrowser(userAgent);
        var os = UserAgentUtils.parseOs(userAgent);
        var accessToken = tokenService.createAccessToken(loginUser, user.getNickname(), browser, os);

        // 5. 生成新的 Refresh Token
        var rawRefreshToken = UUID.randomUUID().toString();
        refreshTokenService.create(rawRefreshToken, user.getId(), TokenService.hashAccessToken(accessToken), userAgent, clientIp);

        // 6. 设置新的 Refresh Token Cookie
        this.setRefreshTokenCookie(httpRequest, httpResponse, rawRefreshToken);

        // 7. 清除登录失败计数
        loginAttemptService.clearFailures(username);

        // 8. 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now())
                .setLastLoginIp(clientIp);
        userService.updateById(user);

        log.info("用户登录成功: username={}, ip={}", username, clientIp);

        // 9. 记录登录日志
        this.publishLoginLog(username, clientIp, userAgent, true, "登录成功");

        var expireIn = (long) authProperties.getToken().getAccessExpire() * 60;
        return new LoginVO(accessToken, expireIn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefreshVO refresh(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 1. 从 Cookie 提取 Refresh Token
        var rawRefreshToken = this.extractRefreshToken(httpRequest);
        if (rawRefreshToken == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token 不存在");
        }

        sessionCoordinator.lockForIssuance();

        // 2. 验证 Refresh Token
        var refreshToken = refreshTokenService.verify(rawRefreshToken);
        if (refreshToken == null) {
            this.clearRefreshTokenCookie(httpRequest, httpResponse);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token 无效或已过期");
        }

        // 3. 等待同一用户的角色更新/会话操作完成，再重新验证 Refresh Token，避免复用旧查询结果。
        var user = userService.getByIdForUpdate(refreshToken.getUserId());
        refreshToken = refreshTokenService.verify(rawRefreshToken);
        if (refreshToken == null) {
            this.clearRefreshTokenCookie(httpRequest, httpResponse);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Refresh Token 无效或已过期");
        }
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            refreshTokenService.deleteByUserId(refreshToken.getUserId());
            this.clearRefreshTokenCookie(httpRequest, httpResponse);
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已被禁用");
        }

        // 4. 删除旧 Access Token
        tokenService.revokeAfterCommit(Map.of(user.getId(), Set.of(refreshToken.getSessionId())));

        // 5. 删除旧 Refresh Token
        refreshTokenService.deleteByTokenHash(refreshToken.getTokenHash());

        // 6. 生成新 Access Token
        var clientIp = this.getClientIp(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");
        var loginUser = new LoginUser(user.getId(), user.getUsername(), Set.of(), LocalDateTime.now(), clientIp);
        var newAccessToken = tokenService.createAccessToken(loginUser, user.getNickname(),
                UserAgentUtils.parseBrowser(userAgent), UserAgentUtils.parseOs(userAgent));

        // 7. 生成新 Refresh Token (Rotation)
        var newRawRefreshToken = UUID.randomUUID().toString();
        refreshTokenService.create(newRawRefreshToken, user.getId(), TokenService.hashAccessToken(newAccessToken), userAgent, clientIp);

        // 8. 设置新 Refresh Token Cookie
        this.setRefreshTokenCookie(httpRequest, httpResponse, newRawRefreshToken);

        log.info("Token 续期成功: userId={}", user.getId());

        var expireIn = (long) authProperties.getToken().getAccessExpire() * 60;
        return new RefreshVO(newAccessToken, expireIn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        sessionCoordinator.lockForIssuance();
        var loginUser = SecurityUtils.getLoginUser();
        var sessionId = SecurityUtils.getSessionId();
        var rawRefreshToken = this.extractRefreshToken(httpRequest);
        var refreshToken = rawRefreshToken != null && !rawRefreshToken.isBlank()
                ? refreshTokenService.verify(rawRefreshToken) : null;

        // 与续期共用用户行锁；两个凭据属于不同用户时按固定顺序加锁，避免交叉登出死锁。
        Stream.of(loginUser != null ? loginUser.userId() : null,
                        refreshToken != null ? refreshToken.getUserId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(userService::getByIdForUpdate);

        if (loginUser != null && sessionId != null) {
            // 删除当前 Access Token
            tokenService.revokeAfterCommit(Map.of(loginUser.userId(), Set.of(sessionId)));
            // 删除与当前 Access Token 绑定的 Refresh Token，防止登出后仍可续期
            refreshTokenService.deleteBySessionId(sessionId);

            // 记录登出日志
            var clientIp = this.getClientIp(httpRequest);
            var userAgent = httpRequest.getHeader("User-Agent");
            this.publishLoginLog(loginUser.username(), clientIp, userAgent, true, "退出登录");

            log.info("用户退出登录: username={}", loginUser.username());
        }

        // 兜底：如果请求中还带有 Refresh Token Cookie（例如 Access 已过期但 Refresh 仍有效），一并作废
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            var verified = refreshTokenService.verify(rawRefreshToken);
            if (verified != null) {
                tokenService.revokeAfterCommit(Map.of(verified.getUserId(), Set.of(verified.getSessionId())));
                refreshTokenService.deleteByTokenHash(verified.getTokenHash());
            }
        }

        // 清除 Cookie，Path 与签发时一致。
        this.clearRefreshTokenCookie(httpRequest, httpResponse);
    }

    @Override
    public UserInfoVO getUserInfo() {
        var loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        var user = userService.getById(loginUser.userId());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户不存在");
        }

        var authorization = tokenService.getAuthorization(loginUser.userId());
        var superAdmin = authorization.roles().stream().anyMatch(authProperties.getSuperAdmin().getRoleCodes()::contains);
        var menus = superAdmin ? permissionService.getAllPermissionsDTO().menus()
                : permissionService.getUserPermissions(loginUser.userId()).menus();

        return new UserInfoVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                authorization.roles(),
                menus,
                authorization.permissions()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickUser(Long userId) {
        sessionCoordinator.lockForChange();
        sessionCoordinator.revokeUsers(Set.of(userId));
        log.info("踢掉用户所有设备: userId={}", userId);
    }

    // ==================== 登录日志 ====================

    private void publishLoginLog(String username, String ip, String userAgent, boolean success, String msg) {
        eventPublisher.publishEvent(new LoginLogEvent(username, ip, userAgent, success, msg, LocalDateTime.now()));
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 Cookie 中提取 Refresh Token
     */
    private String extractRefreshToken(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 设置 Refresh Token 到 HttpOnly Cookie
     * <p>
     * Secure 跟随当前请求是否为 HTTPS（本地 HTTP 调试可用，生产 HTTPS 自动加 Secure）；
     * SameSite 固定 Lax，阻断第三方站点携带 Cookie 发起 CSRF，同時不影响顶级导航。
     */
    private void setRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response, String rawToken) {
        var cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, rawToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(authProperties.getToken().getRefreshExpire()))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 清除 Refresh Token Cookie（Path 与签发时一致，沿用其他安全属性）
     */
    private void clearRefreshTokenCookie(HttpServletRequest request, HttpServletResponse response) {
        var cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 获取客户端 IP（统一走 IpUtils，仅信任可信代理的透传头，防止客户端伪造 XFF 绕过限流/日志）
     */
    private String getClientIp(HttpServletRequest request) {
        return IpUtils.getClientIp(request);
    }
}
