package com.ray.common.storage.storage;

import com.ray.common.storage.config.StorageProperties;
import com.ray.common.storage.enums.StorageType;
import com.ray.common.storage.support.StorageIdentity;
import com.ray.common.storage.util.FileContentUtils;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.io.InputStream;
import java.net.URI;

/** 私有、未启用版本控制的 S3 兼容存储。启动只校验，不修改桶。 */
public class RustFSStorageService implements StorageService {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;
    private final String storageId;

    /** 分别建立内部读写客户端与公开地址签名器，并在启动阶段只读校验私有桶条件。 */
    public RustFSStorageService(StorageProperties properties) {
        // 1.由规范化内部 endpoint 和 bucket 确定身份，凭证及公开地址不纳入指纹。
        var config = properties.rustFS();
        this.bucket = config.bucket();
        this.storageId = StorageIdentity.fingerprint("RUST_FS", StorageIdentity.endpoint(config.endpoint()) + "\n" + bucket);
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(config.accessKey(), config.secretKey()));
        // 2.内部客户端设置有界调用超时，上传、查询和删除均使用内部地址。
        this.client = S3Client.builder().endpointOverride(URI.create(config.endpoint()))
                .region(Region.US_EAST_1).credentialsProvider(credentials).forcePathStyle(true)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(properties.operationTimeout())
                        .apiCallAttemptTimeout(properties.operationTimeout()).build()).build();
        // 3.签名器使用浏览器可访问的公开地址，保持 path-style 寻址一致。
        this.presigner = S3Presigner.builder().endpointOverride(URI.create(config.presignerEndpoint()))
                .region(Region.US_EAST_1).credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
        // 4.只验证桶，不创建或修改；验证失败时释放已经建立的客户端资源。
        try { this.validateBucket(); }
        catch (RuntimeException e) { this.close(); throw e; }
    }

    /** 拒绝版本化、公开 ACL 或配置桶策略的桶，避免共享对象删除与私有访问语义失效。 */
    private void validateBucket() {
        // 1.确认桶存在，并要求从未启用版本控制；Suspended 仍可能保留历史版本。
        client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        // Suspended 仍可能保留历史版本；必须从未启用版本控制。
        if (client.getBucketVersioning(GetBucketVersioningRequest.builder().bucket(bucket).build()).status() != null) {
            throw new IllegalStateException("文件桶必须关闭版本控制（不能使用 Suspended 桶）");
        }
        // 2.拒绝面向用户组的 ACL 授权，确保对象访问依赖私有账号或签名。
        var acl = client.getBucketAcl(GetBucketAclRequest.builder().bucket(bucket).build());
        if (acl.grants().stream().anyMatch(g -> g.grantee().type() == Type.GROUP)) {
            throw new IllegalStateException("文件桶必须私有");
        }
        try {
            // 3.本期只支持无桶策略的专用私有桶，避免错误推断复杂策略的公开性。
            client.getBucketPolicy(GetBucketPolicyRequest.builder().bucket(bucket).build());
            throw new IllegalStateException("文件桶必须私有且不配置桶策略");
        } catch (S3Exception e) {
            if (!"NoSuchBucketPolicy".equals(e.awsErrorDetails().errorCode())) throw e;
        }
    }

    /** 事务外流式上传已检测内容；原始下载名由单次签名覆盖，不写入共享对象元数据。 */
    @Override
    public void upload(InputStream input, String key, String contentType, long size) {
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType)
                .contentLength(size).build(), RequestBody.fromInputStream(input, size));
    }
    /** 打开对象输入流并交由调用方关闭，存储异常直接向上传递。 */
    @Override
    public InputStream download(String key) {
        return client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }
    /** 删除独有历史 key；未启用版本控制的桶中，成功返回即可用于后续 PURGED 确认。 */
    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
    /** 仅明确的 404 视为不存在，权限或网络异常不可伪装成对象缺失。 */
    @Override
    public boolean exists(String key) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }
    @Override
    public StorageType getStorageType() { return StorageType.RUST_FS; }
    @Override
    public String getStorageId() { return storageId; }
    @Override
    public String getBucket() { return bucket; }

    /** 按逻辑文件的原始名称和用途覆盖本次响应头，不修改被多个文件共享的对象元数据。 */
    @Override
    public String getAccessUrl(SignRequest request) {
        // 1.下载用途强制二进制附件，浏览用途使用服务端已检测的 MIME。
        var type = "download".equals(request.purpose()) ? FileContentUtils.BINARY_TYPE : request.contentType();
        // 2.将下载名、MIME、私有缓存策略一同签入 URL，篡改任一响应参数会导致验签失败。
        return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(request.duration())
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(request.key())
                        .responseContentType(type)
                        .responseContentDisposition(FileContentUtils.contentDisposition(type, request.originalName()))
                        .responseCacheControl(request.cacheControl()).build()).build()).url().toString();
    }

    /** 使用后端 continuation token 分页列举，每页最多 100 项，供孤儿扫描按年龄筛选。 */
    @Override
    public ObjectPage list(String cursor) {
        // 1.将游标原样交给 S3，不在应用侧推测或重建后端分页位置。
        var page = client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket)
                .maxKeys(100).continuationToken(cursor).build());
        // 2.仅返回清理所需的 key、修改时间和大小，未截断时结束扫描。
        return new ObjectPage(page.contents().stream()
                .map(o -> new StoredObject(o.key(), o.lastModified(), o.size())).toList(),
                Boolean.TRUE.equals(page.isTruncated()) ? page.nextContinuationToken() : null);
    }

    @PreDestroy
    public void close() { client.close(); presigner.close(); }
}
