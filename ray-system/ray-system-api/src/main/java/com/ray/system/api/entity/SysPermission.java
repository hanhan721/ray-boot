package com.ray.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ray.common.core.domain.TreeNode;
import com.ray.common.mybatis.entity.BaseEntity;
import com.ray.system.api.enums.PermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.List;

/**
 * 权限实体（树形结构，支持动态路由）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity<SysPermission> implements TreeNode<SysPermission> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 父级 ID，根节点为 0
     */
    private Long parentId;

    /**
     * 权限名称/菜单标题
     */
    @NotBlank(message = "权限名称不能为空")
    private String name;

    /**
     * 权限类型
     */
    @NotNull(message = "权限类型不能为空")
    private PermissionType type;

    /**
     * 路由路径
     */
    private String url;

    /**
     * 组件路径，目录可为空或 Layout
     */
    private String component;

    /**
     * 重定向地址，目录使用
     */
    private String redirect;

    /**
     * 权限标识，如 user:add，按钮必填
     */
    private String code;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否隐藏路由
     */
    private Boolean hidden;

    /**
     * 是否缓存页面
     */
    private Boolean keepAlive;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 前端路由名称（vue-router name），MENU 类型填写
     */
    private String routeName;

    /**
     * 描述
     */
    private String description;

    /**
     * 子权限列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysPermission> children;
}
