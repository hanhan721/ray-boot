package com.ray.schedule.domain;

import com.ray.common.core.domain.PageQuery;
import com.ray.common.mybatis.util.Orderable;
import com.ray.common.mybatis.util.QueryColumn;
import com.ray.common.mybatis.util.QueryType;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务日志分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobLogPageQuery extends PageQuery implements Orderable {

    @Parameter(description = "任务ID")
    private Long jobId;

    @Parameter(description = "任务名称")
    @QueryColumn(QueryType.LIKE)
    private String jobName;

    @Parameter(description = "任务分组")
    private String jobGroup;

    @Parameter(description = "是否成功")
    private Boolean success;
}
