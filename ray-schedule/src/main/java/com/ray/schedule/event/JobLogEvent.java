package com.ray.schedule.event;

import com.ray.schedule.entity.SysJobLog;

/**
 * 任务执行日志事件
 *
 * @param jobLog 任务执行日志
 */
public record JobLogEvent(SysJobLog jobLog) {
}
