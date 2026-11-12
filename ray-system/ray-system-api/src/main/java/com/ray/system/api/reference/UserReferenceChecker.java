package com.ray.system.api.reference;

import java.util.Set;

/** 业务模块报告对系统用户的真实引用；删除前调用，只读业务数据，不在删除事务中锁定业务行。 */
public interface UserReferenceChecker {
    /** 返回候选用户中仍被业务引用的 id；审计字段 create_by/update_by 不算引用，返回集合不能为 null。 */
    Set<Long> referencedUserIds(Set<Long> userIds);
}
