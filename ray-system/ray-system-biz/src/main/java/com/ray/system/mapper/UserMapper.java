package com.ray.system.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.ray.system.api.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends MPJBaseMapper<SysUser> {

    /** 显式更新或置空头像外键；调用方在同一事务内维护文件归属及旧头像逻辑删除。 */
    @org.apache.ibatis.annotations.Update("UPDATE sys_user SET avatar_file_id=#{fileId} WHERE id=#{id} AND NOT deleted")
    int updateAvatar(@Param("id") long id, @Param("fileId") Long fileId);

    /**
     * 串行化同一用户的角色变更与会话签发；清除一级缓存以重新验证等待期间的会话状态。
     */
    @Select("SELECT * FROM sys_user WHERE id = #{id} AND deleted = FALSE FOR UPDATE")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    SysUser selectByIdForUpdate(@Param("id") Long id);
}
