package com.ray.system.api.domain;

import com.ray.common.core.domain.PageQuery;
import com.ray.common.mybatis.util.Orderable;
import com.ray.common.mybatis.util.QueryColumn;
import com.ray.common.mybatis.util.QueryType;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RolePageQuery extends PageQuery implements Orderable {

    @Parameter(description = "角色名称")
    @QueryColumn(QueryType.LIKE)
    private String roleName;

    @Parameter(description = "角色标识")
    @QueryColumn(QueryType.LIKE)
    private String roleKey;

    @Parameter(description = "状态")
    private Boolean enabled;

    @Override
    public String orderColumn() {
        return "sort";
    }

    @Override
    public boolean orderAsc() {
        return true;
    }
}
