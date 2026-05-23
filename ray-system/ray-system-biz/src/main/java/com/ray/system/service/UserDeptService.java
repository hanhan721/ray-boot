package com.ray.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.entity.SysUserDept;

import java.util.Collection;
import java.util.Set;

/**
 * 用户部门关联 Service
 */
public interface UserDeptService extends IService<SysUserDept> {

    /**
     * 根据用户 ID 查询部门 ID 列表
     *
     * @param userId 用户 ID
     * @return 部门 ID 集合
     */
    Set<Long> getDeptIdsByUserId(Long userId);

    /**
     * 根据部门 ID 查询用户 ID 列表
     *
     * @param deptId 部门 ID
     * @return 用户 ID 集合
     */
    Set<Long> getUserIdsByDeptId(Long deptId);

    /**
     * 删除用户的所有部门关联
     *
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);

    /**
     * 删除用户的所有部门关联（批量）
     *
     * @param userIds 用户 ID 集合
     */
    void deleteByUserIds(Collection<Long> userIds);

    /**
     * 删除部门的所有用户关联
     *
     * @param deptId 部门 ID
     */
    void deleteByDeptId(Long deptId);

    /**
     * 删除部门的所有用户关联（批量）
     *
     * @param deptIds 部门 ID 集合
     */
    void deleteByDeptIds(Collection<Long> deptIds);

    /**
     * 保存用户部门关联
     *
     * @param userId  用户 ID
     * @param deptIds 部门 ID 集合
     */
    void saveUserDepts(Long userId, Set<Long> deptIds);

    /**
     * 检查部门是否存在用户关联
     *
     * @param deptIds 部门 ID 集合
     * @return 是否存在关联
     */
    boolean existsByDeptIds(Collection<Long> deptIds);
}
