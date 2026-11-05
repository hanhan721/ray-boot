package com.ray.system.api.domain;

/** 上传结果不暴露 hash、key、bucket。 */
public record FileUploadVO(Long fileId, String fileName, String ext, long size, String contentType, String url) { }
