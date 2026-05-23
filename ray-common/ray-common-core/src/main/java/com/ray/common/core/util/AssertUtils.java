package com.ray.common.core.util;

import com.ray.common.core.constant.ResultCode;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.core.exception.ErrorCode;

import java.util.Collection;
import java.util.Map;

/**
 * 断言工具类
 * <p>
 * 条件不满足时抛出 BusinessException
 */
public final class AssertUtils {

    private AssertUtils() {
    }

    public static void isTrue(boolean expression, String msg) {
        if (!expression) {
            throw new BusinessException(msg);
        }
    }

    public static void isTrue(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new BusinessException(errorCode);
        }
    }

    public static void isFalse(boolean expression, String msg) {
        isTrue(!expression, msg);
    }

    public static void notNull(Object object, String msg) {
        if (object == null) {
            throw new BusinessException(msg);
        }
    }

    public static void notNull(Object object, ErrorCode errorCode) {
        if (object == null) {
            throw new BusinessException(errorCode);
        }
    }

    public static void isNull(Object object, String msg) {
        if (object != null) {
            throw new BusinessException(msg);
        }
    }

    public static void notEmpty(String str, String msg) {
        if (str == null || str.isBlank()) {
            throw new BusinessException(msg);
        }
    }

    public static void notEmpty(Collection<?> collection, String msg) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(msg);
        }
    }

    public static void notEmpty(Map<?, ?> map, String msg) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(msg);
        }
    }

    public static void notEmpty(Object[] array, String msg) {
        if (array == null || array.length == 0) {
            throw new BusinessException(msg);
        }
    }

    public static void notFound(Object object) {
        if (object == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    public static void notFound(Object object, String msg) {
        if (object == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, msg);
        }
    }
}
