package com.ray.common.storage.service;

import com.ray.common.storage.annotation.AccessLevel;
import java.util.Map;
import java.util.Set;

/** 业务接口必须先鉴权。系统模块实现批量解析，公共层不反向依赖业务层。 */
public interface FileResponseResolver {
    /** 对已完成业务鉴权的文件 ID 批量解析；level 仅控制链接寿命，不代替访问权限。 */
    Map<Long, FileView> resolve(Set<Long> ids, AccessLevel level);
    /** 响应字段统一展示结构，不暴露 hash 或内部存储位置。 */
    record FileView(Long fileId, String name, String url, String ext, long size) { }
}
