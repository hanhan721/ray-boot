package com.ray.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.system.api.domain.OperLogPageQuery;
import com.ray.system.api.entity.SysOperLog;
import com.ray.system.service.OperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志管理
 */
@Tag(name = "操作日志管理")
@RestController
@RequestMapping("/api/sys/oper-log")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @PreAuthorize("@perm.has('system:oper-log:list')")
    public R<IPage<SysOperLog>> page(@Valid OperLogPageQuery query) {
        return R.ok(operLogService.page(query));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:oper-log:query')")
    public R<SysOperLog> getById(@PathVariable Long id) {
        return R.ok(operLogService.getById(id));
    }

    @Operation(summary = "清空操作日志")
    @DeleteMapping("/clean")
    @PreAuthorize("@perm.has('system:oper-log:clean')")
    public R<Void> clean() {
        operLogService.clean();
        return R.ok();
    }
}
