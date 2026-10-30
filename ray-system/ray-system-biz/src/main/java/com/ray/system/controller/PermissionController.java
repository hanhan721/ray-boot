package com.ray.system.controller;

import com.ray.common.core.domain.R;
import com.ray.system.api.entity.SysPermission;
import com.ray.system.service.PermissionService;
import com.ray.system.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 权限管理
 */
@Tag(name = "权限管理")
@Validated
@RestController
@RequestMapping("/api/sys/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;
    private final RolePermissionService rolePermissionService;

    @Operation(summary = "获取权限列表")
    @GetMapping("/list")
    @PreAuthorize("@perm.has('system:permission:list')")
    public R<List<SysPermission>> list() {
        return R.ok(permissionService.listAll());
    }

    @Operation(summary = "获取树形权限列表")
    @GetMapping("/tree")
    @PreAuthorize("@perm.has('system:permission:list')")
    public R<List<SysPermission>> tree() {
        return R.ok(permissionService.listTree());
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:permission:query')")
    public R<SysPermission> getById(@PathVariable Long id) {
        return R.ok(permissionService.getById(id));
    }

    @Operation(summary = "新增权限")
    @PostMapping
    @PreAuthorize("@perm.has('system:permission:add')")
    public R<Void> add(@Valid @RequestBody SysPermission permission) {
        permissionService.add(permission);
        return R.ok();
    }

    @Operation(summary = "修改权限")
    @PutMapping
    @PreAuthorize("@perm.has('system:permission:edit')")
    public R<Void> update(@Valid @RequestBody SysPermission permission) {
        permissionService.update(permission);
        return R.ok();
    }

    @Operation(summary = "删除权限")
    @DeleteMapping
    @PreAuthorize("@perm.has('system:permission:remove')")
    public R<Void> delete(@RequestBody @NotEmpty(message = "ID 集合不能为空") Set<Long> ids) {
        permissionService.delete(ids);
        return R.ok();
    }

    @Operation(summary = "查询角色权限ID列表")
    @GetMapping("/role/{roleId}")
    @PreAuthorize("@perm.has('system:role:query')")
    public R<Set<Long>> getPermissionIdsByRoleId(@PathVariable Long roleId) {
        return R.ok(rolePermissionService.getPermissionIdsByRoleId(roleId));
    }

    @Operation(summary = "保存角色权限")
    @PostMapping("/role/{roleId}")
    @PreAuthorize("@perm.has('system:role:permission')")
    public R<Void> saveRolePermissions(@PathVariable Long roleId, @RequestBody Set<Long> permissionIds) {
        rolePermissionService.saveRolePermissions(roleId, permissionIds);
        return R.ok();
    }

    @Operation(summary = "清理权限树缓存")
    @DeleteMapping("/cache")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.has('system:permission:cache')")
    public R<Void> clearCache() {
        permissionService.clearTreeCache();
        return R.ok();
    }
}
