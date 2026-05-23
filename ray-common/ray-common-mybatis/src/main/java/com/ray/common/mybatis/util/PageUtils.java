package com.ray.common.mybatis.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.common.core.domain.PageQuery;

/**
 * 分页工具类
 */
public final class PageUtils {

    private PageUtils() {
    }

    /**
     * 从 PageQuery 构建 MyBatis-Plus 分页对象
     */
    public static <T> Page<T> buildPage(PageQuery query) {
        return new Page<>(query.getPageNum(), query.getPageSize());
    }

    /**
     * 从 PageQuery 构建 MyBatis-Plus 分页对象（不查询总数）
     */
    public static <T> Page<T> buildPageNoCount(PageQuery query) {
        return new Page<>(query.getPageNum(), query.getPageSize(), false);
    }
}
