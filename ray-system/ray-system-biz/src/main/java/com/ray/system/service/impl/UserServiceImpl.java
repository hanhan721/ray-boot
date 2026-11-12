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
import com.ray.system.api.reference.UserReferenceChecker;
import com.ray.system.mapper.UserMapper;
import com.ray.system.security.AuthorizationSessionCoordinator;
import com.ray.system.security.SuperAdminGuard;
import com.ray.system.service.DeptService;
import com.ray.system.service.UserDeptService;
import com.ray.system.service.UserRoleService;
import com.ray.system.service.UserService;
import com.ray.system.file.FileBindings;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.HashSet;
import java.util.stream.Collectors;

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
    private final FileBindings fileBindings;
    private final List<UserReferenceChecker> userReferenceCheckers;

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

    /** 新增用户并在同一事务中绑定头像；绑定失败时用户记录一并回滚。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SysUser user) {
        // 1.校验用户名唯一性，处理密码后再创建用户记录。
        this.checkUsernameUnique(user.getUsername(), null);

        // 加密密码
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 2.暂不写入头像外键，先取得用户 ID，供文件归属校验与绑定使用。
        var avatarId = user.getAvatarFileId();
        user.setAvatarFileId(null);
        super.save(user);
        // 3.统一绑定服务确认文件归属后，再写用户头像外键，保证两侧同事务一致。
        if (avatarId != null) {
            fileBindings.bind(avatarId, "USER_AVATAR", user.getId());
            baseMapper.updateAvatar(user.getId(), avatarId);
            user.setAvatarFileId(avatarId);
        }
    }

    /** 更新用户及头像引用；先绑定新文件，更新业务外键后再逻辑删除旧头像文件。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUser user) {
        // 1.禁用操作先取得授权协调锁，再校验并锁定用户行，保持跨流程锁顺序一致。
        if (Boolean.FALSE.equals(user.getEnabled())) {
            sessionCoordinator.lockForChange();
        }
        AssertUtils.notNull(user.getId(), "用户 ID 不能为空");
        superAdminGuard.checkUsers(Set.of(user.getId()));
        this.checkUsernameUnique(user.getUsername(), user.getId());
        var previous = this.getByIdForUpdate(user.getId());
        AssertUtils.notNull(previous, "用户不存在");
        // 2.比较新旧头像，按文件 ID 排序加锁，并在业务外键切换前绑定新文件。
        var avatarId = user.getAvatarFileId();
        boolean replaceAvatar = !Objects.equals(avatarId, previous.getAvatarFileId());
        if (replaceAvatar) {
            var avatarIds = new HashSet<Long>();
            if (avatarId != null) avatarIds.add(avatarId);
            if (previous.getAvatarFileId() != null) avatarIds.add(previous.getAvatarFileId());
            fileBindings.lockFiles(avatarIds);
            if (avatarId != null) fileBindings.bind(avatarId, "USER_AVATAR", user.getId());
        }
        // 3.通用用户更新暂时排除头像字段，空头像也由显式外键更新负责清除。
        user.setAvatarFileId(null);

        // 不允许通过此接口修改密码
        user.setPassword(null);

        // 检测是否禁用账号
        var needKick = Boolean.FALSE.equals(user.getEnabled());

        super.updateById(user);
        // 4.先切换真实业务外键，再释放旧文件；任一步失败均随用户更新回滚。
        if (replaceAvatar) {
            baseMapper.updateAvatar(user.getId(), avatarId);
            if (previous.getAvatarFileId() != null) fileBindings.release(previous.getAvatarFileId(), "USER_AVATAR", user.getId());
        }
        user.setAvatarFileId(avatarId);

        // 5.禁用账号后撤销所有设备会话，授权变更与数据库提交保持一致。
        if (needKick) {
            this.kickUser(user.getId());
        }
    }

    /** 删除用户及关联数据，并在业务事务中释放头像文件，不执行物理字节删除。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 1.依次取得授权协调锁、用户行锁及头像文件行锁，避免与并发替换交错。
        sessionCoordinator.lockForChange();
        superAdminGuard.checkUsers(ids);
        var previousUsers = ids.stream().sorted().map(this::getByIdForUpdate).filter(Objects::nonNull).toList();

        // 业务模块仍引用候选用户时整批拒绝，检查先于会话撤销、关联删除和文件释放。
        this.checkUserReferences(ids);

        fileBindings.lockFiles(previousUsers.stream().map(SysUser::getAvatarFileId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        sessionCoordinator.revokeUsers(ids);

        // 2.撤销会话并删除角色、部门关联，为用户逻辑删除保持业务完整性。
        userRoleService.deleteByUserIds(ids);
        userDeptService.deleteByUserIds(ids);

        // 3.先删除真实用户引用，再释放头像文件；文件变更随业务事务共同回滚。
        super.removeByIds(ids);
        for (var user : previousUsers) {
            if (user.getAvatarFileId() != null) fileBindings.release(user.getAvatarFileId(), "USER_AVATAR", user.getId());
        }
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

    /** 业务模块引用候选用户时整批拒绝；检查器只读业务数据，异常随删除事务回滚。 */
    private void checkUserReferences(Set<Long> ids) {
        var referenced = userReferenceCheckers.stream()
                .flatMap(checker -> checker.referencedUserIds(ids).stream())
                .collect(Collectors.toSet());
        AssertUtils.isTrue(referenced.isEmpty(), "用户仍被业务引用: " + referenced);
    }
}
