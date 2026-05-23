package com.ray.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.domain.DictTypePageQuery;
import com.ray.system.api.entity.SysDictType;

import java.util.List;
import java.util.Set;

/**
 * 字典类型 Service
 */
public interface DictTypeService extends IService<SysDictType> {

    /**
     * 分页查询字典类型
     *
     * @param query 分页参数
     * @return 分页结果
     */
    IPage<SysDictType> page(DictTypePageQuery query);

    /**
     * 查询所有字典类型
     *
     * @return 字典类型列表
     */
    List<SysDictType> listAll();

    /**
     * 根据 ID 查询字典类型
     *
     * @param id 字典类型 ID
     * @return 字典类型
     */
    SysDictType getById(Long id);

    /**
     * 根据字典类型编码查询
     *
     * @param dictType 字典类型编码
     * @return 字典类型
     */
    SysDictType getByDictType(String dictType);

    /**
     * 新增字典类型
     *
     * @param dictType 字典类型
     */
    void add(SysDictType dictType);

    /**
     * 修改字典类型
     *
     * @param dictType 字典类型
     */
    void update(SysDictType dictType);

    /**
     * 删除字典类型
     *
     * @param ids 字典类型 ID 集合
     */
    void delete(Set<Long> ids);
}
