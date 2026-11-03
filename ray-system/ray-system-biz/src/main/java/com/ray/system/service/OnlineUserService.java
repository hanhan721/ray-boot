package com.ray.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.system.api.domain.OnlineUserPageQuery;
import com.ray.system.api.domain.OnlineUserVO;

/**
 * 在线用户 Service
 */
public interface OnlineUserService {

    /**
     * 按登录时间倒序分页查询在线会话。
     */
    IPage<OnlineUserVO> page(OnlineUserPageQuery query);

    /**
     * 强制下线（单个会话）
     *
     * @param sessionId 在线列表返回的管理会话标识（非认证凭据）
     */
    void kickBySessionId(String sessionId);

    /**
     * 强制下线（用户所有会话）
     *
     * @param userId 用户 ID
     */
    void kickByUserId(Long userId);
}
