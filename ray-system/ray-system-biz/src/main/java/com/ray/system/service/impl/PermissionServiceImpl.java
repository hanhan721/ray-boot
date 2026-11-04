package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ray.common.core.domain.TreeNode;
import com.ray.common.core.util.AssertUtils;
import com.ray.common.core.util.TreeUtils;
import com.ray.system.api.domain.UserPermissionDTO;
import com.ray.system.api.entity.SysPermission;
import com.ray.system.api.entity.SysRole;
import com.ray.system.api.entity.SysRolePermission;
import com.ray.system.api.entity.SysUserRole;
import com.ray.system.api.enums.PermissionType;
import com.ray.system.constant.SystemCacheConstants;
import com.ray.system.mapper.PermissionMapper;
import com.ray.system.security.AuthorizationSessionCoordinator;
import com.ray.system.service.PermissionService;
import com.ray.system.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限 Service 实现
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, SysPermission> implements PermissionService {

    private final RolePermissionService rolePermissionService;
    private final AuthorizationSessionCoordinator sessionCoordinator;

    @Override
    public List<SysPermission> listAll() {
        return super.list(
                Wrappers.<SysPermission>lambdaQuery()
                        .orderByAsc(SysPermission::getSort)
        );
    }

    @Cacheable(cacheNames = SystemCacheConstants.PERMISSIONS, key = SystemCacheConstants.KEY_PERMISSION_TREE)
    @Override
    public List<SysPermission> listTree() {
        return TreeUtils.buildTree(this.listAll());
    }

    @Override
    public SysPermission getById(Long id) {
        return super.getById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.PERMISSIONS, key = SystemCacheConstants.KEY_PERMISSION_TREE)
    @Override
    public void add(SysPermission permission) {
        sessionCoordinator.lockForChange();
        this.validatePermission(permission);
        this.checkCodeUnique(permission.getCode(), null);
        if (permission.getParentId() == null) {
            permission.setParentId(TreeNode.ROOT_PARENT_ID);
        }
        this.checkParentExists(permission.getParentId());
        super.save(permission);
        // enabled 由数据库 DEFAULT 决定，插入后读取有效值。
        var saved = super.getById(permission.getId());
        AssertUtils.notNull(saved, "新增权限失败");
        sessionCoordinator.revokePermissionUsers(Set.of(permission.getId()),
                Boolean.TRUE.equals(saved.getEnabled()) && StringUtils.hasText(saved.getCode()));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.PERMISSIONS, key = SystemCacheConstants.KEY_PERMISSION_TREE)
    @Override
    public void update(SysPermission permission) {
        var authorizationWrite = permission.getCode() != null || permission.getEnabled() != null;
        if (authorizationWrite) {
            sessionCoordinator.lockForChange();
        }
        AssertUtils.notNull(permission.getId(), "权限 ID 不能为空");
        this.validatePermission(permission);
        this.checkCodeUnique(permission.getCode(), permission.getId());
        if (permission.getParentId() == null) {
            permission.setParentId(TreeNode.ROOT_PARENT_ID);
        }
        this.checkParentExists(permission.getParentId());
        this.checkCyclic(permission.getId(), permission.getParentId());
        if (authorizationWrite) {
            sessionCoordinator.revokePermissionUsers(Set.of(permission.getId()), true);
        }
        super.updateById(permission);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.PERMISSIONS, key = SystemCacheConstants.KEY_PERMISSION_TREE)
    @Override
    public void delete(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        sessionCoordinator.lockForChange();
        sessionCoordinator.revokePermissionUsers(ids, true);
        // 检查是否有子级（排除同时要删除的子级）
        var childCount = super.count(
                Wrappers.<SysPermission>lambdaQuery()
                        .in(SysPermission::getParentId, ids)
                        .notIn(SysPermission::getId, ids)
        );
        AssertUtils.isFalse(childCount > 0, "存在子级权限，无法删除");

        // 删除权限
        super.removeByIds(ids);
        // 删除角色权限关联
        rolePermissionService.deleteByPermissionIds(ids);
    }

    @Override
    public UserPermissionDTO getUserPermissions(Long userId) {
        // 一次性查询用户所有权限（菜单 + 权限代码），再在 Java 中分离
        var wrapper = this.userPermissionsQuery(userId)
                .orderByAsc(SysPermission::getSort);

        var allPermissions = baseMapper.selectJoinList(SysPermission.class, wrapper);
        return this.extractPermissionsDTO(allPermissions);
    }

    private MPJLambdaWrapper<SysPermission> userPermissionsQuery(Long userId) {
        return new MPJLambdaWrapper<SysPermission>()
                .distinct()
                .innerJoin(SysRolePermission.class, SysRolePermission::getPermissionId, SysPermission::getId)
                .innerJoin(SysUserRole.class, SysUserRole::getRoleId, SysRolePermission::getRoleId)
                .innerJoin(SysRole.class, SysRole::getId, SysRolePermission::getRoleId)
                .eq(SysUserRole::getUserId, userId)
                .eq(SysRole::getEnabled, true)
                .eq(SysRole::getDeleted, false)
                .eq(SysPermission::getEnabled, true);
    }

    @Override
    public UserPermissionDTO getAllPermissionsDTO() {
        // 查询所有启用的权限（超管使用），一次查询，Java 中分离
        var allPermissions = super.list(
                Wrappers.<SysPermission>lambdaQuery()
                        .eq(SysPermission::getEnabled, true)
                        .orderByAsc(SysPermission::getSort)
        );
        return this.extractPermissionsDTO(allPermissions);
    }

    /**
     * 从权限列表中提取菜单树和权限编码
     */
    private UserPermissionDTO extractPermissionsDTO(List<SysPermission> allPermissions) {
        // 分离菜单：DIRECTORY 和 MENU 类型
        var menus = allPermissions.stream()
                .filter(p -> p.getType() == PermissionType.DIRECTORY || p.getType() == PermissionType.MENU)
                .collect(Collectors.toList());
        var menuTree = TreeUtils.buildTree(menus);

        // 分离权限编码：所有 code 字段非空的权限
        return new UserPermissionDTO(menuTree, this.extractCodes(allPermissions));
    }

    private Set<String> extractCodes(List<SysPermission> permissions) {
        return permissions.stream()
                .filter(Objects::nonNull)
                .map(SysPermission::getCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getCodesByUserId(Long userId) {
        var wrapper = this.userPermissionsQuery(userId)
                .select(SysPermission::getCode)
                .isNotNull(SysPermission::getCode);
        return this.extractCodes(baseMapper.selectJoinList(SysPermission.class, wrapper));
    }

    @Override
    public Set<String> getAllCodes() {
        return this.extractCodes(super.list(Wrappers.<SysPermission>lambdaQuery()
                .select(SysPermission::getCode)
                .isNotNull(SysPermission::getCode)
                .eq(SysPermission::getEnabled, true)));
    }

    @Override
    @CacheEvict(cacheNames = SystemCacheConstants.PERMISSIONS, key = SystemCacheConstants.KEY_PERMISSION_TREE)
    public void clearTreeCache() {
    }

    /**
     * 校验权限数据合法性
     */
    private void validatePermission(SysPermission permission) {
        switch (permission.getType()) {
            case null -> { }
            case MENU -> AssertUtils.notEmpty(permission.getUrl(), "菜单路由路径不能为空");
            case BUTTON -> AssertUtils.notEmpty(permission.getCode(), "按钮权限标识不能为空");
            default -> { }
        }
    }

    /**
     * 校验权限编码唯一性
     */
    private void checkCodeUnique(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            return;
        }
        var wrapper = Wrappers.<SysPermission>lambdaQuery()
                .eq(SysPermission::getCode, code)
                .ne(excludeId != null, SysPermission::getId, excludeId);
        AssertUtils.isFalse(super.exists(wrapper), "权限编码已存在");
    }

    /**
     * 校验父级是否存在
     */
    private void checkParentExists(Long parentId) {
        if (parentId == null || parentId.equals(TreeNode.ROOT_PARENT_ID)) {
            return;
        }
        AssertUtils.isTrue(super.exists(
                Wrappers.<SysPermission>lambdaQuery().eq(SysPermission::getId, parentId)
        ), "父级权限不存在");
    }

    /**
     * 校验是否存在循环依赖
     */
    private void checkCyclic(Long id, Long parentId) {
        if (parentId == null || parentId.equals(TreeNode.ROOT_PARENT_ID)) {
            return;
        }
        // 不能将父级设置为自己
        AssertUtils.isFalse(id.equals(parentId), "父级不能设置为自己");

        // 一次性查询所有权限的 id -> parentId 映射
        var parentMap = super.list(
                Wrappers.<SysPermission>lambdaQuery()
                        .select(SysPermission::getId, SysPermission::getParentId)
        ).stream().collect(Collectors.toMap(
                SysPermission::getId,
                p -> p.getParentId() != null ? p.getParentId() : TreeNode.ROOT_PARENT_ID
        ));

        Set<Long> visited = new HashSet<>();
        visited.add(id);
        Long current = parentId;
        while (current != null && !current.equals(TreeNode.ROOT_PARENT_ID)) {
            AssertUtils.isFalse(visited.contains(current), "不能形成循环依赖");
            visited.add(current);
            current = parentMap.get(current);
        }
    }
}
