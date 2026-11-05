package com.ray.common.storage.config;

import com.ray.common.storage.enums.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import java.time.Duration;

/** 单存储后端配置；上传并发数同时供请求入口限流与 Service 上传名额控制使用。 */
@ConfigurationProperties(prefix = "ray.storage")
public record StorageProperties(StorageType storageType, LocalConfig local, RustFSConfig rustFS,
                                AccessConfig access, Integer uploadConcurrency, Duration operationTimeout) {
    /** 补齐缺失的运行默认值，非法并发或超时配置在启动绑定阶段即失败。 */
    public StorageProperties {
        // 1.缺失配置使用每实例 8 个上传名额和 2 分钟存储操作超时。
        if (uploadConcurrency == null) uploadConcurrency = 8;
        if (operationTimeout == null) operationTimeout = Duration.ofMinutes(2);
        // 2.限制资源占用和阻塞时长，显式错误配置不能静默回退。
        if (uploadConcurrency < 1 || uploadConcurrency > 128 || operationTimeout.isNegative()
                || operationTimeout.isZero() || operationTimeout.compareTo(Duration.ofMinutes(10)) > 0) {
            throw new IllegalArgumentException("非法存储并发数或超时配置");
        }
    }

    public record LocalConfig(String basePath) { }
    public record RustFSConfig(String endpoint, String publicEndpoint, String accessKey, String secretKey,
                               String bucket) {
        /** 优先使用浏览器可访问的公开地址，未配置时回退到内部 endpoint。 */
        public String presignerEndpoint() {
            return StringUtils.hasText(publicEndpoint) ? publicEndpoint : endpoint;
        }
    }
    public record AccessConfig(String publicBaseUrl, String signSecret) { }
}
