package com.ray.common.mybatis.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 含完整审计的基础实体（id + createTime + createBy + updateTime + updateBy）
 * <p>
 * 适用于需要完整审计但无需逻辑删除的场景
 *
 * @param <T> 子类类型
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class AuditBaseEntity<T extends AuditBaseEntity<T>> extends CreatedBaseEntity<T> {

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新人 ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @SuppressWarnings("unchecked")
    public T setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
        return (T) this;
    }
}
