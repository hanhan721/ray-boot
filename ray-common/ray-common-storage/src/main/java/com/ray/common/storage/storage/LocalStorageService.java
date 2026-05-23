package com.ray.common.storage.storage;

import com.ray.common.core.exception.BusinessException;
import com.ray.common.storage.config.StorageProperties;
import com.ray.common.storage.enums.StorageType;
import com.ray.common.storage.support.LocalAccessUrlSigner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * 本地文件存储实现
 */
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path basePath;
    private final String publicBaseUrl;
    private final LocalAccessUrlSigner signer;

    public LocalStorageService(StorageProperties properties) {
        this.basePath = Path.of(properties.local().basePath()).toAbsolutePath().normalize();
        var accessConfig = properties.access();
        this.publicBaseUrl = accessConfig != null && StringUtils.hasText(accessConfig.publicBaseUrl())
                ? trimTrailingSlash(accessConfig.publicBaseUrl())
                : "";
        this.signer = new LocalAccessUrlSigner(accessConfig != null ? accessConfig.signSecret() : null);
        this.initBaseDirectory();
    }

    @Override
    public void upload(InputStream inputStream, String filePath, String contentType, long contentLength) {
        try {
            var targetPath = this.resolve(filePath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件上传失败: {}", filePath, e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public InputStream download(String filePath) {
        try {
            var targetPath = this.resolve(filePath);
            if (!Files.exists(targetPath)) {
                throw new BusinessException("文件不存在");
            }
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            log.error("文件下载失败: {}", filePath, e);
            throw new BusinessException("文件下载失败");
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            var targetPath = this.resolve(filePath);
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.error("文件删除失败: {}", filePath, e);
            throw new BusinessException("文件删除失败");
        }
    }

    @Override
    public boolean exists(String filePath) {
        return Files.exists(this.resolve(filePath));
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    @Override
    public String getAccessUrl(String bucket, String filePath, Duration duration) {
        long expiresAt = Instant.now().plus(duration).getEpochSecond();
        String level = "LOCAL";
        String signature = signer.sign(filePath, expiresAt, level);
        String encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8);
        String endpoint = "/api/sys/file/access";
        return publicBaseUrl + endpoint
                + "?path=" + encodedPath
                + "&exp=" + expiresAt
                + "&level=" + level
                + "&sig=" + signature;
    }

    @Override
    public String extractKey(String url) {
        try {
            var query = URI.create(url).getQuery();
            if (query == null) {
                return url;
            }
            for (String param : query.split("&")) {
                if (param.startsWith("path=")) {
                    return URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.warn("从 URL 提取存储 key 失败，原样返回: {}", url);
        }
        return url;
    }

    /**
     * 解析完整路径并防止路径穿越
     */
    private Path resolve(String filePath) {
        var resolved = basePath.resolve(filePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new BusinessException("非法文件路径");
        }
        return resolved;
    }

    /**
     * 初始化根目录
     */
    private void initBaseDirectory() {
        try {
            Files.createDirectories(basePath);
            log.info("本地文件存储根目录: {}", basePath);
        } catch (IOException e) {
            throw new BusinessException("初始化文件存储目录失败: " + basePath);
        }
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
