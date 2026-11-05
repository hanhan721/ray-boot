package com.ray.system.api.file;

import java.util.Map;
import java.util.Set;

/** 业务模块报告真实引用；只查询，不在文件删除事务中锁定业务行。 */
public interface FileReferenceChecker {
    /** 返回候选文件的全部真实业务引用，不能仅依据 sys_file 的归属字段返回结果。 */
    Map<Long, Set<Reference>> references(Set<Long> fileIds);
    /** 与文件归属字段对应的业务定位，用于发现错误绑定或遗漏解绑。 */
    record Reference(String bizType, Long bizId) { }
}
