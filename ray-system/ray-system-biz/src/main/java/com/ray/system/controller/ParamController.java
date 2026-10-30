package com.ray.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.system.api.domain.ParamCreateRequest;
import com.ray.system.api.domain.ParamPageQuery;
import com.ray.system.api.domain.ParamUpdateRequest;
import com.ray.system.api.domain.ParamVO;
import com.ray.system.service.ParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 系统参数管理
 */
@Tag(name = "系统参数管理")
@Validated
@RestController
@RequestMapping("/api/sys/param")
@RequiredArgsConstructor
public class ParamController {

    private final ParamService sysParamService;

    @Operation(summary = "创建参数")
    @PostMapping
    @PreAuthorize("@perm.has('system:param:add')")
    public R<Long> create(@Valid @RequestBody ParamCreateRequest request) {
        Long id = sysParamService.create(request);
        return R.ok(id, "参数创建成功");
    }

    @Operation(summary = "更新参数")
    @PutMapping
    @PreAuthorize("@perm.has('system:param:edit')")
    public R<Void> update(@Valid @RequestBody ParamUpdateRequest request) {
        sysParamService.update(request);
        return R.ok(null, "参数更新成功");
    }

    @Operation(summary = "删除参数")
    @DeleteMapping
    @PreAuthorize("@perm.has('system:param:remove')")
    public R<Void> delete(@RequestBody Set<Long> ids) {
        sysParamService.delete(ids);
        return R.ok(null, "参数删除成功");
    }

    @Operation(summary = "查询参数详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:param:query')")
    public R<ParamVO> getById(
            @Parameter(description = "参数ID")
            @PathVariable Long id) {
        return R.ok(sysParamService.getDetailById(id));
    }

    @Operation(summary = "分页查询参数")
    @GetMapping("/page")
    @PreAuthorize("@perm.has('system:param:list')")
    public R<IPage<ParamVO>> page(@Valid ParamPageQuery query) {
        return R.ok(sysParamService.page(query));
    }

    @Operation(summary = "按业务类型查询参数列表")
    @GetMapping("/by-type")
    @PreAuthorize("@perm.has('system:param:list')")
    public R<?> listByBizType(
            @Parameter(description = "业务分类")
            @RequestParam Integer bizType) {

        return R.ok(sysParamService.listByBizType(bizType));
    }

    @Operation(summary = "按参数键获取参数值")
    @GetMapping("/value/{paramKey}")
    @PreAuthorize("@perm.has('system:param:value')")
    public R<?> getValueByKey(
            @Parameter(description = "参数键")
            @PathVariable String paramKey) {

        Object value = sysParamService.getValueByKey(paramKey);
        return R.ok(value);
    }

    @Operation(summary = "按参数键获取字符串值")
    @GetMapping("/string/{paramKey}")
    @PreAuthorize("@perm.has('system:param:value')")
    public R<String> getStringByKey(
            @Parameter(description = "参数键")
            @PathVariable String paramKey) {

        String value = sysParamService.getStringByKey(paramKey);
        return R.ok(value);
    }

    @Operation(summary = "按参数键获取整数值")
    @GetMapping("/integer/{paramKey}")
    @PreAuthorize("@perm.has('system:param:value')")
    public R<Integer> getIntegerByKey(
            @Parameter(description = "参数键")
            @PathVariable String paramKey) {

        Integer value = sysParamService.getIntegerByKey(paramKey);
        return R.ok(value);
    }

    @Operation(summary = "按参数键获取布尔值")
    @GetMapping("/boolean/{paramKey}")
    @PreAuthorize("@perm.has('system:param:value')")
    public R<Boolean> getBooleanByKey(
            @Parameter(description = "参数键")
            @PathVariable String paramKey) {

        Boolean value = sysParamService.getBooleanByKey(paramKey);
        return R.ok(value);
    }
}
