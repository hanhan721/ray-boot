# 业务模块接入指南

面向在 `ray-business` 下新增业务模块的开发者。本文覆盖模块创建步骤、系统扩展契约，以及最容易遗漏的"跨模块引用检查"。扩展点的整体设计与依赖方向见[扩展点](../design/extension-points.md)。

## 1. 新建业务模块

1. 在 `ray-business/` 下创建子模块，并在 `ray-business/pom.xml` 的 `<modules>` 中登记。
2. 在根 `pom.xml` 的 `dependencyManagement` 中登记该模块版本。
3. 在 `ray-boot-app/pom.xml` 添加该模块依赖。

约定：

- 业务代码放在 `com.ray.business.*` 包下（组件扫描 `com.ray`，Mapper 扫描 `com.ray.**.mapper`）。
- 业务模块依赖 `ray-system-api` 获取系统契约（实体、VO、扩展接口），**不要**依赖 `ray-system-biz`。
- 新接口默认要求登录（`anyRequest().authenticated()`），需要细粒度权限时用 `@PreAuthorize("@perm.has('...')")`，并把权限码写入 `sys_permission` 初始化数据。
- 响应统一返回 `R`，校验失败、未登录、无权限由全局处理器处理。

## 2. 可用扩展点总览

| 需求 | 实现契约 | 说明 |
| --- | --- | --- |
| 发送通知 | `Notifier` | 业务只声明通知意图，渠道实现独立 |
| 响应字段翻译 | `Translator` | 注册为 Bean 即生效，字典翻译已内置 |
| 参与删除判断 | `UserReferenceChecker` 等 | 见第 3 节 |
| 绑定/释放文件 | `FileBindingService` | 与业务更新共用事务 |
| 定时任务 | `@ScheduleTask` | Bean + `sys_job` 记录 Bean 名 |
| 自定义存储后端 | `StorageService` | 配置切换，一般不需要 |

## 3. 跨模块引用检查（删除保护）

### 3.1 什么时候需要实现

业务表保存了系统数据的 id，并且该引用是**有效业务依赖**时，必须实现对应检查器，否则系统删除接口不会感知业务引用，删除后业务行会指向已删除的 id。

| 接口（均在 `com.ray.system.api.reference`） | 方法 | 保护的删除入口 |
| --- | --- | --- |
| `UserReferenceChecker` | `Set<Long> referencedUserIds(Set<Long> userIds)` | `DELETE /api/sys/user` |
| `DeptReferenceChecker` | `Set<Long> referencedDeptIds(Set<Long> deptIds)` | `DELETE /api/sys/dept` |
| `RoleReferenceChecker` | `Set<Long> referencedRoleIds(Set<Long> roleIds)` | `DELETE /api/sys/role` |
| `DictTypeReferenceChecker` | `Set<String> referencedDictTypes(Set<String> dictTypes)` | `DELETE /api/sys/dict/type` |
| `FileReferenceChecker`（`api.file` 包） | `Map<Long, Set<Reference>> references(Set<Long> fileIds)` | `DELETE /api/sys/file`、文件过期清理，并参与绑定一致性核对 |

不实现检查器时，对应删除**照常执行，不会报错**——这是为"还没有业务模块"预留的默认行为，也意味着遗漏实现不会被发现，需要靠 3.7 的清单防止。

### 3.2 实现示例

```java
@Component
@RequiredArgsConstructor
public class ContractUserReferenceChecker implements UserReferenceChecker {

    private final ContractMapper contracts;

    @Override
    public Set<Long> referencedUserIds(Set<Long> userIds) {
        return contracts.selectReferencedUserIds(userIds);
    }
}
```

对应 Mapper 只查**有效业务行**：

```sql
SELECT DISTINCT owner_user_id
FROM contract
WHERE owner_user_id IN (...)
  AND deleted = false
```

实现要求：

