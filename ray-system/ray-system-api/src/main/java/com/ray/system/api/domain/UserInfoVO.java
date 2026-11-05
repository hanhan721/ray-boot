package com.ray.system.api.domain;

import com.ray.common.storage.annotation.AccessLevel;
import com.ray.common.storage.annotation.StorageFile;
import com.ray.system.api.entity.SysPermission;

import java.util.List;
import java.util.Set;

/**
 * 当前用户信息响应
 *
 * @param userId      用户 ID
 * @param username    用户名
 * @param nickname    昵称
 * @param avatarFileId 头像文件 ID
 * @param roles       角色标识集合
 * @param menus       菜单权限树（前端动态路由）
 * @param permissions 权限编码集合（接口权限校验）
 */
public record UserInfoVO(
        Long userId,
        String username,
        String nickname,
        @StorageFile(level = AccessLevel.PROTECTED)
        Long avatarFileId,
        Set<String> roles,
        List<SysPermission> menus,
        Set<String> permissions
) {
}
