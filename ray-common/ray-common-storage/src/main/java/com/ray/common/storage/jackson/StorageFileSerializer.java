package com.ray.common.storage.jackson;

import com.ray.common.storage.annotation.AccessLevel;
import com.ray.common.storage.annotation.StorageFile;
import com.ray.common.storage.service.FileNameResolver;
import com.ray.common.storage.storage.StorageService;
import com.ray.common.valkey.util.ValkeyUtils;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdScalarSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Jackson 序列化器：存储 key 自动转结构化文件信息 {path, url, name}
 */
public class StorageFileSerializer extends StdScalarSerializer<String> {

    private static final String NAME_CACHE_PREFIX = "storage:name:";
    private static final Duration NAME_CACHE_TTL = Duration.ofDays(7);

    private final StorageService storageService;
    private final ValkeyUtils valkeyUtils;
    private final ObjectProvider<FileNameResolver> fileNameResolverProvider;
    private final AccessLevel level;
    private final boolean multiple;
    private final boolean resolveName;

    /** 无参构造器：降级用 */
    public StorageFileSerializer() {
        this(null, null, null, AccessLevel.PRIVATE, false, false);
    }

    /**
     * Bean 注册构造器：接收 ObjectProvider 延迟解析 FileNameResolver，
     * 避免自动配置类执行时 FileNameResolver 实现尚未注册
     */
    public StorageFileSerializer(StorageService storageService, ValkeyUtils valkeyUtils,
                                 ObjectProvider<FileNameResolver> fileNameResolverProvider) {
        this(storageService, valkeyUtils, fileNameResolverProvider, AccessLevel.PRIVATE, false, false);
    }

    private StorageFileSerializer(StorageService storageService, ValkeyUtils valkeyUtils,
                                  ObjectProvider<FileNameResolver> fileNameResolverProvider,
                                  AccessLevel level, boolean multiple, boolean resolveName) {
        super(String.class);
        this.storageService = storageService;
        this.valkeyUtils = valkeyUtils;
        this.fileNameResolverProvider = fileNameResolverProvider;
        this.level = level;
        this.multiple = multiple;
        this.resolveName = resolveName;
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        if (property != null) {
            StorageFile annotation = property.getAnnotation(StorageFile.class);
            if (annotation != null && (annotation.level() != this.level
                    || annotation.multiple() != this.multiple
                    || annotation.resolveName() != this.resolveName)) {
                return new StorageFileSerializer(this.storageService, this.valkeyUtils,
                        this.fileNameResolverProvider, annotation.level(), annotation.multiple(),
                        annotation.resolveName());
            }
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
        if (value == null || value.isBlank()) {
            gen.writeNull();
            return;
        }
        if (this.multiple) {
            List<String> keys = splitKeys(value);
            if (this.resolveName) {
                preloadNames(keys);
            }
            gen.writeStartArray();
            for (String key : keys) {
                writeFileInfo(gen, key);
            }
            gen.writeEndArray();
        } else {
            writeFileInfo(gen, value.trim());
        }
    }

    private void writeFileInfo(JsonGenerator gen, String key) {
        gen.writeStartObject();
        gen.writeName("path");
        gen.writeString(key);
        gen.writeName("url");
        gen.writeString(resolveUrl(key));
        if (this.resolveName) {
            gen.writeName("name");
            String name = doResolveName(key);
            if (name != null) {
                gen.writeString(name);
            } else {
                gen.writeNull();
            }
        }
        gen.writeEndObject();
    }

    private String resolveUrl(String key) {
        if (storageService == null) {
            return key;
        }
        return storageService.getAccessUrl(storageService.getBucket(), key, level.getDuration());
    }

    private String doResolveName(String key) {
        if (valkeyUtils != null) {
            String cached = valkeyUtils.get(NAME_CACHE_PREFIX + key, String.class);
            if (cached != null) {
                return cached;
            }
        }
        FileNameResolver resolver = getFileNameResolver();
        if (resolver == null) {
            return null;
        }
        Map<String, String> result = resolver.resolve(List.of(key));
        String name = result.get(key);
        if (name != null && valkeyUtils != null) {
            valkeyUtils.set(NAME_CACHE_PREFIX + key, name, NAME_CACHE_TTL.getSeconds());
        }
        return name;
    }

    private void preloadNames(List<String> keys) {
        FileNameResolver resolver = getFileNameResolver();
        if (resolver == null || valkeyUtils == null) {
            return;
        }
        List<String> missing = keys.stream()
                .filter(k -> valkeyUtils.get(NAME_CACHE_PREFIX + k, String.class) == null)
                .toList();
        if (missing.isEmpty()) {
            return;
        }
        Map<String, String> names = resolver.resolve(missing);
        names.forEach((k, v) -> valkeyUtils.set(NAME_CACHE_PREFIX + k, v, NAME_CACHE_TTL.getSeconds()));
    }

    private FileNameResolver getFileNameResolver() {
        return fileNameResolverProvider != null ? fileNameResolverProvider.getIfAvailable() : null;
    }

    private List<String> splitKeys(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
