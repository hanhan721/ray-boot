package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.system.api.domain.DictDataPageQuery;
import com.ray.common.core.util.AssertUtils;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.system.api.entity.SysDictData;
import com.ray.system.mapper.DictDataMapper;
import com.ray.system.constant.SystemCacheConstants;
import com.ray.system.service.DictDataService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 字典数据 Service 实现
 */
@Service
public class DictDataServiceImpl extends ServiceImpl<DictDataMapper, SysDictData> implements DictDataService {

    @Override
    public IPage<SysDictData> page(DictDataPageQuery query) {
        var wrapper = QueryWrapperHelper.<SysDictData>build(query)
                .orderByDesc("create_time");
        return super.page(PageUtils.buildPage(query), wrapper);
    }

    @Override
    @Cacheable(cacheNames = SystemCacheConstants.DICT_DATA, key = "#dictType")
    public List<SysDictData> listByDictType(String dictType) {
        return super.list(
                Wrappers.<SysDictData>lambdaQuery()
                        .eq(SysDictData::getDictType, dictType)
                        .eq(SysDictData::getEnabled, true)
                        .orderByAsc(SysDictData::getSort)
        );
    }

    @Override
    public SysDictData getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.DICT_DATA, key = "#dictData.dictType")
    public void add(SysDictData dictData) {
        super.save(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.DICT_DATA, key = "#dictData.dictType")
    public void update(SysDictData dictData) {
        AssertUtils.notNull(dictData.getId(), "字典数据 ID 不能为空");
        super.updateById(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.DICT_DATA, allEntries = true)
    public void delete(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        super.removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.DICT_DATA, allEntries = true)
    public void deleteByDictTypes(Collection<String> dictTypes) {
        if (dictTypes == null || dictTypes.isEmpty()) {
            return;
        }
        super.remove(
                Wrappers.<SysDictData>lambdaQuery()
                        .in(SysDictData::getDictType, dictTypes)
        );
    }
}
