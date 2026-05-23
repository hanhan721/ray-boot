package com.ray.common.mybatis.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 完整基础实体（id + createTime + createBy + updateTime + updateBy + deleted）
 * <p>
 * 包含所有公共字段，适用于需要逻辑删除的主表
 * <p>
 * 继承体系：
 * <pre>
 * IdBaseEntity (id)
 *   ├── SimpleBaseEntity (+ createTime + updateTime + deleted)  ← 无操作人的轻量表
 *   └── CreatedBaseEntity (+ createTime, createBy)
 *         └── AuditBaseEntity (+ updateTime, updateBy)
 *               └── BaseEntity (+ deleted)                     ← 后台管理表
 * </pre>
 *
 * @param <T> 子类类型
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class BaseEntity<T extends BaseEntity<T>> extends AuditBaseEntity<T> {

    /**
     * 逻辑删除（false-正常，true-已删除）
     */
    private Boolean deleted;

    @SuppressWarnings("unchecked")
    public T setDeleted(Boolean deleted) {
        this.deleted = deleted;
        return (T) this;
    }
}
