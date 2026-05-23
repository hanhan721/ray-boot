package com.ray.system.controller;

import com.ray.common.core.domain.R;
import com.ray.system.api.vo.ColumnItem;
import com.ray.system.service.UserColumnConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户列配置
 */
@Tag(name = "用户列配置")
@Validated
@RestController
@RequestMapping("/api/sys/user-column-config")
@RequiredArgsConstructor
public class UserColumnConfigController {

    private final UserColumnConfigService userColumnConfigService;

    @Operation(summary = "获取列配置")
    @GetMapping("/{pageKey}")
    public R<List<ColumnItem>> get(
            @Parameter(description = "页面标识")
            @PathVariable String pageKey) {
        return R.ok(userColumnConfigService.getByPageKey(pageKey));
    }

    @Operation(summary = "保存列配置")
    @PutMapping("/{pageKey}")
    public R<Void> save(
            @Parameter(description = "页面标识")
            @PathVariable String pageKey,
            @Valid @NotEmpty(message = "列配置不能为空")
            @RequestBody List<ColumnItem> columns) {
        userColumnConfigService.saveByPageKey(pageKey, columns);
        return R.ok();
    }

    @Operation(summary = "恢复默认列配置")
    @DeleteMapping("/{pageKey}")
    public R<Void> delete(
            @Parameter(description = "页面标识")
            @PathVariable String pageKey) {
        userColumnConfigService.deleteByPageKey(pageKey);
        return R.ok();
    }
}
