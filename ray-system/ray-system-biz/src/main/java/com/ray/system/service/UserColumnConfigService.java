package com.ray.system.service;

import com.ray.system.api.vo.ColumnItem;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 用户列配置 Service
 */
public interface UserColumnConfigService {

    /**
     * 获取当前用户指定页面的列配置
     *
     * @param pageKey 页面标识
     * @return 列配置列表，无配置返回 null
     */
    @Nullable
    List<ColumnItem> getByPageKey(String pageKey);

    /**
     * 保存当前用户指定页面的列配置（upsert）
     *
     * @param pageKey 页面标识
     * @param columns 列配置列表
     */
    void saveByPageKey(String pageKey, List<ColumnItem> columns);

    /**
     * 删除当前用户指定页面的列配置（恢复默认）
     *
     * @param pageKey 页面标识
     */
    void deleteByPageKey(String pageKey);
}
