package com.ray.system.file;

import com.ray.system.api.entity.SysFile;
import com.ray.common.storage.enums.StorageType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文件联表查询的内部结果，合并逻辑文件与对象信息，不能直接作为对外响应。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FileRecord extends SysFile {
    private String storageId;
    private StorageType storageType;
    private String bucket;
    private String objectKey;
    private Long fileSize;
    private String contentType;
    private String status;
}
