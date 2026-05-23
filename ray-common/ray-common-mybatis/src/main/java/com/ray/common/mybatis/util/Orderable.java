package com.ray.common.mybatis.util;

/**
 * 可排序查询接口
 *
 * Query DTO 实现此接口以声明默认排序规则，{@link QueryWrapperHelper} 会自动应用。
 * 默认按 id 倒序排列，可覆写方法自定义。
 *
 * - 默认排序：id DESC
 * - 自定义排序：覆写 orderColumn() 和 orderAsc()
 * - 多字段排序：覆写 orderColumns() 和 orderDirections()
 */
public interface Orderable {

    /**
     * 排序列名，默认 id
     */
    default String orderColumn() {
        return "id";
    }

    /**
     * 是否升序，默认 false（降序）
     */
    default boolean orderAsc() {
        return false;
    }
}
