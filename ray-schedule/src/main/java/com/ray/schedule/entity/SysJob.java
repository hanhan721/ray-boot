package com.ray.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ray.common.mybatis.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_job")
public class SysJob extends BaseEntity<SysJob> {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 64, message = "任务名称长度不能超过64")
    private String jobName;

    @NotBlank(message = "任务分组不能为空")
    @Size(max = 64, message = "任务分组长度不能超过64")
    private String jobGroup;

    @NotBlank(message = "Bean名称不能为空")
    @Size(max = 200, message = "Bean名称长度不能超过200")
    private String beanName;

    @NotBlank(message = "方法名称不能为空")
    @Size(max = 200, message = "方法名称长度不能超过200")
    private String methodName;

    @Size(max = 500, message = "方法参数长度不能超过500")
    private String methodParams;

    @NotBlank(message = "Cron表达式不能为空")
    @Size(max = 255, message = "Cron表达式长度不能超过255")
    private String cronExpression;

    @NotNull(message = "是否并发不能为空")
    private Boolean concurrent;

    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
