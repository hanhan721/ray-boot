package com.ray.system.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.service.TokenService;
import com.ray.system.api.entity.RefreshToken;
import com.ray.system.api.entity.SysRole;
import com.ray.system.api.entity.SysRolePermission;
import com.ray.system.api.entity.SysUserRole;
import com.ray.system.mapper.AuthorizationLockMapper;
import com.ray.system.mapper.RoleMapper;
import com.ray.system.mapper.UserMapper;
import com.ray.system.mapper.UserRoleMapper;
import com.ray.system.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** 协调数据库变更与会话签发；提交后只撤销事务内捕获的 Access Token。 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AuthorizationSessionCoordinator {
    private final AuthorizationLockMapper locks;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RefreshTokenService refreshTokenService;
    // 延迟取得 TokenService，避免授权加载服务与业务服务形成构造器循环。
    private final ObjectProvider<TokenService> tokenServices;
    private final AuthProperties authProperties;

    public void lockForIssuance() {
        locks.lockForIssuance();
    }

    public void lockForChange() {
        locks.lockForChange();
    }

    public void revokeRoleUsers(Collection<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        this.revokeUsers(userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .in(SysUserRole::getRoleId, roleIds)).stream()
                .map(SysUserRole::getUserId).collect(Collectors.toSet()));
    }

    public void revokePermissionUsers(Collection<Long> permissionIds, boolean includeSuperAdmins) {
        Set<Long> userIds = new TreeSet<>();
        if (!permissionIds.isEmpty()) {
            var query = new MPJLambdaWrapper<SysUserRole>()
                    .distinct().select(SysUserRole::getUserId)
                    .innerJoin(SysRolePermission.class, SysRolePermission::getRoleId, SysUserRole::getRoleId)
                    .in(SysRolePermission::getPermissionId, permissionIds);
            userRoleMapper.selectJoinList(SysUserRole.class, query).forEach(row -> userIds.add(row.getUserId()));
        }
        var superCodes = authProperties.getSuperAdmin().getRoleCodes();
        if (includeSuperAdmins && !superCodes.isEmpty()) {
            var roleIds = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                            .in(SysRole::getRoleKey, superCodes).eq(SysRole::getEnabled, true)).stream()
                    .map(SysRole::getId).toList();
            if (!roleIds.isEmpty()) {
                userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getRoleId, roleIds))
                        .forEach(row -> userIds.add(row.getUserId()));
            }
        }
        this.revokeUsers(userIds);
    }

    public void revokeUsers(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        // 重入独占锁安全；入口必须在读取影响范围/取得任何用户行锁前先调用 lockForChange。
        this.lockForChange();
        var sortedIds = new TreeSet<>(userIds);
        sortedIds.forEach(userMapper::selectByIdForUpdate);
        var tokens = tokenServices.getObject();
        var captured = new TreeMap<>(tokens.captureUserSessions(sortedIds));
        // 数据库凭据是持久化来源；Valkey 索引只作为快速路径，缓存淘汰时仍须能枚举全部会话。
        refreshTokenService.findSessionIdsByUserIds(sortedIds).forEach((userId, sessionIds) -> {
            var sessions = new TreeSet<>(captured.getOrDefault(userId, Set.of()));
            sessions.addAll(sessionIds);
            captured.put(userId, sessions);
        });
        refreshTokenService.remove(Wrappers.<RefreshToken>lambdaQuery().in(RefreshToken::getUserId, sortedIds));
        tokens.revokeAfterCommit(captured);
    }
}
