package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.system.api.entity.SysRolePermission;
import com.ray.system.mapper.RolePermissionMapper;
import com.ray.system.security.AuthorizationSessionCoordinator;
import com.ray.system.security.SuperAdminGuard;
import com.ray.system.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色权限关联 Service 实现
 */
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, SysRolePermission> implements RolePermissionService {

    private final SuperAdminGuard superAdminGuard;
    private final AuthorizationSessionCoordinator sessionCoordinator;

    @Override
    public Set<Long> getPermissionIdsByRoleId(Long roleId) {
        var wrapper = Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId)
                .select(SysRolePermission::getPermissionId);
        return super.list(wrapper).stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getRoleIdsByPermissionId(Long permissionId) {
        var wrapper = Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getPermissionId, permissionId)
                .select(SysRolePermission::getRoleId);
        return super.list(wrapper).stream()
                .map(SysRolePermission::getRoleId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByRoleId(Long roleId) {
        var wrapper = Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getRoleId, roleId);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByRoleIds(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        var wrapper = Wrappers.<SysRolePermission>lambdaQuery()
                .in(SysRolePermission::getRoleId, roleIds);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByPermissionId(Long permissionId) {
        var wrapper = Wrappers.<SysRolePermission>lambdaQuery()
                .eq(SysRolePermission::getPermissionId, permissionId);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByPermissionIds(Collection<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        var wrapper = Wrappers.<SysRolePermission>lambdaQuery()
                .in(SysRolePermission::getPermissionId, permissionIds);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(Long roleId, Set<Long> permissionIds) {
        sessionCoordinator.lockForChange();
        superAdminGuard.checkRoles(Set.of(roleId));
        sessionCoordinator.revokeRoleUsers(Set.of(roleId));
        // 先删除原有关联
        this.deleteByRoleId(roleId);

        // 批量插入新关联
        if (!CollectionUtils.isEmpty(permissionIds)) {
            List<SysRolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> new SysRolePermission()
                            .setRoleId(roleId)
                            .setPermissionId(permissionId))
                    .toList();
            super.saveBatch(rolePermissions);
        }
    }
}
