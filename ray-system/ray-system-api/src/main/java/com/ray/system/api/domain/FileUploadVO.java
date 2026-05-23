package com.ray.system.api.domain;

import com.ray.common.storage.annotation.AccessLevel;
import com.ray.common.storage.annotation.StorageFile;

/**
 * 文件上传响应
 *
 * @param id       文件 ID
 * @param filePath 存储路径
 * @param bucket   存储桶
 */
public record FileUploadVO(
        Long id,
        String filePath,
        String bucket,
        @StorageFile(level = AccessLevel.PROTECTED)
        String fileUrl
) {
}
