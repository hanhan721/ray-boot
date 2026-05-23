package com.ray.system.api.domain;

import com.ray.common.core.domain.PageQuery;
import com.ray.common.mybatis.util.Orderable;
import com.ray.common.mybatis.util.QueryColumn;
import com.ray.common.mybatis.util.QueryType;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypePageQuery extends PageQuery implements Orderable {

    @Parameter(description = "字典名称")
    @QueryColumn(QueryType.LIKE)
    private String dictName;

    @Parameter(description = "字典类型")
    @QueryColumn(QueryType.LIKE)
    private String dictType;

    @Parameter(description = "状态")
    private Boolean enabled;
}
