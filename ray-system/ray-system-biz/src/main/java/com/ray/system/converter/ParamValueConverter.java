package com.ray.system.converter;

import com.ray.system.api.enums.ParamType;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 参数值类型转换器
 * <p>
 * 负责在数据库字符串和业务类型之间进行转换和校验
 */
@Component
@RequiredArgsConstructor
public class ParamValueConverter {

    private final JsonMapper jsonMapper;

    /**
     * 将字符串值转换为目标类型
     *
     * @param value 字符串值
     * @param paramType 目标参数类型
     * @return 转换后的值
     */
    public Object convert(String value, ParamType paramType) {
        return switch (paramType) {
            case STRING -> value;
            case INTEGER -> {
                try {
                    yield Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("参数值无法转换为整数：" + value);
                }
            }
            case BOOLEAN -> {
                if ("true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value)) {
                    yield true;
                } else if ("false".equalsIgnoreCase(value) || "0".equals(value) || "no".equalsIgnoreCase(value)) {
                    yield false;
                } else {
                    throw new IllegalArgumentException("参数值无法转换为布尔值：" + value);
                }
            }
            case JSON -> {
                try {
                    yield jsonMapper.readValue(value, Object.class);
                } catch (Exception e) {
                    throw new IllegalArgumentException("参数值不是有效的 JSON：" + value);
                }
            }
            case STRING_LIST -> Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        };
    }

    /**
     * 校验参数值是否与类型匹配
     *
     * @param value 字符串值
     * @param paramType 参数类型
     * @return 是否有效
     */
    public boolean isValid(String value, ParamType paramType) {
        try {
            convert(value, paramType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取类型校验错误信息
     *
     * @param value 字符串值
     * @param paramType 参数类型
     * @return 错误信息，如果有效则返回 null
     */
    public String getValidationError(String value, ParamType paramType) {
        try {
            convert(value, paramType);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * 将值转换为字符串用于存储
     *
     * @param value 原始值
     * @return 字符串值
     */
    public String toStorageString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        if (value instanceof Boolean b) {
            return b ? "true" : "false";
        }
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
