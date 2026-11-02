package com.ray.common.security.service;

import com.ray.common.security.domain.UserAuthorization;

/** 由业务模块提供当前授权，安全模块无需反向依赖角色、权限实体。 */
public interface UserAuthorizationProvider {

    UserAuthorization getAuthorization(Long userId);
}
