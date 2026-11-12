/**
 * 跨模块引用检查契约。
 * <p>
 * 业务模块实现对应检查器后，系统删除接口在删除任何数据前聚合全部实现；
 * 返回的引用集合非空时整批拒绝删除，未实现时删除行为保持不变。
 * 只查有效业务数据，审计字段（create_by/update_by）与已逻辑删除的行不算引用。
 * 接入示例与判定标准见 docs/guides/business-module-integration.md。
 */
package com.ray.system.api.reference;
