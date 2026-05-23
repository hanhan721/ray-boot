package com.ray.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.entity.SysRolePermission;

import java.util.Collection;
import java.util.Set;

/**
 * 角色权限关联 Service
 */
public interface RolePermissionService extends IService<SysRolePermission> {

    /**
     * 根据角色 ID 查询权限 ID 列表
     *
     * @param roleId 角色 ID
     * @return 权限 ID 集合
     */
    Set<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 根据权限 ID 查询角色 ID 列表
     *
     * @param permissionId 权限 ID
     * @return 角色 ID 集合
     */
    Set<Long> getRoleIdsByPermissionId(Long permissionId);

    /**
     * 删除角色的所有权限关联
     *
     * @param roleId 角色 ID
     */
    void deleteByRoleId(Long roleId);

    /**
     * 删除角色的所有权限关联（批量）
     *
     * @param roleIds 角色 ID 集合
     */
    void deleteByRoleIds(Collection<Long> roleIds);

    /**
     * 删除权限的所有角色关联
     *
     * @param permissionId 权限 ID
     */
    void deleteByPermissionId(Long permissionId);

    /**
     * 删除权限的所有角色关联（批量）
     *
     * @param permissionIds 权限 ID 集合
     */
    void deleteByPermissionIds(Collection<Long> permissionIds);

    /**
     * 保存角色权限关联
     *
     * @param roleId        角色 ID
     * @param permissionIds 权限 ID 集合
     */
    void saveRolePermissions(Long roleId, Set<Long> permissionIds);
}
