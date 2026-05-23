package com.ray.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 轻量基础实体（id + createTime + updateTime + deleted）
 * <p>
 * 适用于无需记录操作人的表（用户自操作的表，如地址、收藏、购物车等），
 * 不含 createBy/updateBy 审计人字段。
 * <p>
 * deleted 字段依赖全局逻辑删除配置（logic-delete-field: deleted）。
 * <p>
 * 继承体系：
 * <pre>
 * IdBaseEntity (id)
 *   └── SimpleBaseEntity (+ createTime + updateTime + deleted)
 * </pre>
 *
 * @param <T> 子类类型
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class SimpleBaseEntity<T extends SimpleBaseEntity<T>> extends IdBaseEntity<T> {

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除（false-正常 true-已删除）
     * <p>
     * 由全局配置 logic-delete-value: "true" 维护，数据库默认值为 FALSE
     */
    private Boolean deleted;

    @SuppressWarnings("unchecked")
    public T setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setDeleted(Boolean deleted) {
        this.deleted = deleted;
        return (T) this;
    }
}
