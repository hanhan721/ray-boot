package com.ray.system.api.domain;

import com.ray.common.core.domain.PageQuery;
import com.ray.common.mybatis.util.Orderable;
import com.ray.common.mybatis.util.QueryColumn;
import com.ray.common.mybatis.util.QueryType;
import com.ray.system.api.enums.BusinessType;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperLogPageQuery extends PageQuery implements Orderable {

    @Parameter(description = "操作模块")
    @QueryColumn(QueryType.LIKE)
    private String title;

    @Parameter(description = "业务类型")
    private BusinessType bizType;

    @Parameter(description = "操作人员")
    @QueryColumn(QueryType.LIKE)
    private String operUserName;

    @Parameter(description = "请求方式")
    private String requestMethod;

    @Parameter(description = "操作状态")
    private Boolean success;

    @Parameter(description = "操作IP")
    private String operIp;
}
