package com.ray.system.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ray.system.api.entity.SysFile;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

/** 逻辑文件元数据访问，绑定、主动删除和过期清理共用逻辑文件行锁。 */
@Mapper
public interface FileMapper extends MPJBaseMapper<SysFile> {
    /** 锁定逻辑行并返回删除状态，调用方必须在锁内拒绝已删除文件。 */
    @Select("SELECT * FROM sys_file WHERE id=#{id} FOR UPDATE")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    SysFile lock(long id);

    /** hash 锁内重新确认物理对象是否仍存在活跃逻辑引用。 */
    @Select("SELECT EXISTS(SELECT 1 FROM sys_file WHERE object_id=#{objectId} AND NOT deleted)")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    boolean activeReference(long objectId);

    /** 仅筛选候选 ID，每批最多 100 项；最终过期条件由行锁内复查确认。 */
    @Select("SELECT id FROM sys_file WHERE id>#{after} AND NOT deleted AND biz_type IS NULL AND create_time<#{before} ORDER BY id LIMIT 100")
    List<Long> expired(@Param("after") long after, @Param("before") LocalDateTime before);

    /** 仅标记逻辑删除及审计信息，不删除字节、不修改零引用计时，也不取得 hash 锁。 */
    @Update("UPDATE sys_file SET deleted=TRUE, deleted_at=CURRENT_TIMESTAMP, update_time=CURRENT_TIMESTAMP, update_by=#{actor} WHERE id=#{id} AND NOT deleted")
    int markDeleted(@Param("id") long id, @Param("actor") Long actor);

    /** 在已锁定的活跃文件上维护业务归属，与调用方真实业务引用使用同一事务。 */
    @Update("UPDATE sys_file SET biz_type=#{type}, biz_id=#{bizId}, update_time=CURRENT_TIMESTAMP, update_by=#{actor} WHERE id=#{id} AND NOT deleted")
    int bind(@Param("id") long id, @Param("type") String type, @Param("bizId") Long bizId, @Param("actor") Long actor);
}
