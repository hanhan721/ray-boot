package com.ray.system.api.domain;

import com.ray.common.core.domain.PageQuery;
import com.ray.common.mybatis.util.Orderable;
import com.ray.common.mybatis.util.QueryColumn;
import com.ray.common.mybatis.util.QueryType;
import com.ray.system.api.enums.Gender;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPageQuery extends PageQuery implements Orderable {

    @Parameter(description = "用户名")
    @QueryColumn(QueryType.LIKE)
    private String username;

    @Parameter(description = "昵称")
    @QueryColumn(QueryType.LIKE)
    private String nickname;

    @Parameter(description = "邮箱")
    @QueryColumn(QueryType.LIKE)
    private String email;

    @Parameter(description = "手机号")
    @QueryColumn(QueryType.LIKE)
    private String phone;

    @Parameter(description = "性别")
    private Gender gender;

    @Parameter(description = "状态")
    private Boolean enabled;
}
