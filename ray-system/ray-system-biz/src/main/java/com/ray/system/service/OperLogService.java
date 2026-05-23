package com.ray.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.domain.OperLogPageQuery;
import com.ray.system.api.entity.SysOperLog;

/**
 * 操作日志 Service
 */
public interface OperLogService extends IService<SysOperLog> {

    /**
     * 分页查询操作日志
     *
     * @param query 分页参数
     * @return 分页结果
     */
    IPage<SysOperLog> page(OperLogPageQuery query);

    /**
     * 根据 ID 查询操作日志
     *
     * @param id 日志 ID
     * @return 操作日志
     */
    SysOperLog getById(Long id);

    /**
     * 新增操作日志
     *
     * @param operLog 操作日志
     */
    void add(SysOperLog operLog);

    /**
     * 清空操作日志
     */
    void clean();
}
