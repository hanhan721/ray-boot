package com.ray.schedule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.schedule.domain.JobPageQuery;
import com.ray.schedule.entity.SysJob;
import com.ray.schedule.service.JobService;
import com.ray.schedule.service.JobService.CronValidateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 定时任务管理
 */
@Tag(name = "定时任务管理")
@RestController
@RequestMapping("/api/schedule/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    public R<IPage<SysJob>> page(@Valid JobPageQuery query) {
        return R.ok(jobService.page(query));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    public R<SysJob> getById(@PathVariable Long id) {
        return R.ok(jobService.getById(id));
    }

    @Operation(summary = "新增任务")
    @PostMapping
    public R<Void> create(@Valid @RequestBody SysJob job) {
        jobService.create(job);
        return R.ok();
    }

    @Operation(summary = "修改任务")
    @PutMapping
    public R<Void> update(@Valid @RequestBody SysJob job) {
        jobService.update(job);
        return R.ok();
    }

    @Operation(summary = "删除任务")
    @DeleteMapping
    public R<Void> delete(@RequestBody Set<Long> ids) {
        jobService.delete(ids);
        return R.ok();
    }

    @Operation(summary = "暂停任务")
    @PutMapping("/{id}/pause")
    public R<Void> pause(@PathVariable Long id) {
        jobService.pause(id);
        return R.ok();
    }

    @Operation(summary = "恢复任务")
    @PutMapping("/{id}/resume")
    public R<Void> resume(@PathVariable Long id) {
        jobService.resume(id);
        return R.ok();
    }

    @Operation(summary = "立即执行")
    @PutMapping("/{id}/trigger")
    public R<Void> trigger(@PathVariable Long id) {
        jobService.trigger(id);
        return R.ok();
    }

    @Operation(summary = "Cron表达式校验")
    @GetMapping("/cron/validate")
    public R<CronValidateResult> validateCron(@Parameter(description = "Cron表达式") @RequestParam String expression) {
        return R.ok(jobService.validateCron(expression));
    }
}
