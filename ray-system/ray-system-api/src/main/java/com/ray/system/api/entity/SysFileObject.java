package com.ray.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ray.common.mybatis.entity.IdBaseEntity;
import com.ray.common.storage.enums.StorageType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

/** 物理对象元数据；同存储身份下相同内容仅允许一个 AVAILABLE 对象，历史行永久保留。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_file_object")
public class SysFileObject extends IdBaseEntity<SysFileObject> {
    /** 后端类型和物理位置的稳定指纹，不包含访问凭证或公开地址。 */
    private String storageId;
    private StorageType storageType;
    private String bucket;
    /** 实际内容的 SHA-256，小写十六进制；对象创建后不可修改。 */
    private String fileHash;
    /** 日期 UUID 路径；历史 key 不得重新发布为其他对象。 */
    private String objectKey;
    private Long fileSize;
    private String contentType;
    /** 仅允许 AVAILABLE → DELETING → PURGED，不允许恢复旧对象。 */
    private String status;
    /** GC 首次确认没有活跃逻辑引用的时间，重新复用时清空。 */
    private LocalDateTime unreferencedSince;
    /** 已提交回收声明的时间；删除中断后保留状态供下轮重试。 */
    private LocalDateTime deleteStartedAt;
    /** 存储删除成功或明确不存在后，数据库确认完成回收的时间。 */
    private LocalDateTime purgedAt;
    private LocalDateTime createTime;
}
