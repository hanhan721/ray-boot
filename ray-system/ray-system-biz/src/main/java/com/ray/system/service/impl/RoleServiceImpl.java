package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.system.api.domain.RolePageQuery;
import com.ray.common.core.util.AssertUtils;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.common.security.config.AuthProperties;
import com.ray.system.api.entity.SysRole;
import com.ray.system.mapper.RoleMapper;
import com.ray.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 角色 Service 实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, SysRole> implements RoleService {

    private final AuthProperties authProperties;

    @Override
    public IPage<SysRole> page(RolePageQuery query) {
        return super.page(PageUtils.buildPage(query), QueryWrapperHelper.build(query));
    }

    @Override
    public SysRole getById(Long id) {
        return super.getById(id);
    }

    @Override
    public void add(SysRole role) {
        this.checkRoleKeyUnique(role.getRoleKey(), null);
        super.save(role);
    }

    @Override
    public void update(SysRole role) {
        AssertUtils.notNull(role.getId(), "角色 ID 不能为空");
        this.checkRoleKeyUnique(role.getRoleKey(), role.getId());
        super.updateById(role);
    }

    @Override
    public void delete(Set<Long> ids) {
        super.removeByIds(ids);
    }

    @Override
    public void setEnabled(Long id, boolean enabled) {
        this.checkNotSuperAdminRole(id);
        var role = new SysRole();
        role.setId(id);
        role.setEnabled(enabled);
        super.updateById(role);
    }

    /**
     * 校验角色不是超管角色
     */
    private void checkNotSuperAdminRole(Long id) {
        var role = super.getById(id);
        AssertUtils.notNull(role, "角色不存在");
        var superAdminCodes = authProperties.getSuperAdmin().getRoleCodes();
        AssertUtils.isFalse(superAdminCodes.contains(role.getRoleKey()), "超管角色不允许修改状态");
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
