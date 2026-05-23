package com.ray.common.core.util;

import com.ray.common.core.domain.TreeNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 树形结构工具类
 */
public final class TreeUtils {

    private TreeUtils() {
    }

    /**
     * 将平铺列表构建为树形结构
     *
     * @param nodes 平铺节点列表
     * @param <T>   节点类型
     * @return 树形结构（根节点列表）
     */
    public static <T extends TreeNode<T>> List<T> buildTree(List<T> nodes) {
        return buildTree(nodes, null);
    }

    /**
     * 将平铺列表构建为树形结构，支持排序
     *
     * @param nodes      平铺节点列表
     * @param comparator 排序比较器，可为 null
     * @param <T>        节点类型
     * @return 树形结构（根节点列表）
     */
    public static <T extends TreeNode<T>> List<T> buildTree(List<T> nodes, Comparator<T> comparator) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 按 ID 建立索引
        Map<Long, T> nodeMap = nodes.stream()
                .collect(Collectors.toMap(TreeNode::getId, Function.identity(), (a, b) -> a));

        List<T> roots = new ArrayList<>();

        for (T node : nodes) {
            if (node.isRoot()) {
                roots.add(node);
            } else {
                T parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                }
            }
        }

        // 排序
        if (comparator != null) {
            sortTree(roots, comparator);
        }

        return roots;
    }

    /**
     * 递归排序树
     */
    private static <T extends TreeNode<T>> void sortTree(List<T> nodes, Comparator<T> comparator) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(comparator);
        for (T node : nodes) {
            sortTree(node.getChildren(), comparator);
        }
    }

    /**
     * 将树形结构展开为平铺列表
     *
     * @param roots 根节点列表
     * @param <T>   节点类型
     * @return 平铺列表
     */
    public static <T extends TreeNode<T>> List<T> flatten(List<T> roots) {
        List<T> result = new ArrayList<>();
        flattenRecursive(roots, result);
        return result;
    }

    private static <T extends TreeNode<T>> void flattenRecursive(List<T> nodes, List<T> result) {
        if (nodes == null) {
            return;
        }
        for (T node : nodes) {
            result.add(node);
            flattenRecursive(node.getChildren(), result);
        }
    }

    /**
     * 根据 ID 查找节点
     *
     * @param roots 根节点列表
     * @param id    目标节点 ID
     * @param <T>   节点类型
     * @return 找到的节点，未找到返回 null
     */
    public static <T extends TreeNode<T>> T findById(List<T> roots, Long id) {
        if (roots == null || id == null) {
            return null;
        }
        for (T node : roots) {
            if (Objects.equals(node.getId(), id)) {
                return node;
            }
            T found = findById(node.getChildren(), id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 获取所有子孙节点 ID（不含自身）
     *
     * @param roots    根节点列表
     * @param parentId 父节点 ID
     * @param <T>      节点类型
     * @return 所有子孙节点 ID
     */
    public static <T extends TreeNode<T>> List<Long> getDescendantIds(List<T> roots, Long parentId) {
        List<Long> ids = new ArrayList<>();
        T parent = findById(roots, parentId);
        if (parent != null) {
            collectDescendantIds(parent.getChildren(), ids);
        }
        return ids;
    }

    private static <T extends TreeNode<T>> void collectDescendantIds(List<T> nodes, List<Long> ids) {
        if (nodes == null) {
            return;
        }
        for (T node : nodes) {
            ids.add(node.getId());
            collectDescendantIds(node.getChildren(), ids);
        }
    }

    /**
     * 获取节点到根节点的路径（从根到当前节点）
     *
     * @param roots 根节点列表
     * @param id    目标节点 ID
     * @param <T>   节点类型
     * @return 路径节点列表
     */
    public static <T extends TreeNode<T>> List<T> getPath(List<T> roots, Long id) {
        List<T> path = new ArrayList<>();
        findPath(roots, id, path);
        return path;
    }

    private static <T extends TreeNode<T>> boolean findPath(List<T> nodes, Long id, List<T> path) {
        if (nodes == null) {
            return false;
        }
        for (T node : nodes) {
            path.add(node);
            if (Objects.equals(node.getId(), id)) {
                return true;
            }
            if (findPath(node.getChildren(), id, path)) {
                return true;
            }
            path.removeLast();
        }
        return false;
    }
}
