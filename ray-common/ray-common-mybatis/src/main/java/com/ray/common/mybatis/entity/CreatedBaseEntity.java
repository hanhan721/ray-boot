package com.ray.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 含创建审计的基础实体（id + createTime + createBy）
 * <p>
 * 适用于只记录创建信息、不需要更新审计的场景
 *
 * @param <T> 子类类型
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class CreatedBaseEntity<T extends CreatedBaseEntity<T>> extends IdBaseEntity<T> {

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 创建人 ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @SuppressWarnings("unchecked")
    public T setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setCreateBy(Long createBy) {
        this.createBy = createBy;
        return (T) this;
    }
}
