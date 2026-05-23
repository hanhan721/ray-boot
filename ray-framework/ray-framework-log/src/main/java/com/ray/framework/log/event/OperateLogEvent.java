package com.ray.framework.log.event;

import com.ray.framework.log.enums.OperateType;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志事件
 */
@Data
@Accessors(chain = true)
public class OperateLogEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模块名称
     */
    private String module;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 操作类型
     */
    private OperateType operateType;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求 URL
     */
    private String requestUrl;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应数据
     */
    private String responseData;

    /**
     * 操作用户 ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作 IP
     */
    private String ip;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long duration;

    /**
     * 操作状态（1-成功 0-失败）
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;
}
