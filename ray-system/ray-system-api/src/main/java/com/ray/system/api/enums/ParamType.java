package com.ray.system.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统参数类型枚举
 */
@Getter
@AllArgsConstructor
public enum ParamType {

    STRING("STRING", "字符串"),
    INTEGER("INTEGER", "整数"),
    BOOLEAN("BOOLEAN", "布尔值"),
    JSON("JSON", "JSON 对象"),
    STRING_LIST("STRING_LIST", "字符串列表");

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
