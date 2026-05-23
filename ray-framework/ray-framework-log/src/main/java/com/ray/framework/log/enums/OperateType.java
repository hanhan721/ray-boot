package com.ray.framework.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum OperateType {

    /**
     * 其他
     */
    OTHER(0, "其他"),

    /**
     * 查询
     */
    QUERY(1, "查询"),

    /**
     * 新增
     */
    CREATE(2, "新增"),

    /**
     * 修改
     */
    UPDATE(3, "修改"),

    /**
     * 删除
     */
    DELETE(4, "删除"),

    /**
     * 导入
     */
    IMPORT(5, "导入"),

    /**
     * 导出
     */
    EXPORT(6, "导出"),

    /**
     * 登录
     */
    LOGIN(7, "登录"),

    /**
     * 登出
     */
    LOGOUT(8, "登出");

    private final int code;
    private final String label;
}
