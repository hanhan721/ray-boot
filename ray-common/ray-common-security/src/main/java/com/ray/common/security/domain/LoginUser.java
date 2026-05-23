package com.ray.common.security.domain;

import com.ray.common.core.domain.UserIdAware;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 登录用户信息
 *
 * @param userId    用户 ID
 * @param username  用户名
 * @param roles     角色标识集合
 * @param loginTime 登录时间
 * @param loginIp   登录 IP
 */
public record LoginUser(
        Long userId,
        String username,
        Set<String> roles,
        LocalDateTime loginTime,
        String loginIp
) implements UserIdAware {

    @Override
    public Long getUserId() {
        return this.userId;
    }
}
