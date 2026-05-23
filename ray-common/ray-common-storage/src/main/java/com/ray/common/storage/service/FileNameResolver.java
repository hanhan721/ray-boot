package com.ray.common.storage.service;

import java.util.Collection;
import java.util.Map;

/**
 * 文件名解析接口，用于根据存储路径查询原始文件名
 * <p>
 * 由 system-biz 等模块提供实现，common-storage 通过 Spring 可选注入获取
 */
public interface FileNameResolver {

    /**
     * 批量查询文件原始名称
     *
     * @param filePaths 存储路径集合
     * @return filePath → originalName
     */
    Map<String, String> resolve(Collection<String> filePaths);
}
