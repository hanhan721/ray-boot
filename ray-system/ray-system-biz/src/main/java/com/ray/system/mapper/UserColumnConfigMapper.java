package com.ray.system.mapper;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.yulichang.base.MPJBaseMapper;
import com.ray.system.api.entity.SysUserColumnConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户列配置 Mapper
 */
@Mapper
public interface UserColumnConfigMapper extends MPJBaseMapper<SysUserColumnConfig> {

    /**
     * 按用户ID和页面标识查询
     */
    default SysUserColumnConfig selectByUserAndPage(Long userId, String pageKey) {
        return selectOne(Wrappers.<SysUserColumnConfig>lambdaQuery()
                .eq(SysUserColumnConfig::getUserId, userId)
                .eq(SysUserColumnConfig::getPageKey, pageKey));
    }

    /**
     * 按用户ID和页面标识删除（物理删除）
     */
    default int deleteByUserAndPage(Long userId, String pageKey) {
        return delete(Wrappers.<SysUserColumnConfig>lambdaQuery()
                .eq(SysUserColumnConfig::getUserId, userId)
                .eq(SysUserColumnConfig::getPageKey, pageKey));
    }
}
