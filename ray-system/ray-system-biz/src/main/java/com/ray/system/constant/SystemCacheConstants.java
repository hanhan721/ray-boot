package com.ray.system.constant;

/**
 * System 模块缓存 Key 常量
 */
public final class SystemCacheConstants {

    private SystemCacheConstants() {
    }

    // ==================== Cache Names ====================

    /** 权限缓存 */
    public static final String PERMISSIONS = "permissions";

    /** 字典类型缓存 */
    public static final String DICT_TYPES = "dictTypes";

    /** 字典数据缓存 */
    public static final String DICT_DATA = "dictData";

    /** 系统参数缓存 */
    public static final String SYS_PARAM = "sysParam";

    /** 部门缓存 */
    public static final String DEPT = "dept";

    // ==================== Cache Keys ====================

    /** 权限树 */
    public static final String KEY_PERMISSION_TREE = "'tree'";

    /** 所有字典类型 */
    public static final String KEY_DICT_TYPE_ALL = "'all'";

    /** 部门树 */
    public static final String KEY_DEPT_TREE = "'tree'";
}
