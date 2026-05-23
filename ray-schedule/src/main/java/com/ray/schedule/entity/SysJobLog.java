package com.ray.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("sys_job_log")
public class SysJobLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long jobId;

    private String jobName;

    private String jobGroup;

    private String beanName;

    private String methodName;

    private String methodParams;

    private Boolean success;

    private String result;

    private String errorMsg;

    private Integer costTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
