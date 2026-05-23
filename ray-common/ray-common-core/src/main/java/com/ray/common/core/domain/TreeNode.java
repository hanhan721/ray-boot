package com.ray.common.core.domain;

import com.ray.common.core.util.TreeUtils;

import java.util.List;

/**
 * 树形节点接口
 * <p>
 * 树形实体实现此接口后，可使用 {@link TreeUtils} 进行树形操作
 *
 * @param <T> 节点类型
 */
public interface TreeNode<T extends TreeNode<T>> {

    /**
     * 根节点的 parentId
     */
    Long ROOT_PARENT_ID = 0L;

    /**
     * 获取节点 ID
     */
    Long getId();

    /**
     * 获取父节点 ID
     */
    Long getParentId();

    /**
     * 获取子节点列表
     */
    List<T> getChildren();

    /**
     * 设置子节点列表
     *
     * @return 当前节点（支持链式调用）
     */
    T setChildren(List<T> children);

    /**
     * 判断是否为根节点
     */
    default boolean isRoot() {
        return getParentId() == null || ROOT_PARENT_ID.equals(getParentId());
    }

    /**
     * 判断是否为叶子节点
     */
    default boolean isLeaf() {
        return getChildren() == null || getChildren().isEmpty();
    }
}
