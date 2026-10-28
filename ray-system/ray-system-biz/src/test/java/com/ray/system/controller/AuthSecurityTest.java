package com.ray.system.controller;

import com.ray.common.core.util.IpUtils;
import com.ray.common.security.config.SecurityConfig;
import com.ray.common.security.filter.TokenAuthenticationFilter;
import com.ray.common.security.handler.AccessDeniedHandlerImpl;
import com.ray.common.security.handler.AuthenticationEntryPointImpl;
import com.ray.common.security.service.TokenService;
import com.ray.framework.web.filter.ClientIpFilter;
import com.ray.system.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AuthSecurityTest.TestConfig.class)
class AuthSecurityTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private AuthService auth;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        clearInvocations(auth);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(new ClientIpFilter(new String[]{"10.0.0.5"}), context.getBean(FilterChainProxy.class))
                .build();
    }

    @Test
    void expiredAccessTokenCanReachLogoutWithRefreshCookie() throws Exception {
        mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer expired")
                        .header("X-Forwarded-For", "203.0.113.123, 198.51.100.7")
                        .cookie(new Cookie("refresh_token", "still-valid"))
                        .with(request -> { request.setRemoteAddr("10.0.0.5"); return request; }))
                .andExpect(status().isOk());

        var request = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(auth).logout(request.capture(), any());
        assertEquals("198.51.100.7", IpUtils.getClientIp(request.getValue()));
    }

    @Test
    void userInfoStillRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/auth/userinfo")).andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, TokenAuthenticationFilter.class,
            AuthenticationEntryPointImpl.class, AccessDeniedHandlerImpl.class, AuthController.class})
    static class TestConfig {
        @Bean
        AuthService authService() { return mock(AuthService.class); }

        @Bean
        TokenService tokenService() { return mock(TokenService.class); }

        @Bean
        JsonMapper jsonMapper() { return JsonMapper.builder().build(); }
    }
}
