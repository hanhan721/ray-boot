package com.ray.common.storage.jackson;

import com.ray.common.storage.storage.StorageService;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

import java.util.StringJoiner;

/**
 * Jackson 反序列化器：接受字符串、对象或数组，统一还原为逗号分隔的存储 key
 */
public class StorageFileDeserializer extends StdScalarDeserializer<String> {

    private final StorageService storageService;

    /** 无参构造器：降级用 */
    public StorageFileDeserializer() {
        this(null);
    }

    public StorageFileDeserializer(StorageService storageService) {
        super(String.class);
        this.storageService = storageService;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) {
        return switch (p.currentToken()) {
            case VALUE_STRING -> extractFromString(p.getString());
            case START_OBJECT -> extractFromObject(p);
            case START_ARRAY -> extractFromArray(p);
            case VALUE_NULL -> null;
            default -> p.getString();
        };
    }

    private String extractFromString(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.contains(",")) {
            var joiner = new StringJoiner(",");
            for (String part : value.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    joiner.add(stripUrl(trimmed));
                }
            }
            return joiner.toString();
        }
        return stripUrl(value);
    }

    private String extractFromObject(JsonParser p) {
        String path = null;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();
            if ("path".equals(fieldName)) {
                path = p.getString();
            }
        }
        return path != null ? stripUrl(path) : null;
    }

    private String extractFromArray(JsonParser p) {
        var joiner = new StringJoiner(",");
        while (p.nextToken() != JsonToken.END_ARRAY) {
            String key = switch (p.currentToken()) {
                case VALUE_STRING -> stripUrl(p.getString());
                case START_OBJECT -> extractFromObject(p);
                default -> null;
            };
            if (key != null && !key.isBlank()) {
                joiner.add(key);
            }
        }
        String result = joiner.toString();
        return result.isEmpty() ? null : result;
    }

    private String stripUrl(String value) {
        if (storageService != null && (value.startsWith("http://") || value.startsWith("https://"))) {
            return storageService.extractKey(value);
        }
        return value;
    }
}
