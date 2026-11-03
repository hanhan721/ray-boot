package com.ray.system.task;

import com.ray.common.core.annotation.ScheduleTask;
import com.ray.common.security.constant.AuthCacheConstants;
import com.ray.common.security.service.OnlineSessionIndex;
import com.ray.common.valkey.util.ValkeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** 升级后手动执行一次，为既有在线会话补齐管理索引。 */
@Slf4j
@Component
@ScheduleTask
@RequiredArgsConstructor
public class OnlineSessionIndexTask {
    private final ValkeyUtils valkeyUtils;
    private final OnlineSessionIndex onlineSessionIndex;

    public void rebuild() {
        var prefix = AuthCacheConstants.SESSION_KEY_PREFIX;
        valkeyUtils.scanEach(prefix + "*", 200, keys -> {
            var fields = valkeyUtils.pipelinedHMGet(keys, List.of(AuthCacheConstants.FIELD_LOGIN_TIME));
            for (int i = 0; i < keys.size(); i++) {
                var value = fields.get(i).get(AuthCacheConstants.FIELD_LOGIN_TIME);
                if (value instanceof String time) {
                    try {
                        onlineSessionIndex.add(keys.get(i).substring(prefix.length()), LocalDateTime.parse(time));
                    } catch (java.time.format.DateTimeParseException exception) {
                        log.warn("跳过登录时间无效的在线会话索引");
                    }
                }
            }
        });
    }
}
