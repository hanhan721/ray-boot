package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.system.api.domain.OperLogPageQuery;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.system.api.entity.SysOperLog;
import com.ray.system.mapper.OperLogMapper;
import com.ray.system.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志 Service 实现
 */
@Service
@RequiredArgsConstructor
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, SysOperLog> implements OperLogService {

    @Override
    public IPage<SysOperLog> page(OperLogPageQuery query) {
        return super.page(PageUtils.buildPage(query), QueryWrapperHelper.build(query));
    }

    @Override
    public SysOperLog getById(Long id) {
        return super.getById(id);
    }

    @Override
    public void add(SysOperLog operLog) {
        super.save(operLog);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clean() {
        super.remove(Wrappers.emptyWrapper());
    }
}
