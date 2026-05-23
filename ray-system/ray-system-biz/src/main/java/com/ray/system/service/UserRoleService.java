package com.ray.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.entity.SysUserRole;

import java.util.Collection;
import java.util.Set;

/**
 * 用户角色关联 Service
 */
public interface UserRoleService extends IService<SysUserRole> {

    /**
     * 根据用户 ID 查询角色 ID 列表
     *
     * @param userId 用户 ID
     * @return 角色 ID 集合
     */
    Set<Long> getRoleIdsByUserId(Long userId);

    /**
     * 根据角色 ID 查询用户 ID 列表
     *
     * @param roleId 角色 ID
     * @return 用户 ID 集合
     */
    Set<Long> getUserIdsByRoleId(Long roleId);

    /**
     * 删除用户的所有角色关联
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 删除用户的所有角色关联（批量）
     *
     * @param userIds 用户 ID 集合
     */
    void deleteByUserIds(Collection<Long> userIds);

    /**
     * 删除角色的所有用户关联
     *
     * @param roleId 角色 ID
     */
    void deleteByRoleId(Long roleId);

    /**
     * 批量保存用户角色关联
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 集合
     */
    void saveUserRoles(Long userId, Set<Long> roleIds);
}
