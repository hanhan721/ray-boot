package com.ray.system.controller;

import com.ray.common.core.domain.R;
import com.ray.system.api.entity.SysDept;
import com.ray.system.service.DeptService;
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
 * 部门管理
 */
@Tag(name = "部门管理")
@Validated
@RestController
@RequestMapping("/api/sys/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "获取部门列表")
    @GetMapping("/list")
    @PreAuthorize("@perm.has('system:dept:list')")
    public R<List<SysDept>> list() {
        return R.ok(deptService.listAll());
    }

    @Operation(summary = "获取树形部门列表")
    @GetMapping("/tree")
    @PreAuthorize("@perm.has('system:dept:list')")
    public R<List<SysDept>> tree() {
        return R.ok(deptService.listTree());
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:dept:query')")
    public R<SysDept> getById(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    @PreAuthorize("@perm.has('system:dept:add')")
    public R<Void> add(@Valid @RequestBody SysDept dept) {
        deptService.add(dept);
        return R.ok();
    }

    @Operation(summary = "修改部门")
    @PutMapping
    @PreAuthorize("@perm.has('system:dept:edit')")
    public R<Void> update(@Valid @RequestBody SysDept dept) {
        deptService.update(dept);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping
    @PreAuthorize("@perm.has('system:dept:remove')")
    public R<Void> delete(@RequestBody @NotEmpty(message = "ID 集合不能为空") Set<Long> ids) {
        deptService.delete(ids);
        return R.ok();
    }

    @Operation(summary = "清理部门树缓存")
    @DeleteMapping("/cache")
    @org.springframework.security.access.prepost.PreAuthorize("@perm.has('system:dept:cache')")
    public R<Void> clearCache() {
        deptService.clearTreeCache();
        return R.ok();
    }

}
