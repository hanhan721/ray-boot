package com.ray.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.common.core.exception.BusinessException;
import com.ray.common.mybatis.util.PageUtils;
import com.ray.common.mybatis.util.QueryWrapperHelper;
import com.ray.system.api.domain.ParamCreateRequest;
import com.ray.system.api.domain.ParamPageQuery;
import com.ray.system.api.domain.ParamUpdateRequest;
import com.ray.system.api.domain.ParamVO;
import com.ray.system.api.entity.SysParam;
import com.ray.system.converter.ParamValueConverter;
import com.ray.system.crypto.ParamEncryptionHelper;
import com.ray.system.mapper.ParamMapper;
import com.ray.system.constant.SystemCacheConstants;
import com.ray.system.service.ParamService;
import com.ray.system.event.ParamChangedEvent;
import com.ray.common.valkey.util.ValkeyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统参数 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ParamServiceImpl extends ServiceImpl<ParamMapper, SysParam> implements ParamService {

    private final ParamMapper paramMapper;
    private final ParamValueConverter paramValueConverter;
    private final ParamEncryptionHelper encryptionHelper;
    private final ValkeyUtils valkeyUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationContext applicationContext;

    private ParamService getSelf() {
        return applicationContext.getBean(ParamService.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.SYS_PARAM, key = "#request.paramKey()")
    public Long create(ParamCreateRequest request) {
        // 检查参数键唯一性
        if (paramMapper.existsByParamKey(request.paramKey(), null)) {
            throw new BusinessException("参数键 [" + request.paramKey() + "] 已存在");
        }

        // 校验参数值类型
        String validationError = paramValueConverter.getValidationError(request.paramValue(), request.paramType());
        if (validationError != null) {
            throw new BusinessException(validationError);
        }

        // 构建参数实体
        SysParam param = new SysParam()
                .setParamKey(request.paramKey())
                .setParamType(request.paramType())
                .setBizType(request.bizType())
                .setEncrypted(request.encrypted() != null && request.encrypted())
                .setDescription(request.description());

        // 加密处理
        String paramValue = request.paramValue();
        if (param.getEncrypted()) {
            paramValue = encryptionHelper.encrypt(paramValue);
        }
        param.setParamValue(paramValue);

        // 保存到数据库
        super.save(param);
        return param.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ParamUpdateRequest request) {
        // 获取原参数
        SysParam param = super.getById(request.id());
        if (param == null) {
            throw new BusinessException("参数不存在");
        }

        // 校验参数值类型
        String validationError = paramValueConverter.getValidationError(request.paramValue(), param.getParamType());
        if (validationError != null) {
            throw new BusinessException(validationError);
        }

        // 处理加密状态变更
        boolean wasEncrypted = param.getEncrypted();
        boolean nowEncrypted = request.encrypted() != null ? request.encrypted() : wasEncrypted;

        // 更新参数值（根据新的加密状态决定是否加密）
        String paramValue = request.paramValue();
        if (nowEncrypted) {
            paramValue = encryptionHelper.encrypt(paramValue);
        }
        param.setParamValue(paramValue);
        param.setEncrypted(nowEncrypted);

        // 更新描述
        if (StringUtils.hasText(request.description())) {
            param.setDescription(request.description());
        }

        // 更新到数据库
        super.updateById(param);

        // 按前缀批量清除该参数的所有缓存变体（base + :string/:integer/:boolean/:json/:stringList）
        this.evictParamCache(param.getParamKey());

        // 发布参数变更事件
        eventPublisher.publishEvent(new ParamChangedEvent(param.getParamKey()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = SystemCacheConstants.SYS_PARAM, allEntries = true)
    public void delete(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 查出被删除的参数键，用于发布变更事件
        var params = super.listByIds(ids);
        super.removeBatchByIds(ids);

        // 发布参数变更事件
        params.forEach(p -> eventPublisher.publishEvent(new ParamChangedEvent(p.getParamKey())));
    }

    @Override
    public ParamVO getDetailById(Long id) {
        SysParam param = super.getById(id);
        if (param == null) {
            throw new BusinessException("参数不存在");
        }
        return this.buildVO(param, false);
    }

    @Override
    @Cacheable(cacheNames = SystemCacheConstants.SYS_PARAM, key = "#paramKey")
    public Object getValueByKey(String paramKey) {
        SysParam param = paramMapper.selectByParamKey(paramKey);
        if (param == null) {
            return null;
        }

        String value = this.decryptIfNeeded(param);
        return paramValueConverter.convert(value, param.getParamType());
    }

    @Override
    @Cacheable(cacheNames = SystemCacheConstants.SYS_PARAM, key = "#paramKey + ':string'")
    public String getStringByKey(String paramKey) {
        SysParam param = paramMapper.selectByParamKey(paramKey);
        if (param == null) {
            return null;
        }
        return this.decryptIfNeeded(param);
    }

    @Override
    @Cacheable(cacheNames = SystemCacheConstants.SYS_PARAM, key = "#paramKey + ':integer'")
    public Integer getIntegerByKey(String paramKey) {
        Object value = getSelf().getValueByKey(paramKey);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return i;
        }
        return Integer.parseInt(value.toString());
    }

    @Override
    @Cacheable(cacheNames = SystemCacheConstants.SYS_PARAM, key = "#paramKey + ':boolean'")
    public Boolean getBooleanByKey(String paramKey) {
        Object value = getSelf().getValueByKey(paramKey);
        if (value == null) {
            return null;
        }
        return (Boolean) value;
    }

    @Override
    @Cacheable(cacheNames = SystemCacheConstants.SYS_PARAM, key = "#paramKey + ':json'")
    public Object getJsonByKey(String paramKey) {
        return getSelf().getValueByKey(paramKey);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Cacheable(cacheNames = SystemCacheConstants.SYS_PARAM, key = "#paramKey + ':stringList'")
    public List<String> getStringListByKey(String paramKey) {
        Object value = getSelf().getValueByKey(paramKey);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of(value.toString());
    }

    @Override
    public IPage<ParamVO> page(ParamPageQuery query) {
        QueryWrapper<SysParam> wrapper = QueryWrapperHelper.build(query);
        return super.page(PageUtils.buildPage(query), wrapper)
                .convert(param -> this.buildVO(param, true));
    }

    @Override
    public List<ParamVO> listByBizType(Integer bizType) {
        LambdaQueryWrapper<SysParam> wrapper = new LambdaQueryWrapper<SysParam>()
                .eq(SysParam::getBizType, bizType)
                .eq(SysParam::getDeleted, false)
                .orderByDesc(SysParam::getCreateTime);

        List<SysParam> list = super.list(wrapper);
        return list.stream()
                .map(param -> this.buildVO(param, true))
                .collect(Collectors.toList());
    }

    /**
     * 构建 VO，处理加密参数脱敏
     */
    private ParamVO buildVO(SysParam param, boolean maskEncrypted) {
        ParamVO vo = new ParamVO()
                .setId(param.getId())
                .setParamKey(param.getParamKey())
                .setParamType(param.getParamType())
                .setBizType(param.getBizType())
                .setEncrypted(param.getEncrypted())
                .setDescription(param.getDescription())
                .setCreateTime(param.getCreateTime())
                .setUpdateTime(param.getUpdateTime())
                .setCreateBy(param.getCreateBy())
                .setUpdateBy(param.getUpdateBy());

        // 处理参数值：如果加密且在列表中，显示掩码；否则解密返回原值
        if (param.getEncrypted() && maskEncrypted) {
            vo.setParamValue("****");
        } else {
            String value = this.decryptIfNeeded(param);
            vo.setParamValue(value);
        }

        return vo;
    }

    /**
     * 按前缀批量清除参数缓存（SCAN + DELETE）
     */
    private void evictParamCache(String paramKey) {
        var keys = valkeyUtils.scan(SystemCacheConstants.SYS_PARAM + "::" + paramKey + "*", 100);
        if (!keys.isEmpty()) {
            valkeyUtils.delete(keys);
        }
    }

    /**
     * 解密参数值（如果需要的话）
     */
    private String decryptIfNeeded(SysParam param) {
        if (param.getEncrypted()) {
            return encryptionHelper.decrypt(param.getParamValue());
        }
        return param.getParamValue();
    }

}
