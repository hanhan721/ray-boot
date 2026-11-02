package com.ray.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.domain.UserPermissionDTO;
import com.ray.system.api.entity.SysPermission;

import java.util.List;
import java.util.Set;

/**
 * 权限 Service
 */
public interface PermissionService extends IService<SysPermission> {

    /**
     * 获取权限列表（按排序）
     *
     * @return 权限列表
     */
    List<SysPermission> listAll();

    /**
     * 获取树形权限列表
     *
     * @return 树形权限列表
     */
    List<SysPermission> listTree();

    /**
     * 根据 ID 查询权限
     *
     * @param id 权限 ID
     * @return 权限信息
     */
    SysPermission getById(Long id);

    /**
     * 新增权限
     *
     * @param permission 权限信息
     */
    void add(SysPermission permission);

    /**
     * 修改权限
     *
     * @param permission 权限信息
     */
    void update(SysPermission permission);

    /**
     * 删除权限
     *
     * @param ids 权限 ID 集合
     */
    void delete(Set<Long> ids);

    /**
     * 根据用户 ID 一次性查询菜单和权限（一次数据库查询，Java 中分离）
     *
     * @param userId 用户 ID
     * @return 包含菜单树和权限编码的结果
     */
    UserPermissionDTO getUserPermissions(Long userId);

    /**
     * 一次性查询所有权限，分离为菜单树和权限编码（超管使用）
     * 只查询启用的权限
     *
     * @return 包含菜单树和权限编码的结果
     */
    UserPermissionDTO getAllPermissionsDTO();

    /**
     * 根据用户 ID 查询权限编码列表
     *
     * @param userId 用户 ID
     * @return 权限编码集合
     */
    Set<String> getCodesByUserId(Long userId);

    /**
     * 清理权限树缓存
     */
    void clearTreeCache();

    /** 查询全部有效权限码（超管）。 */
    Set<String> getAllCodes();
}
