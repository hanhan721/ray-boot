package com.ray.common.storage.storage;

import com.ray.common.storage.enums.StorageType;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** 字节操作必须在数据库事务外执行；key 一经发布永不复用。 */
public interface StorageService {
    /** 流式写入新 key，MIME 与大小必须来自服务端内容检测。 */
    void upload(InputStream input, String key, String contentType, long size);
    /** 打开字节流，由调用方负责关闭；不承担业务权限校验。 */
    InputStream download(String key);
    /** 成功返回表示已删除或明确不存在，其他情况必须抛出异常。 */
    void delete(String key);
    /** 仅明确不存在时返回 false，权限或网络失败必须抛出异常。 */
    boolean exists(String key);
    StorageType getStorageType();
    /** 返回由后端类型与规范化物理位置确定的稳定身份。 */
    String getStorageId();
    String getBucket();
    /** 按用途和原始名称签发链接，调用方须先完成权限及文件状态校验。 */
    String getAccessUrl(SignRequest request);
    /** 每页不超过 100 个，游标是后端的不透明 continuation token。 */
    ObjectPage list(String cursor);

    /** 统一签名输入；响应头覆盖仅影响当前链接，不修改共享对象元数据。 */
    record SignRequest(long fileId, String key, String originalName, String contentType,
                       String purpose, Duration duration, String cacheControl) { }
    /** 孤儿扫描所需的最小存储元数据，修改时间用于避开正常上传窗口。 */
    record StoredObject(String key, Instant modifiedAt, long size) { }
    /** nextCursor 为 null 表示扫描结束，调用方不解析后端游标。 */
    record ObjectPage(List<StoredObject> objects, String nextCursor) { }
}
