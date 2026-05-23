package com.ray.framework.log.handler;

import com.ray.framework.log.event.OperateLogEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认操作日志处理器
 * <p>
 * 仅输出到日志文件，业务模块可覆盖此 Bean 实现数据库存储
 */
@Slf4j
public class DefaultOperateLogHandler implements OperateLogHandler {

    @Override
    public void handle(OperateLogEvent event) {
        log.info("[操作日志] 模块: {}, 描述: {}, 用户: {}, IP: {}, 耗时: {}ms, 状态: {}",
                event.getModule(),
                event.getDescription(),
                event.getUsername(),
                event.getIp(),
                event.getDuration(),
                event.getStatus() == 1 ? "成功" : "失败");
    }
}
