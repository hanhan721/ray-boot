package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.system.api.domain.DictTypePageQuery;
import com.ray.common.core.util.AssertUtils;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.system.api.entity.SysDictData;
import com.ray.system.api.entity.SysDictType;
import com.ray.system.mapper.DictTypeMapper;
import com.ray.system.service.DictDataService;
import com.ray.system.constant.SystemCacheConstants;
import com.ray.system.service.DictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 字典类型 Service 实现
 */
@Service
@RequiredArgsConstructor
public class DictTypeServiceImpl extends ServiceImpl<DictTypeMapper, SysDictType> implements DictTypeService {

    private final DictDataService dictDataService;

    @Override
    public IPage<SysDictType> page(DictTypePageQuery query) {
        return super.page(PageUtils.buildPage(query), QueryWrapperHelper.build(query));
    }

    @Override
    @Cacheable(cacheNames = SystemCacheConstants.DICT_TYPES, key = SystemCacheConstants.KEY_DICT_TYPE_ALL)
    public List<SysDictType> listAll() {
        return super.list(
                Wrappers.<SysDictType>lambdaQuery()
                        .eq(SysDictType::getEnabled, true)
                        .orderByAsc(SysDictType::getDictType)
        );
    }

    @Override
    public SysDictType getById(Long id) {
        return super.getById(id);
    }

    @Override
    @Cacheable(cacheNames = SystemCacheConstants.DICT_TYPES, key = "#dictType")
    public SysDictType getByDictType(String dictType) {
        return super.getOne(
                Wrappers.<SysDictType>lambdaQuery()
                        .eq(SysDictType::getDictType, dictType)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.DICT_TYPES, allEntries = true)
    public void add(SysDictType dictType) {
        this.checkDictTypeUnique(dictType.getDictType(), null);
        super.save(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.DICT_TYPES, allEntries = true)
    public void update(SysDictType dictType) {
        AssertUtils.notNull(dictType.getId(), "字典类型 ID 不能为空");

        var oldDictType = super.getById(dictType.getId());
        AssertUtils.notNull(oldDictType, "字典类型不存在");

        this.checkDictTypeUnique(dictType.getDictType(), dictType.getId());

        // 如果字典类型编码变更，同步更新字典数据的类型
        if (!oldDictType.getDictType().equals(dictType.getDictType())) {
            dictDataService.lambdaUpdate()
                    .eq(SysDictData::getDictType, oldDictType.getDictType())
                    .set(SysDictData::getDictType, dictType.getDictType())
                    .update();
        }

        super.updateById(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.DICT_TYPES, allEntries = true)
    public void delete(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 查询要删除的字典类型编码
        var dictTypes = super.lambdaQuery()
                .select(SysDictType::getDictType)
                .in(SysDictType::getId, ids)
                .list()
                .stream()
                .map(SysDictType::getDictType)
                .toList();

        // 批量删除关联的字典数据
        dictDataService.deleteByDictTypes(dictTypes);

        super.removeByIds(ids);
    }

    /**
     * 校验字典类型编码唯一性
     */
    private void checkDictTypeUnique(String dictType, Long excludeId) {
        if (dictType == null || dictType.isBlank()) {
            return;
        }
        var wrapper = Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getDictType, dictType)
                .ne(excludeId != null, SysDictType::getId, excludeId);
        AssertUtils.isTrue(!super.exists(wrapper), "字典类型编码已存在");
    }
}
