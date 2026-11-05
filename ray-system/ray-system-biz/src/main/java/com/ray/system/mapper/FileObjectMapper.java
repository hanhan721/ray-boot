package com.ray.system.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ray.system.api.entity.SysFileObject;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 物理对象元数据访问；状态迁移由文件发布与 GC 在 hash 锁内协调。 */
@Mapper
public interface FileObjectMapper extends MPJBaseMapper<SysFileObject> {
    /** 锁等待后强制清理 MyBatis 缓存重新读取，避免复用等待前的对象状态。 */
    @Select("SELECT * FROM sys_file_object WHERE id=#{id}")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    SysFileObject fresh(long id);
    /** 查询当前命名空间唯一可复用对象；发布阶段必须在 hash 锁内再次调用。 */
    @Select("SELECT * FROM sys_file_object WHERE storage_id=#{storageId} AND file_hash=#{hash} AND status='AVAILABLE'")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    SysFileObject available(@Param("storageId") String storageId, @Param("hash") String hash);

    /** 按主键游标每批扫描最多 100 个可用或回收中对象，不加载已完成回收的历史行。 */
    @Select("SELECT * FROM sys_file_object WHERE id > #{after} AND status IN ('AVAILABLE','DELETING') ORDER BY id LIMIT 100")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    List<SysFileObject> scan(long after);

    /** 启动检查仍需读写的历史对象是否全部属于当前存储身份。 */
    @Select("SELECT EXISTS(SELECT 1 FROM sys_file_object WHERE status IN ('AVAILABLE','DELETING') AND (storage_id<>#{storageId} OR storage_type::text<>#{type} OR bucket<>#{bucket}))")
    boolean mismatched(@Param("storageId") String storageId, @Param("type") String type, @Param("bucket") String bucket);

    /** 孤儿删除前检查 key 是否仍被有效对象跟踪；PURGED 历史行不阻止遗留字节清理。 */
    @Select("SELECT EXISTS(SELECT 1 FROM sys_file_object WHERE storage_id=#{storageId} AND object_key=#{key} AND status IN ('AVAILABLE','DELETING'))")
    @Options(useCache=false, flushCache=Options.FlushCachePolicy.TRUE)
    boolean tracked(@Param("storageId") String storageId, @Param("key") String key);
}
