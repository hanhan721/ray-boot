package com.ray.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ray.common.mybatis.entity.IdBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 用户列配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_user_column_config")
public class SysUserColumnConfig extends IdBaseEntity<SysUserColumnConfig> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 页面标识
     */
    private String pageKey;

    /**
     * 列配置 JSON
     */
    private String columns;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
