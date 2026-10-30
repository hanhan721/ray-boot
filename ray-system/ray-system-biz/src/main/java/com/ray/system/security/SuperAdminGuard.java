package com.ray.system.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ray.common.core.constant.ResultCode;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.security.config.AuthProperties;
import com.ray.common.security.util.SecurityUtils;
import com.ray.system.api.entity.SysRole;
import com.ray.system.api.entity.SysUserRole;
import com.ray.system.mapper.RoleMapper;
import com.ray.system.mapper.UserMapper;
import com.ray.system.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * 超管对象的管理边界；独立于接口权限，身份以数据库当前角色为准。
 */
@Component
@RequiredArgsConstructor
public class SuperAdminGuard {

    private final AuthProperties authProperties;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public void checkUsers(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds) || authProperties.getSuperAdmin().getRoleCodes().isEmpty()) {
            return;
        }
        this.checkRoles(this.getRoleIds(userIds));
    }

    public void checkRoles(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds) || authProperties.getSuperAdmin().getRoleCodes().isEmpty()) {
            return;
        }
        // 停用的超管角色也属于受保护对象，不能由普通账号重新启用或分配。
        var hasSuperRole = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .in(SysRole::getId, roleIds)).stream()
                .anyMatch(role -> this.isSuperRoleKey(role.getRoleKey()));
        this.checkOperator(hasSuperRole);
    }

    public void checkRoleKey(String roleKey) {
        this.checkOperator(this.isSuperRoleKey(roleKey));
    }

    private boolean isSuperRoleKey(String roleKey) {
        return roleKey != null && authProperties.getSuperAdmin().getRoleCodes().contains(roleKey);
    }

    private List<Long> getRoleIds(Collection<Long> userIds) {
        return userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .in(SysUserRole::getUserId, userIds)
                        .select(SysUserRole::getRoleId)).stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
    }

    private void checkOperator(boolean protectedObject) {
        if (protectedObject && !this.isCurrentSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超管可管理超管账户或超管角色");
        }
    }

    private boolean isCurrentSuperAdmin() {
        var userId = SecurityUtils.getUserId();
        if (userId == null) {
            return false;
        }
        var user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            return false;
        }
        var roleIds = this.getRoleIds(List.of(userId));
        return !roleIds.isEmpty() && roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getEnabled, true)).stream()
                .anyMatch(role -> this.isSuperRoleKey(role.getRoleKey()));
    }
}
