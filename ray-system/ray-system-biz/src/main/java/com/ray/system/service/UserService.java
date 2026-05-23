package com.ray.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.domain.UserDetailVO;
import com.ray.system.api.domain.UserPageQuery;
import com.ray.system.api.entity.SysUser;

import java.util.Set;

/**
 * 用户 Service
 */
public interface UserService extends IService<SysUser> {

    /**
     * 分页查询用户
     *
     * @param query 分页参数
     * @return 分页结果
     */
    IPage<SysUser> page(UserPageQuery query);

    /**
     * 根据 ID 查询用户（含关联角色、部门 ID）
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    UserDetailVO getUserDetail(Long id);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    SysUser getById(Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUser getByUsername(String username);

    /**
     * 新增用户
     *
     * @param user 用户信息
     */
    void add(SysUser user);

    /**
     * 修改用户
     *
     * @param user 用户信息
     */
    void update(SysUser user);

    /**
     * 删除用户
     *
     * @param ids 用户 ID 集合
     */
    void delete(Set<Long> ids);

    /**
     * 重置密码
     *
     * @param id       用户 ID
     * @param password 新密码
     */
    void resetPassword(Long id, String password);

    /**
     * 分配角色
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 集合
     */
    void assignRoles(Long userId, Set<Long> roleIds);

    /**
     * 查询用户角色 ID 列表
     *
     * @param userId 用户 ID
     * @return 角色 ID 集合
     */
    Set<Long> getRoleIds(Long userId);

    /**
     * 分配部门
     *
     * @param userId  用户 ID
     * @param deptIds 部门 ID 集合（传空集合则清空所有部门关联）
     */
    void assignDepts(Long userId, Set<Long> deptIds);

    /**
     * 查询用户部门 ID 列表
     *
     * @param userId 用户 ID
     * @return 部门 ID 集合
     */
    Set<Long> getDeptIds(Long userId);

    /**
     * 设置用户启用状态
     *
     * @param id      用户 ID
     * @param enabled 是否启用
     */
    void setEnabled(Long id, boolean enabled);
}