- 返回**候选集合中被引用的子集**，不是 boolean；没有引用返回空集合，不能返回 `null`。
- 只读业务数据，不加锁、不写库；检查器在系统删除事务内执行，查询失败会随删除事务回滚（fail closed）。
- 一个实现类可以合并多张业务表的查询；多个业务模块也可以各自实现同一接口，系统会自动聚合。

### 3.3 什么算引用

| 场景 | 是否阻止删除 |
| --- | --- |
| 合同的 `owner_user_id`，合同仍有效 | 是 |
| 项目的 `dept_id`，项目仍归属该部门 | 是 |
| 业务配置使用中的字典类型编码 | 是 |
| `create_by` / `update_by` 等审计字段 | 否 |
| `sys_oper_log.oper_user_id`、登录日志 | 否 |
| 订单快照中的用户名 / 部门名 | 否 |
| 已逻辑删除的业务行 | 否 |

判断标准：**删除该系统记录后，业务数据是否还能正常工作**。审计字段和历史记录删除后仍能正常展示，所以不算引用；把审计字段也算进检查器会让用户、部门永远无法删除。

### 3.4 系统删除时的行为

以用户删除为例，系统按以下顺序执行：

1. 取得授权协调锁与用户行锁；
2. 调用全部 `UserReferenceChecker`，合并返回的 id；
3. 并集非空 → 抛出 `BusinessException("用户仍被业务引用: [ids]")`，**整批拒绝**，不撤销会话、不删除关联、不释放文件；
4. 并集为空 → 按原逻辑删除。

多个检查器中任意一个报告引用，整批失败，避免部分删除。

### 3.5 边界与限制

- **只能阻止，不能级联**。希望"删除用户时自动清理业务数据"需要另一套删除事件机制，当前扩展点不提供。
- **不是强一致约束**。检查器只读业务数据，检查通过后、事务提交前，另一个事务仍可能插入业务引用。需要硬保证时，业务侧应使用同一把锁、数据库外键或事务协调。
- **单数据源假设**。检查器与系统删除共用一个事务；业务模块使用独立数据源时该保证不成立。
- **数据库外键帮不上忙**。用户、部门、角色等系统记录是逻辑删除（`UPDATE deleted = true`），外键不会触发，所以必须用检查器。
- **遗漏实现不报错**。新表接入时对照 3.6 的清单逐项确认。

### 3.6 新业务表接入清单

- [ ] 表中是否引用了系统 id（`user_id` / `dept_id` / `role_id` / `dict_type` / `file_id`）？
- [ ] 该引用是业务依赖，还是审计/历史字段？
- [ ] 查询是否过滤了 `deleted = false`（或等价有效状态）？
- [ ] 返回的是候选子集，且不会返回 `null`？
- [ ] 实现类在 `com.ray` 扫描范围内，业务模块已加入 `ray-boot-app` 依赖？
- [ ] 有"有引用拒绝 / 无引用放行"两条测试？

### 3.7 测试建议

业务模块至少写两条：候选 id 被引用时检查器返回它；无引用时返回空集合。系统侧的聚合、整批拒绝与空实现行为已由 `ReferenceCheckerContractTest` 覆盖，不需要重复测试系统模块。

## 4. 文件绑定与释放

业务表保存文件 id 时，除引用检查外，归属变更应通过 `FileBindingService` 完成：

- `bind(fileId, bizType, bizId)`：在业务事务中绑定文件归属，系统会核对上传者归属与真实业务引用；
- `release(fileId, bizType, bizId)`：业务引用移除后调用，复核通过后逻辑删除文件。

先写业务外键、再调用 `release` 释放旧文件，任一步失败随业务事务一起回滚。存储与桶的部署细节见[文件存储部署](../operations/file-storage.md)。

## 5. 定时任务

任务类加上 `@ScheduleTask` 注册为 Spring Bean，并在 `sys_job` 写入 Bean 名与 Cron。调度器按 Bean 名反射调用，支持并发控制与启停；任务日志由调度模块异步写入。
