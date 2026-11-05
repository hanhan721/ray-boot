package com.ray.system.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ray.system.api.entity.SysUser;
import com.ray.system.api.file.FileReferenceChecker;
import com.ray.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

/** 以用户表的头像外键报告真实引用，不依赖文件表中的业务归属推断。 */
@Component
@RequiredArgsConstructor
public class UserAvatarReferenceChecker implements FileReferenceChecker {
    private final UserMapper users;
    /** 批量查询未删除用户的头像引用，按文件 ID 汇总对应 USER_AVATAR 业务记录。 */
    @Override
    public Map<Long, Set<Reference>> references(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        var result = new HashMap<Long, Set<Reference>>();
        // 1.只查询候选文件涉及的用户与头像字段，不锁定业务行。
        users.selectList(Wrappers.<SysUser>lambdaQuery().select(SysUser::getId, SysUser::getAvatarFileId)
                .in(SysUser::getAvatarFileId, ids)).forEach(user -> result
                .computeIfAbsent(user.getAvatarFileId(), _ -> new HashSet<>())
                .add(new Reference("USER_AVATAR", user.getId())));
        // 2.保留同一文件的全部真实引用，让绑定、删除流程能发现归属不一致。
        return result;
    }
}
