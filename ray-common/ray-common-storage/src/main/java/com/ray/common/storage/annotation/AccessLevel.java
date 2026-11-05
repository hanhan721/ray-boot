package com.ray.common.storage.annotation;

import lombok.Getter;

import java.time.Duration;

/**
 * 链接寿命档位，不授予访问权限，也不允许由请求参数提升档位。
 */
@Getter
public enum AccessLevel {

    PRIVATE(Duration.ofMinutes(15)),
    PROTECTED(Duration.ofHours(1)),
    PUBLIC(Duration.ofDays(7));

    private final Duration duration;

    AccessLevel(Duration duration) {
        this.duration = duration;
    }

}
