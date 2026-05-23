package com.ray.system.service.impl;

import com.ray.common.core.exception.BusinessException;
import com.ray.common.security.util.SecurityUtils;
import com.ray.system.api.entity.SysUserColumnConfig;
import com.ray.system.api.vo.ColumnItem;
import com.ray.system.mapper.UserColumnConfigMapper;
import com.ray.system.service.UserColumnConfigService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * 用户列配置 Service 实现
 */
@Service
@RequiredArgsConstructor
public class UserColumnConfigServiceImpl implements UserColumnConfigService {

    private static final TypeReference<List<ColumnItem>> COLUMN_ITEM_LIST_TYPE = new TypeReference<>() {};

    private final UserColumnConfigMapper columnConfigMapper;
    private final JsonMapper jsonMapper;

    @Override
    @Nullable
    public List<ColumnItem> getByPageKey(String pageKey) {
        Long userId = this.requireUserId();
        SysUserColumnConfig config = columnConfigMapper.selectByUserAndPage(userId, pageKey);
        if (config == null) {
            return null;
        }
        return this.parseColumns(config.getColumns());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveByPageKey(String pageKey, List<ColumnItem> columns) {
        Long userId = this.requireUserId();
        String columnsJson = this.toJson(columns);

        SysUserColumnConfig existing = columnConfigMapper.selectByUserAndPage(userId, pageKey);
        if (existing != null) {
            existing.setColumns(columnsJson);
            columnConfigMapper.updateById(existing);
        } else {
            var config = new SysUserColumnConfig()
                    .setUserId(userId)
                    .setPageKey(pageKey)
                    .setColumns(columnsJson);
            columnConfigMapper.insert(config);
        }
    }

    @Override
    public void deleteByPageKey(String pageKey) {
        Long userId = this.requireUserId();
        columnConfigMapper.deleteByUserAndPage(userId, pageKey);
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        return userId;
    }

    private List<ColumnItem> parseColumns(String json) {
        return jsonMapper.readValue(json, COLUMN_ITEM_LIST_TYPE);
    }

    private String toJson(List<ColumnItem> columns) {
        return jsonMapper.writeValueAsString(columns);
    }
}
