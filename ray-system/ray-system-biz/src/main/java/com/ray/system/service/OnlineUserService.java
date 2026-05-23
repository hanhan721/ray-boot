package com.ray.system.service;

import com.ray.system.api.domain.OnlineUserVO;

import java.util.List;

/**
 * 在线用户 Service
 */
public interface OnlineUserService {

    /**
     * 查询在线用户列表
     *
     * @param username 用户名（模糊匹配，可为空）
     * @param loginIp  登录 IP（模糊匹配，可为空）
     * @return 在线用户列表
     */
    List<OnlineUserVO> list(String username, String loginIp);

    /**
     * 强制下线（单个会话）
     *
     * @param tokenId Access Token UUID
     */
    void kickByToken(String tokenId);

    /**
     * 强制下线（用户所有会话）
     *
     * @param userId 用户 ID
     */
    void kickByUserId(Long userId);
}
