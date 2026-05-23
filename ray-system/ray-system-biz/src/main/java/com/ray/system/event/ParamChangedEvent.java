package com.ray.system.event;

/**
 * 系统参数变更事件
 *
 * @param paramKey 变更的参数键
 */
public record ParamChangedEvent(String paramKey) {
}
