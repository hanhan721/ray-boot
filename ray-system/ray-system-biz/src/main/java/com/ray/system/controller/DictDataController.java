package com.ray.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.system.api.domain.DictDataPageQuery;
import com.ray.system.api.entity.SysDictData;
import com.ray.system.service.DictDataService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 字典数据管理
 */
@Tag(name = "字典数据管理")
@RestController
@RequestMapping("/api/sys/dict/data")
@RequiredArgsConstructor
public class DictDataController {

    private final DictDataService dictDataService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @PreAuthorize("@perm.has('system:dict-data:list')")
    public R<IPage<SysDictData>> page(@Valid DictDataPageQuery query) {
        return R.ok(dictDataService.page(query));
    }

    @Operation(summary = "根据字典类型查询数据列表")
    @GetMapping("/type/{dictType}")
    @PreAuthorize("@perm.has('system:dict-data:list')")
    public R<List<SysDictData>> listByDictType(@PathVariable String dictType) {
        return R.ok(dictDataService.listByDictType(dictType));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('system:dict-data:query')")
    public R<SysDictData> getById(@PathVariable Long id) {
        return R.ok(dictDataService.getById(id));
    }

    @Operation(summary = "新增字典数据")
    @PostMapping
    @PreAuthorize("@perm.has('system:dict-data:add')")
    public R<Void> add(@Valid @RequestBody SysDictData dictData) {
        dictDataService.add(dictData);
        return R.ok();
    }

    @Operation(summary = "修改字典数据")
    @PutMapping
    @PreAuthorize("@perm.has('system:dict-data:edit')")
    public R<Void> update(@Valid @RequestBody SysDictData dictData) {
        dictDataService.update(dictData);
        return R.ok();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping
    @PreAuthorize("@perm.has('system:dict-data:remove')")
    public R<Void> delete(@RequestBody Set<Long> ids) {
        dictDataService.delete(ids);
        return R.ok();
    }
}
