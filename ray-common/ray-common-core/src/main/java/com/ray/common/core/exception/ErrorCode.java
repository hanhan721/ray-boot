package com.ray.common.core.exception;

/**
 * 错误码接口
 * <p>
 * 各模块可实现此接口定义自己的错误码枚举
 */
public interface ErrorCode {

    /**
     * 获取错误码
     */
    int getCode();

    /**
     * 获取错误信息
     */
    String getMsg();
}
