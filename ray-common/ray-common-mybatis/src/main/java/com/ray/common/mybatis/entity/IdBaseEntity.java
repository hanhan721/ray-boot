package com.ray.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 最小基础实体，仅包含主键
 * <p>
 * 适用于关联表等无需审计字段和逻辑删除的场景
 *
 * @param <T> 子类类型
 */
@Getter
@EqualsAndHashCode
public abstract class IdBaseEntity<T extends IdBaseEntity<T>> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @SuppressWarnings("unchecked")
    public T setId(Long id) {
        this.id = id;
        return (T) this;
    }
}
