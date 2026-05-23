package com.ray.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.system.api.domain.RolePageQuery;
import com.ray.system.api.entity.SysRole;
import com.ray.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 角色管理
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/sys/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public R<IPage<SysRole>> page(@Valid RolePageQuery query) {
        return R.ok(roleService.page(query));
    }

    @Operation(summary = "查询列表")
    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.ok(roleService.list());
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysRole role) {
        roleService.add(role);
        return R.ok();
    }

    @Operation(summary = "修改角色")
    @PutMapping
    public R<Void> update(@Valid @RequestBody SysRole role) {
        roleService.update(role);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping
    public R<Void> delete(@RequestBody Set<Long> ids) {
        roleService.delete(ids);
        return R.ok();
    }

    @Operation(summary = "设置角色状态")
    @PutMapping("/{id}/status")
    public R<Void> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        roleService.setEnabled(id, enabled);
        return R.ok();
    }
}
