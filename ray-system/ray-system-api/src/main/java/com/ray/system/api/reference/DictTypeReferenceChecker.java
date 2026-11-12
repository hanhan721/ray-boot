package com.ray.system.api.reference;

import java.util.Set;

/** 业务模块报告对系统字典类型的真实引用；删除前调用，只读业务数据，不在删除事务中锁定业务行。 */
public interface DictTypeReferenceChecker {
    /** 返回候选字典类型中仍被业务引用的编码；返回集合不能为 null。 */
    Set<String> referencedDictTypes(Set<String> dictTypes);
}
