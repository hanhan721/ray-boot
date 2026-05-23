package com.ray.framework.log.handler;

import com.ray.framework.log.event.OperateLogEvent;

/**
 * 操作日志处理接口
 * <p>
 * 业务模块实现此接口，完成日志的持久化存储
 */
public interface OperateLogHandler {

    /**
     * 处理操作日志
     *
     * @param event 操作日志事件
     */
    void handle(OperateLogEvent event);
}
