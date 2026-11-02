package com.ray.system.security;

import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.domain.UserAuthorization;
import com.ray.common.security.service.UserAuthorizationProvider;
import com.ray.system.api.entity.SysRole;
import com.ray.system.service.PermissionService;
import com.ray.system.service.RoleService;
import com.ray.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/** 仅供登录和刷新在协调共享锁内加载授权。 */
@Service
@RequiredArgsConstructor
public class UserAuthorizationProviderImpl implements UserAuthorizationProvider {
    private final UserRoleService userRoleService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuthProperties authProperties;

    @Override
    @Transactional(readOnly = true)
    public UserAuthorization getAuthorization(Long userId) {
        var roleIds = userRoleService.getRoleIdsByUserId(userId);
        Set<String> roles = roleIds.isEmpty() ? Set.of() : roleService.listByIds(roleIds).stream()
                .filter(role -> Boolean.TRUE.equals(role.getEnabled()))
                .map(SysRole::getRoleKey)
                .collect(Collectors.toSet());
        var superAdmin = roles.stream().anyMatch(authProperties.getSuperAdmin().getRoleCodes()::contains);
        var permissions = superAdmin ? permissionService.getAllCodes() : permissionService.getCodesByUserId(userId);
        return new UserAuthorization(roles, permissions);
    }
}
