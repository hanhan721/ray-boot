package com.ray.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ray.common.mybatis.entity.BaseEntity;
import com.ray.system.api.enums.ParamType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 系统参数实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_param")
public class SysParam extends BaseEntity<SysParam> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 参数键（唯一标识）
     */
    @NotBlank(message = "参数键不能为空")
    private String paramKey;

    /**
     * 参数值（数据库统一存字符串）
     */
    @NotBlank(message = "参数值不能为空")
    private String paramValue;

    /**
     * 参数类型
     */
    @NotNull(message = "参数类型不能为空")
    private ParamType paramType;

    /**
     * 业务分类：0=系统，1=用户，2=订单，3=支付，4=通知，99=其他
     */
    @NotNull(message = "业务分类不能为空")
    private Integer bizType;

    /**
     * 是否加密：0=否，1=是
     */
    private Boolean encrypted;

    /**
     * 参数描述
     */
    private String description;
}
