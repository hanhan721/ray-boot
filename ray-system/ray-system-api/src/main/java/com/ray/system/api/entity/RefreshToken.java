package com.ray.system.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Refresh Token 实体
 */
@Data
@Accessors(chain = true)
@TableName("sys_refresh_token")
public class RefreshToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Token 的 SHA-256 哈希值
     */
    private String tokenHash;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 关联的会话摘要
     */
    private String sessionId;

    /** 一次登录的稳定标识，轮换记录共用此标识。 */
    private String loginSessionId;

    /** 已轮换的凭据仅供登出定位，不再允许续期。 */
    private Boolean rotated;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 设备信息
     */
    private String userAgent;

    /**
     * 登录 IP
     */
    private String ip;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
