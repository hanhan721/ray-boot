package com.ray.common.storage.storage;

import com.ray.common.storage.enums.StorageType;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/**
 * 文件存储服务抽象接口
 * <p>
 * 定义文件存储的基本操作，不同存储后端（本地、RustFS 等）各自实现
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param filePath    存储路径（相对路径）
     * @param contentType   MIME 类型
     * @param contentLength 文件大小（字节）
     */
    void upload(InputStream inputStream, String filePath, String contentType, long contentLength);

    /**
     * 下载文件
     *
     * @param filePath 存储路径（相对路径）
     * @return 文件输入流
     */
    InputStream download(String filePath);

    /**
     * 从指定桶下载文件（仅对象存储有效，本地存储忽略 bucket）
     *
     * @param bucket   存储桶名称，为空则使用默认桶
     * @param filePath 存储路径（相对路径）
     * @return 文件输入流
     */
    default InputStream download(String bucket, String filePath) {
        return download(filePath);
    }

    /**
     * 删除文件
     *
     * @param filePath 存储路径（相对路径）
     */
    void delete(String filePath);

    /**
     * 批量删除文件
     * <p>默认逐个删除，对象存储可覆写为原生批量接口</p>
     *
     * @param filePaths 存储路径列表
     */
    default void batchDelete(List<String> filePaths) {
        for (var filePath : filePaths) {
            delete(filePath);
        }
    }

    /**
     * 从指定桶批量删除文件（仅对象存储有效，本地存储忽略 bucket）
     *
     * @param bucket    存储桶名称，为空则使用默认桶
     * @param filePaths 存储路径列表
     */
    default void batchDelete(String bucket, List<String> filePaths) {
        batchDelete(filePaths);
    }

    /**
     * 判断文件是否存在
     *
     * @param filePath 存储路径（相对路径）
     * @return 是否存在
     */
    boolean exists(String filePath);

    /**
     * 获取存储类型
     */
    StorageType getStorageType();

    /**
     * 获取存储桶名称（仅对象存储有值，本地存储返回 null）
     */
    default String getBucket() {
        return null;
    }

    /**
     * 生成可访问 URL
     *
     * @param bucket   存储桶，为空时实现自行兜底
     * @param filePath 存储路径
     * @param duration 有效时长
     */
    default String getAccessUrl(String bucket, String filePath, Duration duration) {
        throw new UnsupportedOperationException("当前存储实现不支持访问 URL 生成");
    }

    /**
     * 使用默认桶生成可访问 URL
     */
    default String getAccessUrl(String filePath, Duration duration) {
        return getAccessUrl(getBucket(), filePath, duration);
    }

    /**
     * 从完整访问 URL 中提取存储 key
     * <p>
     * 用于反序列化场景：前端回传完整 URL 时自动还原为存储 key
     *
     * @param url 完整的访问 URL
     * @return 存储 key（相对路径），提取失败则原样返回
     */
    String extractKey(String url);
}
