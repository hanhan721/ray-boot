package com.ray.common.mybatis.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注查询字段的查询规则
 * <p>
 * 用于 Query DTO 字段上，配合 {@link QueryWrapperHelper} 自动构建查询条件。
 * 未标注此注解的字段默认使用 EQ 查询。
 *
 * <pre>
 * public class UserPageQuery extends PageQuery {
 *     private Integer status;                    // 无注解 -> EQ
 *
 *     &#064;QueryColumn(QueryType.LIKE)
 *     private String username;                   // LIKE
 *
 *     &#064;QueryColumn(value = QueryType.GE, column = "create_time")
 *     private LocalDateTime startTime;           // >= ，指定列名
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface QueryColumn {

    /**
     * 查询类型，默认 EQ
     */
    QueryType value() default QueryType.EQ;

    /**
     * 对应数据库列名。默认为空，由字段名驼峰转下划线自动推导。
     */
    String column() default "";
}
