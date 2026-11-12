package com.ray.system.api.reference;

import java.util.Set;

/** 业务模块报告对系统角色的真实引用；删除前调用，只读业务数据，不在删除事务中锁定业务行。 */
public interface RoleReferenceChecker {
    /** 返回候选角色中仍被业务引用的 id；返回集合不能为 null。 */
    Set<Long> referencedRoleIds(Set<Long> roleIds);
}
