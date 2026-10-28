package com.ray.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.system.api.domain.UserDetailVO;
import com.ray.system.api.domain.UserPageQuery;
import com.ray.system.api.entity.SysUser;
import com.ray.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 用户管理
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/sys/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public R<IPage<SysUser>> page(@Valid UserPageQuery query) {
        return R.ok(userService.page(query));
    }

    @Operation(summary = "查询详情（含角色、部门 ID）")
    @GetMapping("/{id}")
    public R<UserDetailVO> getById(@PathVariable Long id) {
        return R.ok(userService.getUserDetail(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("@perm.has('user:add')")
    public R<Void> add(@Valid @RequestBody SysUser user) {
        userService.add(user);
        return R.ok();
    }

    @Operation(summary = "修改用户")
    @PutMapping
    public R<Void> update(@Valid @RequestBody SysUser user) {
        userService.update(user);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping
    public R<Void> delete(@RequestBody Set<Long> ids) {
        userService.delete(ids);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.password());
        return R.ok();
    }
    record ResetPasswordRequest(@NotBlank(message = "密码不能为空")
                                @Size(max = 72, message = "密码长度不能超过72个字符")
                                String password) {}

    @Operation(summary = "分配角色")
    @PutMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody Set<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return R.ok();
    }

    @Operation(summary = "查询用户角色 ID 列表")
    @GetMapping("/{id}/roles")
    public R<Set<Long>> getRoleIds(@PathVariable Long id) {
        return R.ok(userService.getRoleIds(id));
    }

    @Operation(summary = "分配部门")
    @PutMapping("/{id}/depts")
    public R<Void> assignDepts(@PathVariable Long id, @RequestBody Set<Long> deptIds) {
        userService.assignDepts(id, deptIds);
        return R.ok();
    }

    @Operation(summary = "查询用户部门 ID 列表")
    @GetMapping("/{id}/depts")
    public R<Set<Long>> getDeptIds(@PathVariable Long id) {
        return R.ok(userService.getDeptIds(id));
    }

    @Operation(summary = "设置用户状态")
    @PutMapping("/{id}/status")
    public R<Void> setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        userService.setEnabled(id, enabled);
        return R.ok();
    }
}
