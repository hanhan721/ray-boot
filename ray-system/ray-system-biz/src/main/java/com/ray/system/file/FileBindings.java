package com.ray.system.file;

import com.ray.common.core.exception.BusinessException;
import com.ray.common.security.evaluator.PermissionEvaluator;
import com.ray.common.security.util.SecurityUtils;
import com.ray.common.storage.storage.StorageService;
import com.ray.system.api.entity.SysFile;
import com.ray.system.api.file.FileBindingService;
import com.ray.system.api.file.FileReferenceChecker;
import com.ray.system.mapper.FileMapper;
import com.ray.system.mapper.FileObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 必须由已完成业务记录鉴权的 Service 调用，并与业务更新共用事务。 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class FileBindings implements FileBindingService {
    private final FileMapper files;
    private final FileObjectMapper objects;
    private final StorageService storage;
    private final PermissionEvaluator permissions;
    private final List<FileReferenceChecker> checkers;

    /** 在当前业务事务中按文件 ID 排序取得行锁，协调多文件替换与删除并减少死锁。 */
    public void lockFiles(Set<Long> ids) {
        ids.stream().sorted().forEach(id -> this.require(files.lock(id)));
    }

    /**
     * 将逻辑文件绑定到唯一业务记录；业务 Service 须先完成记录鉴权。
     * 这里只维护文件归属，真实业务引用必须由调用方在同一事务中写入。
     */
    public void bind(long fileId, String bizType, long bizId) {
        // 1.校验业务类型，锁定逻辑文件并检查上传者归属或文件管理权限。
        if (bizType == null || bizType.isBlank() || bizType.length() > 50) throw new BusinessException("非法文件业务类型");
        var file = this.require(files.lock(fileId));
        this.checkOwner(file, "system:file:manage");
        // 2.确认物理对象仍可用且属于当前存储，禁止绑定已进入回收的对象。
        var object = objects.selectById(file.getObjectId());
        if (object == null || !"AVAILABLE".equals(object.getStatus())
                || !storage.getStorageId().equals(object.getStorageId())) throw new BusinessException("文件对象不可用");
        // 3.核对文件归属与真实业务引用，只有完全一致的重复绑定才可直接返回。
        var reference = new FileReferenceChecker.Reference(bizType, bizId);
        var actual = this.actual(fileId);
        if (file.getBizType() != null) {
            if (!reference.equals(new FileReferenceChecker.Reference(file.getBizType(), file.getBizId()))
                    || !actual.equals(Set.of(reference))) throw new BusinessException("文件已绑定或业务引用异常");
            return;
        }
        if (!actual.isEmpty()) throw new BusinessException("文件业务归属与真实引用不一致");
        // 4.写入新归属，后续业务引用写入失败时由同一业务事务回滚。
        files.bind(fileId, bizType, bizId, SecurityUtils.getUserId());
    }

    /** 业务引用已移除后调用；复核归属后逻辑删除文件，与业务变更共同提交或回滚。 */
    public void release(long fileId, String bizType, long bizId) {
        // 1.锁定逻辑文件并确认释放方与原业务归属一致。
        var file = this.require(files.lock(fileId));
        if (!Objects.equals(file.getBizType(), bizType) || !Objects.equals(file.getBizId(), bizId)) {
            throw new BusinessException("文件业务归属异常");
        }
        // 2.确认真实业务引用已移除；文件删除不取得 hash 锁，也不立即删除字节。
        if (!this.actual(fileId).isEmpty()) throw new BusinessException("文件仍有业务引用");
        files.markDeleted(fileId, SecurityUtils.getUserId());
    }

    /** 对无业务引用的文件执行阻断式整批逻辑删除，任一目标不满足条件则整批拒绝。 */
    public void delete(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        // 1.按 ID 排序取得逻辑行锁；不取得 hash 锁。
        var locked = ids.stream().sorted().map(id -> this.require(files.lock(id))).toList();
        // 2.先校验全部目标的操作权限和真实业务引用，避免部分删除。
        for (var file : locked) {
            this.checkOwner(file, "system:file:remove");
            this.requireUnbound(file);
        }
        // 3.所有校验通过后统一标记删除，物理对象由 GC 后续观察和回收。
        locked.forEach(f -> files.markDeleted(f.getId(), SecurityUtils.getUserId()));
    }

    /** 行锁内复查未绑定文件的过期条件，避免与业务绑定、主动删除并发冲突。 */
    public boolean expire(long id, java.time.LocalDateTime cutoff) {
        // 1.锁定后重新检查删除标记、归属和创建时间，不直接信任候选扫描结果。
        var file = files.lock(id);
        if (file == null || Boolean.TRUE.equals(file.getDeleted()) || file.getBizType() != null
                || !file.getCreateTime().isBefore(cutoff)) return false;
        // 2.确认没有遗漏的真实业务引用，再执行不可恢复的逻辑删除。
        this.requireUnbound(file);
        files.markDeleted(id, null);
        return true;
    }

    /** 同时核对归属字段与真实引用，数据不一致时报告异常，不推测或自动修复。 */
    private void requireUnbound(SysFile file) {
        var actual = this.actual(file.getId());
        var expected = file.getBizType() == null ? Set.<FileReferenceChecker.Reference>of()
                : Set.of(new FileReferenceChecker.Reference(file.getBizType(), file.getBizId()));
        if (!actual.equals(expected)) throw new BusinessException("文件业务归属与真实引用不一致: " + file.getId());
        if (!actual.isEmpty()) throw new BusinessException("文件仍被业务引用，请通过对应业务解绑");
    }

    /** 聚合各业务模块提供的真实引用；检查器只读业务数据，不反向取得业务行锁。 */
    private Set<FileReferenceChecker.Reference> actual(long id) {
        var actual = new HashSet<FileReferenceChecker.Reference>();
        for (var checker : checkers) actual.addAll(checker.references(Set.of(id)).getOrDefault(id, Set.of()));
        return actual;
    }

    /** 拒绝不存在或已删除的逻辑文件，防止通过绑定流程恢复删除记录。 */
    private SysFile require(SysFile file) {
        if (file == null || Boolean.TRUE.equals(file.getDeleted())) throw new BusinessException("文件不存在或已删除");
        return file;
    }

    /** 仅允许上传者或拥有指定文件管理权限的已登录用户操作。 */
    private void checkOwner(SysFile file, String permission) {
        var actor = SecurityUtils.getUserId();
        if (actor == null || (!actor.equals(file.getCreateBy()) && !permissions.has(permission))) {
            throw new org.springframework.security.access.AccessDeniedException("无权操作此文件");
        }
    }
}
