package com.ray.system.service.impl;

import com.ray.common.core.constant.ResultCode;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.service.TokenService;
import com.ray.common.valkey.util.ValkeyUtils;
import com.ray.system.api.domain.LoginRequest;
import com.ray.system.api.entity.RefreshToken;
import com.ray.system.api.entity.SysUser;
import com.ray.system.service.PermissionService;
import com.ray.system.service.RefreshTokenService;
import com.ray.system.service.RoleService;
import com.ray.system.service.UserRoleService;
import com.ray.system.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final UserService users = mock(UserService.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final TokenService tokens = mock(TokenService.class);
    private final ValkeyUtils valkey = mock(ValkeyUtils.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AuthServiceImpl auth = new AuthServiceImpl(users, mock(UserRoleService.class),
            mock(RoleService.class), mock(PermissionService.class), refreshTokens, tokens, encoder,
            new AuthProperties(), valkey, mock(ApplicationEventPublisher.class), JsonMapper.builder().build());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", "密"})
    void longPasswordsHaveTheSameFailureForKnownAndUnknownUsers(String character) {
        var password = character.repeat(73);
        var existing = new SysUser().setUsername("known").setPassword(encoder.encode("correct-password"));
        when(users.getByUsername("known")).thenReturn(existing);

        for (var username : new String[]{"unknown", "known"}) {
            var error = assertThrows(BusinessException.class, () -> auth.login(
                    new LoginRequest(username, password), new MockHttpServletRequest(), new MockHttpServletResponse()));

            assertEquals(ResultCode.UNAUTHORIZED.getCode(), error.getCode());
            assertEquals("用户名或密码错误", error.getMsg());
            verify(valkey).increment(AuthCacheConstants.LOGIN_FAIL_KEY_PREFIX + username);
        }
    }

    @Test
    void logoutWithOnlyRefreshCookieRevokesBothTokensAndClearsCookie() {
        SecurityContextHolder.clearContext();
        var refresh = new RefreshToken().setUserId(7L).setAccessToken("old-access").setTokenHash("stored-hash");
        when(refreshTokens.verify("raw-refresh")).thenReturn(refresh);
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "raw-refresh"));
        var response = new MockHttpServletResponse();

        auth.logout(request, response);

        verify(tokens).removeAccessToken("old-access", 7L);
        verify(refreshTokens).deleteByTokenHash("stored-hash");
        var cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("refresh_token="));
        assertTrue(cookie.contains("Max-Age=0"));
        assertTrue(cookie.contains("Path=/api/auth"));
    }
}
