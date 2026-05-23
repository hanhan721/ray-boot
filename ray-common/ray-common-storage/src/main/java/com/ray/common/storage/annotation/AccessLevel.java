package com.ray.common.storage.annotation;

import lombok.Getter;

import java.time.Duration;

/**
 * 访问级别
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
