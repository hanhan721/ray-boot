package com.ray.system.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ray.system.api.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends MPJBaseMapper<SysUser> {
}
