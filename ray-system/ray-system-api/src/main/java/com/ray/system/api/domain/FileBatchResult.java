package com.ray.system.api.domain;

/** index 从 0 开始，单项失败不回滚其他成功项。 */
public record FileBatchResult(int index, String originalName, boolean ok, FileUploadVO data,
                              String errorCode, String reason) { }
