package com.ray.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ray.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/** 逻辑文件。只允许通过文件发布、绑定和删除服务修改。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_file")
public class SysFile extends BaseEntity<SysFile> {
    /** 多个逻辑文件可共享同一物理对象，大小、MIME 和 hash 不在此重复保存。 */
    private Long objectId;
    private String originalName;
    private String fileExt;
    /** 与 bizId 同时为空表示未绑定；绑定或解绑须参加对应业务事务。 */
    private String bizType;
    private Long bizId;
    /** 逻辑删除不可恢复；此时间不代表物理字节已被回收。 */
    private LocalDateTime deletedAt;
}
