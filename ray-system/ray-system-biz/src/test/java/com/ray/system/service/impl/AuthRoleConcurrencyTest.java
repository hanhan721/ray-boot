package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.domain.LoginUser;
import com.ray.common.security.service.TokenService;
import com.ray.common.valkey.util.ValkeyUtils;
import com.ray.system.api.domain.UserPermissionDTO;
import com.ray.system.api.entity.RefreshToken;
import com.ray.system.api.entity.SysRole;
import com.ray.system.mapper.UserMapper;
import com.ray.system.service.PermissionService;
import com.ray.system.service.RefreshTokenService;
import com.ray.system.service.RoleService;
import com.ray.system.service.UserDeptService;
import com.ray.system.service.UserRoleService;
import jakarta.servlet.http.Cookie;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRoleConcurrencyTest {

    @Test
    void refreshWaitsForRoleChangeCommitAndUsesCommittedRoles() throws Exception {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, username VARCHAR(50), password VARCHAR(100), enabled BOOLEAN, deleted BOOLEAN)");
        jdbc.execute("CREATE TABLE sys_user_role (user_id BIGINT, role_id BIGINT)");
        jdbc.update("INSERT INTO sys_user VALUES (1, 'alice', 'unused', TRUE, FALSE)");
        jdbc.update("INSERT INTO sys_user_role VALUES (1, 1)");

        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(UserMapper.class);
        var sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setConfiguration(configuration);
        var mapper = new SqlSessionTemplate(sessionFactory.getObject()).getMapper(UserMapper.class);
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

        var userRoles = mock(UserRoleService.class);
        when(userRoles.getRoleIdsByUserId(1L)).thenAnswer(invocation ->
                new HashSet<>(jdbc.queryForList("SELECT role_id FROM sys_user_role WHERE user_id = 1", Long.class)));
        doAnswer(invocation -> {
            jdbc.update("DELETE FROM sys_user_role WHERE user_id = 1");
            Set<Long> ids = invocation.getArgument(1);
            for (var id : ids) {
                jdbc.update("INSERT INTO sys_user_role VALUES (1, ?)", id);
            }
            return null;
        }).when(userRoles).saveUserRoles(anyLong(), anySet());
        var roles = mock(RoleService.class);
        var roleById = Map.of(1L, new SysRole().setId(1L).setRoleKey("admin").setEnabled(true),
                2L, new SysRole().setId(2L).setRoleKey("member").setEnabled(true));
        when(roles.listByIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.stream().map(roleById::get).toList();
        });
        var permissions = mock(PermissionService.class);
        when(permissions.getCodesByUserId(1L)).thenReturn(Set.of("profile:read"));
        when(permissions.getUserPermissions(1L)).thenReturn(new UserPermissionDTO(List.of(), Set.of("profile:read")));
        when(permissions.getAllPermissionsDTO()).thenReturn(new UserPermissionDTO(List.of(), Set.of("admin:write")));
        var refreshTokens = mock(RefreshTokenService.class);
        var refreshAttempted = new CountDownLatch(1);
        when(refreshTokens.verify("refresh")).thenAnswer(invocation -> {
            refreshAttempted.countDown();
            return new RefreshToken().setUserId(1L).setAccessToken("old-access").setTokenHash("hash");
        });
        var tokens = mock(TokenService.class);
        var issuedUser = new AtomicReference<LoginUser>();
        when(tokens.createAccessToken(any(), any(), any(), any())).thenAnswer(invocation -> {
            issuedUser.set(invocation.getArgument(0));
            return "new-access";
        });
        var revoked = new CountDownLatch(1);
        var allowCommit = new CountDownLatch(1);
        doAnswer(invocation -> {
            revoked.countDown();
            assertTrue(allowCommit.await(5, SECONDS), "角色变更事务未获准提交");
            return null;
        }).when(tokens).removeAllTokens(1L);

        var properties = new AuthProperties();
        var encoder = new BCryptPasswordEncoder();
        var users = new UserServiceImpl(userRoles, mock(UserDeptService.class), roles, encoder,
                tokens, permissions, refreshTokens, properties);
        ReflectionTestUtils.setField(users, "baseMapper", mapper);
        var auth = new AuthServiceImpl(users, userRoles, roles, permissions, refreshTokens, tokens,
                encoder, properties, mock(ValkeyUtils.class), mock(ApplicationEventPublisher.class), JsonMapper.builder().build());
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "refresh"));

        var workers = Executors.newFixedThreadPool(2);
        try {
            var changingRoles = workers.submit(() -> transactions.executeWithoutResult(
                    status -> users.assignRoles(1L, Set.of(2L))));
            assertTrue(revoked.await(5, SECONDS), "角色变更未到达令牌失效步骤");
            var refreshing = workers.submit(() -> transactions.execute(
                    status -> auth.refresh(request, new MockHttpServletResponse())));
            assertTrue(refreshAttempted.await(5, SECONDS), "续期请求未开始");

            assertThrows(TimeoutException.class, () -> refreshing.get(200, MILLISECONDS),
                    "角色事务提交前不得签发包含旧角色的令牌");
            allowCommit.countDown();
            changingRoles.get(5, SECONDS);
            refreshing.get(5, SECONDS);

            assertEquals(Set.of("member"), issuedUser.get().roles());
        } finally {
            allowCommit.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, SECONDS));
            jdbc.execute("SHUTDOWN");
        }
    }
}
