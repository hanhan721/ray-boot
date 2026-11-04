package com.ray.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.entity.SysDept;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 部门 Service
 */
public interface DeptService extends IService<SysDept> {

    /**
     * 获取部门列表（按排序）
     *
     * @return 部门列表
     */
    List<SysDept> listAll();

    /**
     * 获取树形部门列表
     *
     * @return 树形部门列表
     */
    List<SysDept> listTree();

    /**
     * 根据 ID 查询部门
     *
     * @param id 部门 ID
     * @return 部门信息
     */
    SysDept getById(Long id);

    /**
     * 按 ID 升序锁定部门行，必须在事务内调用。
     *
     * @param ids 部门 ID 集合
     * @return 实际存在的部门
     */
    List<SysDept> listByIdsForUpdate(Collection<Long> ids);

    /**
     * 新增部门
     *
     * @param dept 部门信息
     */
    void add(SysDept dept);

    /**
     * 修改部门
     *
     * @param dept 部门信息
     */
    void update(SysDept dept);

    /**
     * 删除部门
     *
     * @param ids 部门 ID 集合
     */
    void delete(Set<Long> ids);

    /**
     * 清理部门树缓存
     */
    void clearTreeCache();
}
