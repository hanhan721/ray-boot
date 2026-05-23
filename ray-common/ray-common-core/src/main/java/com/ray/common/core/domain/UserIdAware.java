package com.ray.common.core.domain;

/**
 * 用户 ID 感知接口
 * <p>
 * 实现此接口的类可以提供当前用户 ID，用于审计字段自动填充
 */
public interface UserIdAware {

    /**
     * 获取用户 ID
     */
    Long getUserId();
}
