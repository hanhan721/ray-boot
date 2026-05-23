package com.ray.system.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务操作类型
 */
@Getter
@AllArgsConstructor
public enum BusinessType {

    OTHER("OTHER", "其他"),
    INSERT("INSERT", "新增"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除"),
    GRANT("GRANT", "授权"),
    EXPORT("EXPORT", "导出"),
    IMPORT("IMPORT", "导入");

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
