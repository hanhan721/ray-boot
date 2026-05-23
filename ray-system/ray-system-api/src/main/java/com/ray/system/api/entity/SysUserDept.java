package com.ray.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户部门关联实体
 * <p>
 * 联合主键：user_id + dept_id
 */
@Data
@Accessors(chain = true)
@TableName("sys_user_dept")
public class SysUserDept implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 部门 ID
     */
    private Long deptId;
}
