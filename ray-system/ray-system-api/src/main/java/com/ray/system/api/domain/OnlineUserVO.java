package com.ray.system.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 在线用户信息
 *
 * @param tokenId   管理会话标识，不能用于认证
 * @param userId    用户 ID
 * @param username  用户名
 * @param nickname  昵称
 * @param loginIp   登录 IP
 * @param browser   浏览器
 * @param os        操作系统
 * @param loginTime 登录时间
 */
@Schema(description = "在线用户信息")
public record OnlineUserVO(
        @Schema(description = "管理会话标识（非认证凭据）")
        String tokenId,

        @Schema(description = "用户 ID")
        Long userId,

        @Schema(description = "用户名")
        String username,

        @Schema(description = "昵称")
        String nickname,

        @Schema(description = "登录 IP")
        String loginIp,

        @Schema(description = "浏览器")
        String browser,

        @Schema(description = "操作系统")
        String os,

        @Schema(description = "登录时间")
        LocalDateTime loginTime
) {
}
