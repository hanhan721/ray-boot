package com.ray.common.security.evaluator;

import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.service.TokenService;
import com.ray.common.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * 自定义权限校验器
 * <p>
 * 使用方式: {@code @PreAuthorize("@perm.has('system:user:add')")}
 * <p>
 * 支持多权限校验: {@code @PreAuthorize("@perm.hasAny('system:user:add', 'system:user:edit')")}
 * <p>
 * 超管自动拥有所有权限
 */
@Component("perm")
@RequiredArgsConstructor
public class PermissionEvaluator {

    private final TokenService tokenService;
    private final AuthProperties authProperties;

    /**
     * 检查当前用户是否为超管
     *
     * @return 是否为超管
     */
    private boolean isSuperAdmin() {
        var roles = SecurityUtils.getRoles();
        if (CollectionUtils.isEmpty(roles)) {
            return false;
        }
        var superAdminRoleCodes = authProperties.getSuperAdmin().getRoleCodes();
        if (CollectionUtils.isEmpty(superAdminRoleCodes)) {
            return false;
        }
        // 检查用户角色是否包含任意超管角色
        return roles.stream().anyMatch(superAdminRoleCodes::contains);
    }

    /**
     * 校验当前用户是否拥有指定权限
     *
     * @param permission 权限编码
     * @return 是否拥有
     */
    public boolean has(String permission) {
        // 超管拥有所有权限
        if (isSuperAdmin()) {
            return true;
        }

        var userId = SecurityUtils.getUserId();
        if (userId == null) {
            return false;
        }

        var permissions = tokenService.getPermissions(userId);
        return permissions.contains(permission);
    }

    /**
     * 校验当前用户是否拥有任意一个权限
     *
     * @param permissions 权限编码列表
     * @return 是否拥有任意一个
     */
    public boolean hasAny(String... permissions) {
        // 超管拥有所有权限
        if (isSuperAdmin()) {
            return true;
        }

        var userId = SecurityUtils.getUserId();
        if (userId == null) {
            return false;
        }

        var userPermissions = tokenService.getPermissions(userId);
        if (CollectionUtils.isEmpty(userPermissions)) {
            return false;
        }

        for (var perm : permissions) {
            if (userPermissions.contains(perm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验当前用户是否拥有所有权限
     *
     * @param permissions 权限编码列表
     * @return 是否全部拥有
     */
    public boolean hasAll(String... permissions) {
        // 超管拥有所有权限
        if (isSuperAdmin()) {
            return true;
        }

        var userId = SecurityUtils.getUserId();
        if (userId == null) {
            return false;
        }

        var userPermissions = tokenService.getPermissions(userId);
        if (CollectionUtils.isEmpty(userPermissions)) {
            return false;
        }

        return userPermissions.containsAll(Set.of(permissions));
    }
}
