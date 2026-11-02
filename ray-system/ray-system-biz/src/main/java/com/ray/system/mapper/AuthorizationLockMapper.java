package com.ray.system.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/** PostgreSQL 事务级锁：签发共享、授权变更独占；必须先于用户行锁取得。 */
@Mapper
public interface AuthorizationLockMapper {
    // 所有实例共用同一资源，防止旧授权在变更提交后被并发签发。
    int LOCK_NAMESPACE = 1380014401;
    int LOCK_RESOURCE = 1;

    @Select("SELECT pg_advisory_xact_lock_shared(" + LOCK_NAMESPACE + ", " + LOCK_RESOURCE + ")")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Object lockForIssuance();

    @Select("SELECT pg_advisory_xact_lock(" + LOCK_NAMESPACE + ", " + LOCK_RESOURCE + ")")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    Object lockForChange();
}
