package com.ray.system.api.domain;

import com.ray.system.api.entity.SysUser;

import java.util.Set;

/**
 * 用户详情响应（含关联角色、部门 ID）
 *
 * @param user    用户信息
 * @param roleIds 角色 ID 集合
 * @param deptIds 部门 ID 集合
 */
public record UserDetailVO(SysUser user, Set<Long> roleIds, Set<Long> deptIds) {
}
