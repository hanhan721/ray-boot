package com.ray.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ray.common.mybatis.entity.BaseEntity;
import com.ray.common.storage.enums.StorageType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 文件实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_file")
public class SysFile extends BaseEntity<SysFile> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名（UUID.ext）
     */
    private String fileName;

    /**
     * 存储路径（相对路径）
     */
    private String filePath;

    /**
     * 文件扩展名（不含点号）
     */
    private String fileExt;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME 类型
     */
    private String contentType;

    /**
     * 存储类型
     */
    private StorageType storageType;

    /**
     * 存储桶（MinIO 使用）
     */
    private String bucket;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务关联 ID
     */
    private Long bizId;
}
