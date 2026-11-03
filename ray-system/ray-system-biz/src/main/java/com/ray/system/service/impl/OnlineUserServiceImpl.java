package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.service.OnlineSessionIndex;
import com.ray.common.security.service.TokenService;
import com.ray.common.valkey.util.ValkeyUtils;
import com.ray.system.api.domain.OnlineUserPageQuery;
import com.ray.system.api.domain.OnlineUserVO;
import com.ray.system.security.AuthorizationSessionCoordinator;
import com.ray.system.service.OnlineUserService;
import com.ray.system.service.RefreshTokenService;
import com.ray.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 在线用户 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final List<String> DISPLAY_FIELDS = List.of(AuthCacheConstants.FIELD_USER_ID,
            AuthCacheConstants.FIELD_USERNAME, AuthCacheConstants.FIELD_NICKNAME, AuthCacheConstants.FIELD_LOGIN_IP,
            AuthCacheConstants.FIELD_BROWSER, AuthCacheConstants.FIELD_OS, AuthCacheConstants.FIELD_LOGIN_TIME);

    private final ValkeyUtils valkeyUtils;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final AuthorizationSessionCoordinator sessionCoordinator;
    private final OnlineSessionIndex onlineSessionIndex;

    @Override
    public IPage<OnlineUserVO> page(OnlineUserPageQuery query) {
        var page = PageUtils.<OnlineUserVO>buildPage(query);
        var indexed = onlineSessionIndex.page(query.getStartTime(), query.getEndTime(),
                (page.getCurrent() - 1) * page.getSize(), query.getPageSize());
        page.setTotal(indexed.total());
        var sessionIds = indexed.sessionIds();
        var fieldsList = valkeyUtils.pipelinedHMGet(sessionIds.stream()
                .map(id -> AuthCacheConstants.SESSION_KEY_PREFIX + id).toList(), DISPLAY_FIELDS);
        var stale = IntStream.range(0, sessionIds.size()).filter(i -> CollectionUtils.isEmpty(fieldsList.get(i)))
                .mapToObj(sessionIds::get).toList();
        onlineSessionIndex.remove(stale);
        page.setTotal(Math.max(0, page.getTotal() - stale.size()));
        page.setRecords(IntStream.range(0, sessionIds.size())
                .mapToObj(i -> {
                    var fields = fieldsList.get(i);
                    if (CollectionUtils.isEmpty(fields)) {
                        return null;
                    }
                    return this.buildOnlineUserVO(sessionIds.get(i), fields);
                })
                .filter(Objects::nonNull)
                .toList());
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickBySessionId(String sessionId) {
        sessionCoordinator.lockForChange();
        var sessionKey = AuthCacheConstants.SESSION_KEY_PREFIX + sessionId;
        var fields = valkeyUtils.hGetAll(sessionKey);

        if (!CollectionUtils.isEmpty(fields)) {
            var userId = ((Number) fields.get(AuthCacheConstants.FIELD_USER_ID)).longValue();
            // 与续期共用用户行锁，使等待中的刷新在提交后重新验证到失效状态。
            userService.getByIdForUpdate(userId);
            tokenService.revokeAfterCommit(Map.of(userId, Set.of(sessionId)));
            log.info("强制下线单个会话: userId={}", userId);
        }
        // 协调锁阻止并发续期；Access 已过期时仍须撤销对应 Refresh Token。
        refreshTokenService.deleteBySessionId(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickByUserId(Long userId) {
        sessionCoordinator.lockForChange();
        sessionCoordinator.revokeUsers(Set.of(userId));

        log.info("强制下线用户所有会话: userId={}", userId);
    }

    private OnlineUserVO buildOnlineUserVO(String sessionId, Map<Object, Object> fields) {
        try {
            var userId = ((Number) fields.get(AuthCacheConstants.FIELD_USER_ID)).longValue();
            var tokenUsername = (String) fields.get(AuthCacheConstants.FIELD_USERNAME);
            var nickname = (String) fields.getOrDefault(AuthCacheConstants.FIELD_NICKNAME, "");
            var loginIp = (String) fields.get(AuthCacheConstants.FIELD_LOGIN_IP);
            var browser = (String) fields.getOrDefault(AuthCacheConstants.FIELD_BROWSER, "Unknown");
            var os = (String) fields.getOrDefault(AuthCacheConstants.FIELD_OS, "Unknown");
            var loginTimeStr = (String) fields.get(AuthCacheConstants.FIELD_LOGIN_TIME);
            var loginTime = LocalDateTime.parse(loginTimeStr);

            return new OnlineUserVO(sessionId, userId, tokenUsername, nickname, loginIp, browser, os, loginTime);
        } catch (Exception e) {
            log.warn("解析在线用户信息失败");
            return null;
        }
    }
}
