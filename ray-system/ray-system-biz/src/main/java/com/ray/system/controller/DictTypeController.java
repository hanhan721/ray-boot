package com.ray.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.system.api.domain.DictTypePageQuery;
import com.ray.system.api.entity.SysDictType;
import com.ray.system.service.DictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 字典类型管理
 */
@Tag(name = "字典类型管理")
@RestController
@RequestMapping("/api/sys/dict/type")
@RequiredArgsConstructor
public class DictTypeController {

    private final DictTypeService dictTypeService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @PreAuthorize("@perm.has('system:dict-type:list')")
    public R<IPage<SysDictType>> page(@Valid DictTypePageQuery query) {
        return R.ok(dictTypeService.page(query));
    }

    @Operation(summary = "查询所有字典类型")
    @GetMapping("/list")
    @PreAuthorize("@perm.has('system:dict-type:list')")
    public R<List<SysDictType>> list() {
        return R.ok(dictTypeService.listAll());
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:dict-type:query')")
    public R<SysDictType> getById(@PathVariable Long id) {
        return R.ok(dictTypeService.getById(id));
    }

    @Operation(summary = "根据字典类型编码查询")
    @GetMapping("/type/{dictType}")
    @PreAuthorize("@perm.has('system:dict-type:query')")
    public R<SysDictType> getByDictType(@PathVariable String dictType) {
        return R.ok(dictTypeService.getByDictType(dictType));
    }

    @Operation(summary = "新增字典类型")
    @PostMapping
    @PreAuthorize("@perm.has('system:dict-type:add')")
    public R<Void> add(@Valid @RequestBody SysDictType dictType) {
        dictTypeService.add(dictType);
        return R.ok();
    }

    @Operation(summary = "修改字典类型")
    @PutMapping
    @PreAuthorize("@perm.has('system:dict-type:edit')")
    public R<Void> update(@Valid @RequestBody SysDictType dictType) {
        dictTypeService.update(dictType);
        return R.ok();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping
    @PreAuthorize("@perm.has('system:dict-type:remove')")
    public R<Void> delete(@RequestBody Set<Long> ids) {
        dictTypeService.delete(ids);
        return R.ok();
    }
}
