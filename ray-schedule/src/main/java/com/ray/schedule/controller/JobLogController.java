package com.ray.schedule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.core.domain.R;
import com.ray.schedule.domain.JobLogPageQuery;
import com.ray.schedule.entity.SysJobLog;
import com.ray.schedule.service.JobLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 任务执行日志
 */
@Tag(name = "任务执行日志")
@RestController
@RequestMapping("/api/schedule/job-log")
@RequiredArgsConstructor
public class JobLogController {

    private final JobLogService jobLogService;

    @Operation(summary = "分页查询")
    @GetMapping("/page")
    @PreAuthorize("@perm.has('schedule:job-log:list')")
    public R<IPage<SysJobLog>> page(@Valid JobLogPageQuery query) {
        return R.ok(jobLogService.page(query));
    }

    @Operation(summary = "查询详情")
    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('schedule:job-log:query')")
    public R<SysJobLog> getById(@PathVariable Long id) {
        return R.ok(jobLogService.getById(id));
    }

    @Operation(summary = "清空全部日志")
    @DeleteMapping("/clean")
    @PreAuthorize("@perm.has('schedule:job-log:clean')")
    public R<Void> clean() {
        jobLogService.clean();
        return R.ok();
    }

    @Operation(summary = "按保留天数清理日志")
    @DeleteMapping("/clean-by-days")
    @PreAuthorize("@perm.has('schedule:job-log:clean')")
    public R<Long> cleanByDays(
            @Parameter(description = "保留最近多少天的日志") @RequestParam(defaultValue = "30") int retainDays) {
        return R.ok(jobLogService.cleanBefore(retainDays));
    }
}
