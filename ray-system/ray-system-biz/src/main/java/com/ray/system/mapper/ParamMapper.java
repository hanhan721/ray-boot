package com.ray.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.ray.system.api.entity.SysParam;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统参数 Mapper
 */
@Mapper
public interface ParamMapper extends BaseMapper<SysParam> {

    /**
     * 按参数键查询
     */
    default SysParam selectByParamKey(String paramKey) {
        return selectOne(Wrappers.<SysParam>lambdaQuery()
                .eq(SysParam::getParamKey, paramKey)
                .eq(SysParam::getDeleted, false));
    }

    /**
     * 检查参数键是否存在
     */
    default boolean existsByParamKey(String paramKey, Long excludeId) {
        return SqlHelper.retBool(selectCount(Wrappers.<SysParam>lambdaQuery()
                .eq(SysParam::getParamKey, paramKey)
                .ne(excludeId != null, SysParam::getId, excludeId)
                .eq(SysParam::getDeleted, false)));
    }
}
