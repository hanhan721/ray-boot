package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.core.util.AssertUtils;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.system.api.domain.UserDetailVO;
import com.ray.system.api.domain.UserPageQuery;
import com.ray.system.api.entity.SysUser;
import com.ray.system.mapper.UserMapper;
import com.ray.system.security.AuthorizationSessionCoordinator;
import com.ray.system.security.SuperAdminGuard;
import com.ray.system.service.DeptService;
import com.ray.system.service.UserDeptService;
import com.ray.system.service.UserRoleService;
import com.ray.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * 用户 Service 实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    private final UserRoleService userRoleService;
    private final UserDeptService userDeptService;
    private final DeptService deptService;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationSessionCoordinator sessionCoordinator;
    private final SuperAdminGuard superAdminGuard;

    @Override
    public IPage<SysUser> page(UserPageQuery query) {
        return super.page(PageUtils.buildPage(query), QueryWrapperHelper.build(query));
    }

    @Override
    public UserDetailVO getUserDetail(Long id) {
        var user = super.getById(id);
        AssertUtils.notNull(user, "用户不存在");
        var roleIds = userRoleService.getRoleIdsByUserId(id);
        var deptIds = userDeptService.getDeptIdsByUserId(id);
        return new UserDetailVO(user, roleIds, deptIds);
    }

    @Override
    public SysUser getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public SysUser getByIdForUpdate(Long id) {
        return baseMapper.selectByIdForUpdate(id);
    }

    @Override
    public SysUser getByUsername(String username) {
        return super.getOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SysUser user) {
        this.checkUsernameUnique(user.getUsername(), null);

        // 加密密码
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        super.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user) {
        if (Boolean.FALSE.equals(user.getEnabled())) {
            sessionCoordinator.lockForChange();
        }
        AssertUtils.notNull(user.getId(), "用户 ID 不能为空");
        superAdminGuard.checkUsers(Set.of(user.getId()));
        this.checkUsernameUnique(user.getUsername(), user.getId());

        // 不允许通过此接口修改密码
        user.setPassword(null);

        // 检测是否禁用账号
        var needKick = Boolean.FALSE.equals(user.getEnabled());

        super.updateById(user);

        // 禁用账号后踢掉该用户所有设备
        if (needKick) {
            this.kickUser(user.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        sessionCoordinator.lockForChange();
        superAdminGuard.checkUsers(ids);
        sessionCoordinator.revokeUsers(ids);

        // 删除用户角色、部门关联
        userRoleService.deleteByUserIds(ids);
        userDeptService.deleteByUserIds(ids);

        super.removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        sessionCoordinator.lockForChange();
        AssertUtils.notNull(id, "用户 ID 不能为空");
        AssertUtils.notEmpty(password, "密码不能为空");
        superAdminGuard.checkUsers(Set.of(id));

        var user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(password));
        super.updateById(user);

        // 密码重置后踢掉该用户所有设备
        this.kickUser(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, Set<Long> roleIds) {
        sessionCoordinator.lockForChange();
        AssertUtils.notNull(userId, "用户 ID 不能为空");
        AssertUtils.notNull(this.getByIdForUpdate(userId), "用户不存在");
        superAdminGuard.checkUsers(Set.of(userId));
        superAdminGuard.checkRoles(roleIds);

        userRoleService.saveUserRoles(userId, roleIds);

        sessionCoordinator.revokeUsers(Set.of(userId));
    }

    @Override
    public Set<Long> getRoleIds(Long userId) {
        return userRoleService.getRoleIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignDepts(Long userId, Set<Long> deptIds) {
        AssertUtils.notNull(userId, "用户 ID 不能为空");
        // 与用户删除共用用户行锁，避免删除完成后又写入部门关联。
        AssertUtils.notNull(this.getByIdForUpdate(userId), "用户不存在");
        superAdminGuard.checkUsers(Set.of(userId));
        if (!CollectionUtils.isEmpty(deptIds)) {
            // 锁定目标部门行，并与部门删除互斥，避免写入已删除部门。
            var existingDepts = deptService.listByIdsForUpdate(deptIds);
            AssertUtils.isTrue(existingDepts.size() == deptIds.size(), "部门不存在或已删除");
        }
        userDeptService.saveUserDepts(userId, deptIds);
    }

    @Override
    public Set<Long> getDeptIds(Long userId) {
        return userDeptService.getDeptIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(Long id, boolean enabled) {
        if (!enabled) {
            sessionCoordinator.lockForChange();
        }
        AssertUtils.notNull(id, "用户 ID 不能为空");
        superAdminGuard.checkUsers(Set.of(id));
        var user = new SysUser();
        user.setId(id);
        user.setEnabled(enabled);
        super.updateById(user);

        // 禁用账号后踢掉该用户所有设备
        if (!enabled) {
            this.kickUser(id);
        }
    }

    /**
     * 校验用户名唯一性
     */
    private void checkUsernameUnique(String username, Long excludeId) {
        if (username == null || username.isBlank()) {
            return;
        }
        var wrapper = Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId);
        AssertUtils.isTrue(!super.exists(wrapper), "用户名已存在");
    }

    /** 撤销全部设备，数据库回滚时保留原会话。 */
    private void kickUser(Long userId) {
        sessionCoordinator.revokeUsers(Set.of(userId));
    }
}
