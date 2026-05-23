package com.ray.common.mybatis.util;

/**
 * 查询条件类型
 */
public enum QueryType {

    /** 等于（默认） */
    EQ,
    /** 不等于 */
    NE,
    /** 模糊匹配 */
    LIKE,
    /** 左模糊 (%value) */
    LEFT_LIKE,
    /** 右模糊 (value%) */
    RIGHT_LIKE,
    /** 大于 */
    GT,
    /** 大于等于 */
    GE,
    /** 小于 */
    LT,
    /** 小于等于 */
    LE,
    /** IN 查询（字符串逗号分隔，或 Collection） */
    IN,
    /** 忽略该字段 */
    IGNORE,
}
