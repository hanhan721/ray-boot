package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.ray.system.api.entity.SysUser;
import com.ray.system.api.entity.SysUserDept;
import com.ray.system.mapper.UserDeptMapper;
import com.ray.system.service.UserDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户部门关联 Service 实现
 */
@Service
@RequiredArgsConstructor
public class UserDeptServiceImpl extends ServiceImpl<UserDeptMapper, SysUserDept> implements UserDeptService {

    @Override
    public Set<Long> getDeptIdsByUserId(Long userId) {
        var wrapper = Wrappers.<SysUserDept>lambdaQuery()
                .eq(SysUserDept::getUserId, userId)
                .select(SysUserDept::getDeptId);
        return super.list(wrapper).stream()
                .map(SysUserDept::getDeptId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> getUserIdsByDeptId(Long deptId) {
        var wrapper = Wrappers.<SysUserDept>lambdaQuery()
                .eq(SysUserDept::getDeptId, deptId)
                .select(SysUserDept::getUserId);
        return super.list(wrapper).stream()
                .map(SysUserDept::getUserId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserId(Long userId) {
        var wrapper = Wrappers.<SysUserDept>lambdaQuery()
                .eq(SysUserDept::getUserId, userId);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserIds(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        var wrapper = Wrappers.<SysUserDept>lambdaQuery()
                .in(SysUserDept::getUserId, userIds);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDeptId(Long deptId) {
        var wrapper = Wrappers.<SysUserDept>lambdaQuery()
                .eq(SysUserDept::getDeptId, deptId);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDeptIds(Collection<Long> deptIds) {
        if (CollectionUtils.isEmpty(deptIds)) {
            return;
        }
        var wrapper = Wrappers.<SysUserDept>lambdaQuery()
                .in(SysUserDept::getDeptId, deptIds);
        super.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserDepts(Long userId, Set<Long> deptIds) {
        // 先删除原有关联
        this.deleteByUserId(userId);

        // 批量插入新关联
        if (!CollectionUtils.isEmpty(deptIds)) {
            List<SysUserDept> userDepts = deptIds.stream()
                    .map(deptId -> new SysUserDept()
                            .setUserId(userId)
                            .setDeptId(deptId))
                    .toList();
            super.saveBatch(userDepts);
        }
    }

    @Override
    public boolean existsByDeptIds(Collection<Long> deptIds) {
        if (CollectionUtils.isEmpty(deptIds)) {
            return false;
        }
        var query = new MPJLambdaWrapper<SysUserDept>()
                .innerJoin(SysUser.class, SysUser::getId, SysUserDept::getUserId)
                .in(SysUserDept::getDeptId, deptIds)
                .eq(SysUser::getDeleted, false);
        var count = baseMapper.selectJoinCount(query);
        return count != null && count > 0;
    }
}
