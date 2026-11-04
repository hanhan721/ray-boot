package com.ray.system.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ray.system.api.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 部门 Mapper
 */
@Mapper
public interface DeptMapper extends MPJBaseMapper<SysDept> {

    /**
     * 锁定部门行直到当前事务结束，用于部门删除与用户部门分配互斥。
     */
    @Select("""
            <script>
            SELECT * FROM sys_dept
            WHERE deleted = FALSE
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            ORDER BY id
            FOR UPDATE
            </script>
            """)
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    List<SysDept> selectByIdsForUpdate(@Param("ids") Collection<Long> ids);
}
