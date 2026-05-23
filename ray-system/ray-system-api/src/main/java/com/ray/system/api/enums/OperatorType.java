package com.ray.system.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作者类型
 */
@Getter
@AllArgsConstructor
public enum OperatorType {

    OTHER("OTHER", "其他"),
    BACKEND("BACKEND", "后台用户"),
    MOBILE("MOBILE", "手机端用户");

    /**
     * 数据库存储值
     */
    @EnumValue
    @JsonValue
    private final String value;

    /**
     * 描述
     */
    private final String desc;
}
