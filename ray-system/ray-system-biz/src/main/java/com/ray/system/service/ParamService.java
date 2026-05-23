package com.ray.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.system.api.domain.ParamCreateRequest;
import com.ray.system.api.domain.ParamPageQuery;
import com.ray.system.api.domain.ParamUpdateRequest;
import com.ray.system.api.domain.ParamVO;
import com.ray.system.api.entity.SysParam;

import java.util.List;
import java.util.Set;

/**
 * 系统参数 Service
 */
public interface ParamService extends IService<SysParam> {

    /**
     * 创建参数
     */
    Long create(ParamCreateRequest request);

    /**
     * 更新参数
     */
    void update(ParamUpdateRequest request);

    /**
     * 删除参数
     */
    void delete(Set<Long> ids);

    /**
     * 按 ID 获取参数详情
     */
    ParamVO getDetailById(Long id);

    /**
     * 按参数键获取参数值
     */
    Object getValueByKey(String paramKey);

    /**
     * 按参数键获取字符串值
     */
    String getStringByKey(String paramKey);

    /**
     * 按参数键获取整数值
     */
    Integer getIntegerByKey(String paramKey);

    /**
     * 按参数键获取布尔值
     */
    Boolean getBooleanByKey(String paramKey);

    /**
     * 按参数键获取 JSON 值
     */
    Object getJsonByKey(String paramKey);

    /**
     * 按参数键获取字符串列表（逗号分隔）
     */
    List<String> getStringListByKey(String paramKey);

    /**
     * 分页查询
     */
    IPage<ParamVO> page(ParamPageQuery query);

    /**
     * 按业务类型查询所有参数
     */
    List<ParamVO> listByBizType(Integer bizType);
}
