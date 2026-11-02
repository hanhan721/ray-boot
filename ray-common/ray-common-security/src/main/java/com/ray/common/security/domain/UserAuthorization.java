package com.ray.common.security.domain;

import java.util.Set;

/** Token 签发时的不可变授权快照；菜单按需查询。 */
public record UserAuthorization(Set<String> roles, Set<String> permissions) {
    public UserAuthorization {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}
