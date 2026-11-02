package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.core.util.AssertUtils;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.system.api.domain.RolePageQuery;
import com.ray.system.api.entity.SysRole;
import com.ray.system.api.entity.SysUserRole;
import com.ray.system.mapper.RoleMapper;
import com.ray.system.security.AuthorizationSessionCoordinator;
import com.ray.system.security.SuperAdminGuard;
import com.ray.system.service.RolePermissionService;
import com.ray.system.service.RoleService;
import com.ray.system.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * 角色 Service 实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, SysRole> implements RoleService {

    private final SuperAdminGuard superAdminGuard;
    private final AuthorizationSessionCoordinator sessionCoordinator;
    private final UserRoleService userRoleService;
    private final RolePermissionService rolePermissionService;

    @Override
    public IPage<SysRole> page(RolePageQuery query) {
        return super.page(PageUtils.buildPage(query), QueryWrapperHelper.build(query));
    }

    @Override
    public SysRole getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SysRole role) {
        sessionCoordinator.lockForChange();
        superAdminGuard.checkRoleKey(role.getRoleKey());
        this.checkRoleKeyUnique(role.getRoleKey(), null);
        super.save(role);
        sessionCoordinator.revokeRoleUsers(Set.of(role.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysRole role) {
        var authorizationWrite = role.getRoleKey() != null || role.getEnabled() != null;
        if (authorizationWrite) {
            sessionCoordinator.lockForChange();
        }
        AssertUtils.notNull(role.getId(), "角色 ID 不能为空");
        superAdminGuard.checkRoles(Set.of(role.getId()));
        superAdminGuard.checkRoleKey(role.getRoleKey());
        this.checkRoleKeyUnique(role.getRoleKey(), role.getId());
        if (authorizationWrite) {
            sessionCoordinator.revokeRoleUsers(Set.of(role.getId()));
        }
        super.updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        sessionCoordinator.lockForChange();
        superAdminGuard.checkRoles(ids);
        sessionCoordinator.revokeRoleUsers(ids);
        userRoleService.remove(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, ids));
        rolePermissionService.deleteByRoleIds(ids);
        super.removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(Long id, boolean enabled) {
        sessionCoordinator.lockForChange();
        AssertUtils.notNull(id, "角色 ID 不能为空");
        var existingRole = super.getById(id);
        AssertUtils.notNull(existingRole, "角色不存在");
        superAdminGuard.checkRoleKey(existingRole.getRoleKey());
        sessionCoordinator.revokeRoleUsers(Set.of(id));
        var role = new SysRole();
        role.setId(id);
        role.setEnabled(enabled);
        super.updateById(role);
    }

    /**
     * 校验角色标识唯一性
     */
    private void checkRoleKeyUnique(String roleKey, Long excludeId) {
        if (roleKey == null || roleKey.isBlank()) {
            return;
        }
        var wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeId != null, SysRole::getId, excludeId);
        AssertUtils.isFalse(super.exists(wrapper), "角色标识已存在");
    }
}
