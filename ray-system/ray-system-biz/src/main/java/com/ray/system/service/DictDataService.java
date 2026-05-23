package com.ray.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.domain.DictDataPageQuery;
import com.ray.system.api.entity.SysDictData;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 字典数据 Service
 */
public interface DictDataService extends IService<SysDictData> {

    /**
     * 分页查询字典数据
     *
     * @param query 分页参数
     * @return 分页结果
     */
    IPage<SysDictData> page(DictDataPageQuery query);

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    List<SysDictData> listByDictType(String dictType);

    /**
     * 根据 ID 查询字典数据
     *
     * @param id 字典数据 ID
     * @return 字典数据
     */
    SysDictData getById(Long id);

    /**
     * 新增字典数据
     *
     * @param dictData 字典数据
     */
    void add(SysDictData dictData);

    /**
     * 修改字典数据
     *
     * @param dictData 字典数据
     */
    void update(SysDictData dictData);

    /**
     * 删除字典数据
     *
     * @param ids 字典数据 ID 集合
     */
    void delete(Set<Long> ids);

    /**
     * 根据字典类型批量删除字典数据
     *
     * @param dictTypes 字典类型集合
     */
    void deleteByDictTypes(Collection<String> dictTypes);
}
