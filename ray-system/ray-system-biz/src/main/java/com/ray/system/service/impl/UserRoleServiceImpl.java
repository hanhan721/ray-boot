package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.system.api.entity.SysUserRole;
import com.ray.system.mapper.UserRoleMapper;
import com.ray.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户角色关联 Service 实现
 */
@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, SysUserRole> implements UserRoleService {

    @Override
    public Set<Long> getRoleIdsByUserId(Long userId) {
        var wrapper = Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .select(SysUserRole::getRoleId);
        return super.list(wrapper).stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getUserIdsByRoleId(Long roleId) {
        var wrapper = Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, roleId)
                .select(SysUserRole::getUserId);
        return super.list(wrapper).stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserId(Long userId) {
        var wrapper = Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        var wrapper = Wrappers.<SysUserRole>lambdaQuery()
                .in(SysUserRole::getUserId, userIds);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByRoleId(Long roleId) {
        var wrapper = Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, roleId);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserRoles(Long userId, Set<Long> roleIds) {
        // 先删除原有关联
        this.deleteByUserId(userId);

        // 批量插入新关联
        if (roleIds != null && !roleIds.isEmpty()) {
            List<SysUserRole> userRoles = roleIds.stream()
                    .map(roleId -> new SysUserRole()
                            .setUserId(userId)
                            .setRoleId(roleId))
                    .toList();
            super.saveBatch(userRoles);
        }
    }
}
