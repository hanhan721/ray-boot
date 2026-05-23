package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.core.domain.TreeNode;
import com.ray.common.core.util.TreeUtils;
import com.ray.common.core.util.AssertUtils;
import com.ray.system.api.entity.SysDept;
import com.ray.system.constant.SystemCacheConstants;
import com.ray.system.mapper.DeptMapper;
import com.ray.system.service.DeptService;
import com.ray.system.service.UserDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门 Service 实现
 */
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends ServiceImpl<DeptMapper, SysDept> implements DeptService {

    private final UserDeptService userDeptService;

    @Override
    public List<SysDept> listAll() {
        return super.list(
                Wrappers.<SysDept>lambdaQuery()
                        .orderByAsc(SysDept::getSort)
        );
    }

    @Cacheable(cacheNames = SystemCacheConstants.DEPT, key = SystemCacheConstants.KEY_DEPT_TREE)
    @Override
    public List<SysDept> listTree() {
        return TreeUtils.buildTree(this.listAll());
    }

    @Override
    public SysDept getById(Long id) {
        return super.getById(id);
    }

    @CacheEvict(cacheNames = SystemCacheConstants.DEPT, allEntries = true)
    @Override
    public void add(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(TreeNode.ROOT_PARENT_ID);
        }
        this.checkParentExists(dept.getParentId());
        this.checkNameUnique(dept.getDeptName(), dept.getParentId(), null);
        super.save(dept);
    }

    @CacheEvict(cacheNames = SystemCacheConstants.DEPT, allEntries = true)
    @Override
    public void update(SysDept dept) {
        AssertUtils.notNull(dept.getId(), "部门 ID 不能为空");
        AssertUtils.notNull(super.getById(dept.getId()), "部门不存在");

        if (dept.getParentId() == null) {
            dept.setParentId(TreeNode.ROOT_PARENT_ID);
        }
        this.checkParentExists(dept.getParentId());
        this.checkNameUnique(dept.getDeptName(), dept.getParentId(), dept.getId());
        this.checkCyclic(dept.getId(), dept.getParentId());

        super.updateById(dept);
    }

    @CacheEvict(cacheNames = SystemCacheConstants.DEPT, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        // 检查是否有子级（排除同时要删除的子级）
        var childCount = super.count(
                Wrappers.<SysDept>lambdaQuery()
                        .in(SysDept::getParentId, ids)
                        .notIn(SysDept::getId, ids)
        );
        AssertUtils.isFalse(childCount > 0, "存在子级部门，无法删除");

        // 检查是否有用户关联
        AssertUtils.isFalse(userDeptService.existsByDeptIds(ids), "部门下存在用户，无法删除");

        // 删除部门
        super.removeByIds(ids);
    }

    @CacheEvict(cacheNames = SystemCacheConstants.DEPT, key = SystemCacheConstants.KEY_DEPT_TREE)
    @Override
    public void clearTreeCache() {
    }

    /**
     * 校验部门名称唯一性（同级下不重复）
     */
    private void checkNameUnique(String deptName, Long parentId, Long excludeId) {
        var wrapper = Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getDeptName, deptName)
                .eq(SysDept::getParentId, parentId != null ? parentId : TreeNode.ROOT_PARENT_ID)
                .ne(excludeId != null, SysDept::getId, excludeId);
        AssertUtils.isFalse(super.exists(wrapper), "同级下已存在相同名称的部门");
    }

    /**
     * 校验父级是否存在
     */
    private void checkParentExists(Long parentId) {
        if (parentId == null || parentId.equals(TreeNode.ROOT_PARENT_ID)) {
            return;
        }
        AssertUtils.isTrue(super.exists(
                Wrappers.<SysDept>lambdaQuery().eq(SysDept::getId, parentId)
        ), "父级部门不存在");
    }

    /**
     * 校验是否存在循环依赖（沿 parentId 链向上遍历）
     */
    private void checkCyclic(Long id, Long parentId) {
        if (parentId == null || parentId.equals(TreeNode.ROOT_PARENT_ID)) {
            return;
        }
        AssertUtils.isFalse(id.equals(parentId), "父级不能设置为自己");

        var parentMap = super.list(
                Wrappers.<SysDept>lambdaQuery()
                        .select(SysDept::getId, SysDept::getParentId)
        ).stream().collect(Collectors.toMap(
                SysDept::getId,
                d -> d.getParentId() != null ? d.getParentId() : TreeNode.ROOT_PARENT_ID
        ));

        Set<Long> visited = new HashSet<>();
        visited.add(id);
        Long current = parentId;
        while (current != null && !current.equals(TreeNode.ROOT_PARENT_ID)) {
            AssertUtils.isFalse(visited.contains(current), "不能形成循环依赖");
            visited.add(current);
            current = parentMap.get(current);
        }
    }
}
