package com.ray.common.storage.config;

import com.ray.common.storage.storage.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    @ConditionalOnProperty(name="ray.storage.storage-type", havingValue="LOCAL", matchIfMissing=true)
    public StorageService localStorageService(StorageProperties properties) { return new LocalStorageService(properties); }

    @Bean
    @ConditionalOnMissingBean(StorageService.class)
    @ConditionalOnProperty(name="ray.storage.storage-type", havingValue="RUST_FS")
    public StorageService rustFSStorageService(StorageProperties properties) { return new RustFSStorageService(properties); }
}
