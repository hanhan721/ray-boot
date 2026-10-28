package com.ray.system.service.impl;

import com.ray.common.core.constant.ResultCode;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.core.util.IpUtils;
import com.ray.common.core.util.UserAgentUtils;
import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.domain.LoginUser;
import com.ray.common.security.service.TokenService;
import com.ray.common.security.util.SecurityUtils;
import com.ray.common.valkey.util.ValkeyUtils;
import com.ray.system.api.domain.LoginRequest;
import com.ray.system.api.domain.LoginVO;
import com.ray.system.api.domain.RefreshVO;
import com.ray.system.api.domain.UserInfoVO;
import com.ray.system.api.domain.UserPermissionDTO;
import com.ray.system.api.entity.SysPermission;
import com.ray.system.api.entity.SysRole;
import com.ray.system.event.LoginLogEvent;
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
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RefreshTokenService refreshTokenService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final ValkeyUtils valkeyUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        var username = request.username();

        // 1. 检查登录失败次数
        this.checkLoginAttempts(username);

        // 2. 查询用户
        var clientIp = this.getClientIp(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");
        var user = userService.getByUsername(username);
        // 两个分支执行相同的匹配流程，超长密码也不能通过 encode/matches 的差异枚举用户。
        var passwordHash = user != null ? user.getPassword() : DUMMY_PASSWORD_HASH;
        var passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
        if (user == null || !passwordMatches) {
            this.recordLoginFail(username);
            this.publishLoginLog(username, clientIp, userAgent, false, "用户名或密码错误");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 3. 与角色更新共用用户行锁，并重新检查 BCrypt 运算期间可能发生的禁用或密码重置。
        user = userService.getByIdForUpdate(user.getId());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !Objects.equals(passwordHash, user.getPassword())) {
            this.recordLoginFail(username);
            this.publishLoginLog(username, clientIp, userAgent, false, "账号不存在、已被禁用或凭据已变更");
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 4. 加载角色
        var roleKeys = this.getRoleKeys(user.getId());

        // 5. 缓存菜单和权限
        this.cacheUserPermissions(user.getId(), roleKeys);

        // 6. 生成 Access Token
        var loginUser = new LoginUser(user.getId(), user.getUsername(), roleKeys, LocalDateTime.now(), clientIp);
        var browser = UserAgentUtils.parseBrowser(userAgent);
        var os = UserAgentUtils.parseOs(userAgent);
        var accessToken = tokenService.createAccessToken(loginUser, user.getNickname(), browser, os);

        // 7. 支持多设备并发登录，不再清除旧 Refresh Token
        // 如需恢复单设备登录策略，取消下行注释即可
        // refreshTokenService.deleteByUserId(user.getId());

        // 8. 生成新的 Refresh Token
        var rawRefreshToken = UUID.randomUUID().toString();
        refreshTokenService.create(rawRefreshToken, user.getId(), accessToken, userAgent, clientIp);

        // 9. 设置新的 Refresh Token Cookie
        this.setRefreshTokenCookie(httpRequest, httpResponse, rawRefreshToken);

        // 10. 清除登录失败计数
        this.clearLoginFail(username);

        // 11. 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now())
                .setLastLoginIp(clientIp);
        userService.updateById(user);

        log.info("用户登录成功: username={}, ip={}", username, clientIp);

        // 12. 记录登录日志
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
        tokenService.removeAccessToken(refreshToken.getAccessToken(), user.getId());

        // 5. 删除旧 Refresh Token
        refreshTokenService.deleteByTokenHash(refreshToken.getTokenHash());

        // 6. 生成新 Access Token
        var roleKeys = this.getRoleKeys(user.getId());
        var clientIp = this.getClientIp(httpRequest);
        var userAgent = httpRequest.getHeader("User-Agent");
        var loginUser = new LoginUser(user.getId(), user.getUsername(), roleKeys, LocalDateTime.now(), clientIp);
        var newAccessToken = tokenService.createAccessToken(loginUser, user.getNickname(),
                UserAgentUtils.parseBrowser(userAgent), UserAgentUtils.parseOs(userAgent));

        // 7. 刷新菜单和权限缓存
        this.cacheUserPermissions(user.getId(), roleKeys);

        // 8. 生成新 Refresh Token (Rotation)
        var newRawRefreshToken = UUID.randomUUID().toString();
        refreshTokenService.create(newRawRefreshToken, user.getId(), newAccessToken, userAgent, clientIp);

        // 9. 设置新 Refresh Token Cookie
        this.setRefreshTokenCookie(httpRequest, httpResponse, newRawRefreshToken);

        log.info("Token 续期成功: userId={}", user.getId());

        var expireIn = (long) authProperties.getToken().getAccessExpire() * 60;
        return new RefreshVO(newAccessToken, expireIn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        var loginUser = SecurityUtils.getLoginUser();
        var accessToken = SecurityUtils.getAccessToken();
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

        if (loginUser != null && accessToken != null) {
            // 删除当前 Access Token
            tokenService.removeAccessToken(accessToken, loginUser.userId());
            // 删除与当前 Access Token 绑定的 Refresh Token，防止登出后仍可续期
            refreshTokenService.deleteByAccessToken(accessToken);

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
                tokenService.removeAccessToken(verified.getAccessToken(), verified.getUserId());
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

        // 直接从缓存读取菜单和权限
        var menusJson = tokenService.getMenus(loginUser.userId());
        var menus = this.deserializeMenus(menusJson);

        var permissions = tokenService.getPermissions(loginUser.userId());

        return new UserInfoVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                loginUser.roles(),
                menus,
                permissions
        );
    }

    @Override
    public void kickUser(Long userId) {
        // 删除所有 Access Token
        tokenService.removeAllTokens(userId);
        // 删除所有 Refresh Token
        refreshTokenService.deleteByUserId(userId);
        // 清除菜单缓存
        tokenService.removeMenus(userId);
        // 清除权限缓存
        tokenService.removePermissions(userId);

        log.info("踢掉用户所有设备: userId={}", userId);
    }

    // ==================== 登录失败限制 ====================

    private void checkLoginAttempts(String username) {
        var key = AuthCacheConstants.LOGIN_FAIL_KEY_PREFIX + username;
        var attempts = valkeyUtils.get(key, Integer.class);

        if (attempts != null && attempts >= authProperties.getSecurity().getMaxLoginAttempts()) {
            var ttl = valkeyUtils.getExpire(key);
            var minutes = ttl != null ? (ttl / 60 + 1) : authProperties.getSecurity().getLockDuration();
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录失败次数过多，请" + minutes + "分钟后重试");
        }
    }

    private void recordLoginFail(String username) {
        var key = AuthCacheConstants.LOGIN_FAIL_KEY_PREFIX + username;
        var attempts = valkeyUtils.increment(key);
        if (attempts != null && attempts == 1) {
            valkeyUtils.expire(key, authProperties.getSecurity().getLockDuration(), TimeUnit.MINUTES);
        }
    }

    private void clearLoginFail(String username) {
        valkeyUtils.delete(AuthCacheConstants.LOGIN_FAIL_KEY_PREFIX + username);
    }

    // ==================== 登录日志 ====================

    private void publishLoginLog(String username, String ip, String userAgent, boolean success, String msg) {
        eventPublisher.publishEvent(new LoginLogEvent(username, ip, userAgent, success, msg, LocalDateTime.now()));
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取用户角色标识集合
     */
    private Set<String> getRoleKeys(Long userId) {
        var roleIds = userRoleService.getRoleIdsByUserId(userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            return Set.of();
        }
        return roleService.listByIds(roleIds).stream()
                .filter(role -> Boolean.TRUE.equals(role.getEnabled()))
                .map(SysRole::getRoleKey)
                .collect(Collectors.toSet());
    }

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

    /**
     * 检查是否为超管角色
     */
    private boolean isSuperAdmin(Set<String> roleKeys) {
        if (CollectionUtils.isEmpty(roleKeys)) {
            return false;
        }
        var superAdminRoleCodes = authProperties.getSuperAdmin().getRoleCodes();
        if (CollectionUtils.isEmpty(superAdminRoleCodes)) {
            return false;
        }
        return roleKeys.stream().anyMatch(superAdminRoleCodes::contains);
    }

    /**
     * 缓存用户菜单和权限
     * 一次查询权限，然后在 Java 中分离菜单和权限编码
     * 超管和普通用户都只需一次数据库查询
     */
    private void cacheUserPermissions(Long userId, Set<String> roleKeys) {
        UserPermissionDTO permissionDto;

        // 根据角色分别查询：超管查所有权限，普通用户查个人权限
        if (this.isSuperAdmin(roleKeys)) {
            // 超管：一次查询所有权限
            permissionDto = permissionService.getAllPermissionsDTO();
        } else {
            // 普通用户：一次查询个人权限
            permissionDto = permissionService.getUserPermissions(userId);
        }

        // 缓存菜单树
        var menusJson = this.serializeMenus(permissionDto.menus());
        tokenService.cacheMenus(userId, menusJson);

        // 缓存权限编码
        tokenService.cachePermissions(userId, permissionDto.permissions());
    }

    /**
     * 菜单树序列化为 JSON
     */
    private String serializeMenus(List<SysPermission> menus) {
        try {
            return jsonMapper.writeValueAsString(menus);
        } catch (Exception e) {
            log.error("菜单序列化失败", e);
            return "[]";
        }
    }

    /**
     * 菜单树从 JSON 反序列化
     */
    private List<SysPermission> deserializeMenus(String menusJson) {
        try {
            if (menusJson == null || menusJson.isBlank()) {
                return List.of();
            }
            var ref = jsonMapper.getTypeFactory().constructCollectionType(List.class, SysPermission.class);
            return jsonMapper.readValue(menusJson, ref);
        } catch (Exception e) {
            log.error("菜单反序列化失败", e);
            return List.of();
        }
    }
}
