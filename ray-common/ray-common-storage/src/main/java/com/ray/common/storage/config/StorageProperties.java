package com.ray.common.storage.config;

import com.ray.common.storage.enums.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 文件存储配置属性
 *
 * @param storageType 存储类型
 * @param local       本地存储配置
 * @param rustFS      RustFS 存储配置
 */
@ConfigurationProperties(prefix = "ray.storage")
public record StorageProperties(
        StorageType storageType,
        LocalConfig local,
        RustFSConfig rustFS,
        AccessConfig access) {

    /**
     * 本地存储配置
     *
     * @param basePath 存储根目录
     */
    public record LocalConfig(String basePath) { }

    /**
     * RustFS 存储配置（S3 兼容）
     *
     * @param endpoint       内部服务地址（用于上传/删除等操作），如 http://rustfs:9000
     * @param publicEndpoint 公开访问地址（用于生成 presigned URL），为空时回退到 endpoint
     * @param accessKey      访问密钥
     * @param secretKey      密钥
     * @param bucket         默认桶名
     */
    public record RustFSConfig(String endpoint, String publicEndpoint, String accessKey, String secretKey,
                                String bucket) {

        /**
         * 获取 presigned URL 使用的端点，优先使用 publicEndpoint
         */
        public String presignerEndpoint() {
            return StringUtils.hasText(publicEndpoint) ? publicEndpoint : endpoint;
        }
    }

    /**
     * 直接访问 URL 配置
     *
     * @param publicBaseUrl 对外访问基础地址，如 http://localhost:8080
     * @param signSecret    LOCAL 直链签名密钥
     */
    public record AccessConfig(String publicBaseUrl, String signSecret) { }
}
