package com.ray.common.storage.storage;

import com.ray.common.core.exception.BusinessException;
import com.ray.common.storage.config.StorageProperties;
import com.ray.common.storage.enums.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import jakarta.annotation.PreDestroy;

import com.google.common.collect.Lists;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * RustFS 文件存储实现（S3 兼容）
 */
@Slf4j
public class RustFSStorageService implements StorageService {

    private static final int MAX_DELETE_BATCH_SIZE = 1000;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public RustFSStorageService(StorageProperties properties) {
        var config = properties.rustFS();
        this.bucket = config.bucket();
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(config.endpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKey(), config.secretKey())
                ))
                .forcePathStyle(true)
                .build();
        var presignerEndpoint = config.presignerEndpoint();
        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(presignerEndpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKey(), config.secretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
        this.ensureBucketExists();
        log.info("RustFS 存储已连接: endpoint={}, presigner={}, bucket={}", config.endpoint(), presignerEndpoint, this.bucket);
    }

    @Override
    public void upload(InputStream inputStream, String filePath, String contentType, long contentLength) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(filePath)
                            .contentType(contentType)
                            .contentLength(contentLength)
                            .build(),
                    RequestBody.fromInputStream(inputStream, contentLength)
            );
        } catch (S3Exception e) {
            log.error("RustFS 上传异常: {}", filePath, e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public InputStream download(String filePath) {
        return this.download(this.bucket, filePath);
    }

    @Override
    public InputStream download(String bucket, String filePath) {
        var targetBucket = StringUtils.hasText(bucket) ? bucket : this.bucket;
        try {
            return s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(targetBucket)
                            .key(filePath)
                            .build()
            );
        } catch (NoSuchKeyException e) {
            throw new BusinessException("文件不存在");
        } catch (S3Exception e) {
            log.error("RustFS 下载异常: bucket={}, filePath={}", targetBucket, filePath, e);
            throw new BusinessException("文件下载失败");
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(filePath)
                            .build()
            );
        } catch (S3Exception e) {
            log.error("RustFS 删除异常: {}", filePath, e);
            throw new BusinessException("文件删除失败");
        }
    }

    @Override
    public void batchDelete(List<String> filePaths) {
        this.batchDelete(this.bucket, filePaths);
    }

    @Override
    public void batchDelete(String bucket, List<String> filePaths) {
        var targetBucket = StringUtils.hasText(bucket) ? bucket : this.bucket;
        // S3 deleteObjects 单次上限 1000，超过需分批
        for (var batch : Lists.partition(filePaths, MAX_DELETE_BATCH_SIZE)) {
            List<ObjectIdentifier> keys = batch.stream()
                    .map(path -> ObjectIdentifier.builder().key(path).build())
                    .toList();
            try {
                s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(targetBucket)
                        .delete(Delete.builder().objects(keys).quiet(true).build())
                        .build());
            } catch (S3Exception e) {
                log.error("RustFS 批量删除异常: bucket={}, count={}", targetBucket, batch.size(), e);
                throw new BusinessException("文件批量删除失败");
            }
        }
    }

    @Override
    public boolean exists(String filePath) {
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(filePath)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("RustFS 查询异常: {}", filePath, e);
            return false;
        }
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.RUST_FS;
    }

    @Override
    public String getBucket() {
        return bucket;
    }

    @Override
    public String getAccessUrl(String bucket, String filePath, Duration duration) {
        var targetBucket = StringUtils.hasText(bucket) ? bucket : this.bucket;
        var request = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(targetBucket)
                        .key(filePath)
                        .build())
                .build();
        return s3Presigner.presignGetObject(request).url().toString();
    }

    @Override
    public String extractKey(String url) {
        // S3 presigned URL 格式: {endpoint}/{bucket}/{key}?X-Amz-...
        try {
            var path = URI.create(url).getPath();
            var prefix = "/" + this.bucket + "/";
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        } catch (Exception e) {
            log.warn("从 URL 提取存储 key 失败，原样返回: {}", url);
        }
        return url;
    }

    @PreDestroy
    public void close() {
        s3Client.close();
        s3Presigner.close();
    }

    /**
     * 确保桶存在，不存在则创建
     */
    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("RustFS 桶已创建: {}", bucket);
        }
    }
}
