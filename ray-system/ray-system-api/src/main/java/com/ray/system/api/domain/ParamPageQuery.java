package com.ray.system.api.domain;

import com.ray.common.core.domain.PageQuery;
import com.ray.common.mybatis.util.Orderable;
import com.ray.system.api.enums.ParamType;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ParamPageQuery extends PageQuery implements Orderable {

    @Parameter(description = "业务分类")
    private Integer bizType;

    @Parameter(description = "参数类型")
    private ParamType paramType;
}
