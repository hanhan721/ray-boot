package com.ray.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.domain.RolePageQuery;
import com.ray.system.api.entity.SysRole;

import java.util.Set;

/**
 * 角色 Service
 */
public interface RoleService extends IService<SysRole> {

    /**
     * 分页查询角色
     *
     * @param query 分页参数
     * @return 分页结果
     */
    IPage<SysRole> page(RolePageQuery query);

    /**
     * 根据 ID 查询角色
     *
     * @param id 角色 ID
     * @return 角色信息
     */
    SysRole getById(Long id);

    /**
     * 新增角色
     *
     * @param role 角色信息
     */
    void add(SysRole role);

    /**
     * 修改角色
     *
     * @param role 角色信息
     */
    void update(SysRole role);

    /**
     * 删除角色
     *
     * @param ids 角色 ID 集合
     */
    void delete(Set<Long> ids);

    /**
     * 设置角色启用状态
     *
     * @param id      角色 ID
     * @param enabled 是否启用
     */
    void setEnabled(Long id, boolean enabled);
}
