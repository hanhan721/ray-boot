package com.ray.system.api.domain;

import com.ray.system.api.entity.SysPermission;

import java.util.List;
import java.util.Set;

/**
 * 用户权限查询结果（菜单树 + 权限编码）
 *
 * @param menus       菜单树（前端动态路由）
 * @param permissions 权限编码集合（接口权限校验）
 */
public record UserPermissionDTO(
        List<SysPermission> menus,
        Set<String> permissions
) {
}
