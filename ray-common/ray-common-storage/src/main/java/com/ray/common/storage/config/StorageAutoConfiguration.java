package com.ray.common.storage.config;

import com.ray.common.storage.jackson.StorageFileDeserializer;
import com.ray.common.storage.jackson.StorageFileSerializer;
import com.ray.common.storage.service.FileNameResolver;
import com.ray.common.storage.storage.LocalStorageService;
import com.ray.common.storage.storage.RustFSStorageService;
import com.ray.common.storage.storage.StorageService;
import com.ray.common.valkey.util.ValkeyUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储自动配置
 * <p>
 * 序列化器/反序列化器注册为 Spring Bean，Spring 的 HandlerInstantiator
 * 会从容器中获取对应实例，使 {@code @JsonSerialize(using=...)} 注解能使用带依赖的 Bean。
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    // ==================== StorageService ====================

    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    @ConditionalOnProperty(name = "ray.storage.storage-type", havingValue = "LOCAL", matchIfMissing = true)
    public StorageService localStorageService(StorageProperties properties) {
        return new LocalStorageService(properties);
    }

    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    @ConditionalOnProperty(name = "ray.storage.storage-type", havingValue = "RUST_FS")
    public StorageService rustFSStorageService(StorageProperties properties) {
        return new RustFSStorageService(properties);
    }

    // ==================== Jackson Serializer/Deserializer ====================

    @Bean
    StorageFileSerializer storageFileSerializer(StorageService storageService,
                                                ObjectProvider<ValkeyUtils> valkeyUtils,
                                                ObjectProvider<FileNameResolver> fileNameResolver) {
        return new StorageFileSerializer(storageService, valkeyUtils.getIfAvailable(), fileNameResolver);
    }

    @Bean
    StorageFileDeserializer storageFileDeserializer(StorageService storageService) {
        return new StorageFileDeserializer(storageService);
    }
}
