package com.ray.system.file;

import com.ray.common.storage.storage.StorageService;
import com.ray.system.mapper.FileObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/** 启动时核对历史活跃对象的存储身份，不隐式迁移或重解释旧 key。 */
@Component
@RequiredArgsConstructor
public class FileStorageGuard implements SmartInitializingSingleton {
    private final FileObjectMapper objects;
    private final StorageService storage;

    /** 所有单例初始化后检查 AVAILABLE/DELETING 对象，发现身份不一致则快速失败。 */
    @Override
    public void afterSingletonsInstantiated() {
        if (objects.mismatched(storage.getStorageId(), storage.getStorageType().name(), storage.getBucket())) {
            throw new IllegalStateException("历史文件存储身份与当前配置不符，请先迁移文件");
        }
    }
}
