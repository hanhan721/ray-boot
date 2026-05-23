package com.ray.system.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ray.system.api.entity.SysUserDept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户部门关联 Mapper
 */
@Mapper
public interface UserDeptMapper extends MPJBaseMapper<SysUserDept> {
}
