# 扩展点

系统模块提供完整的管理能力，业务能力通过扩展点接入。所有扩展点遵循同一条底线：**业务模块可以增加实现，但不需要修改系统内核；系统模块永不反向依赖业务模块。**

## 依赖方向

```text
ray-boot-app → ray-system-biz / ray-business / ray-schedule
                        ↓
                  ray-framework → ray-common
```

- 业务模块依赖 `ray-system-api`（系统契约），不依赖 `ray-system-biz`（系统实现）。
- 公共模块（common、framework、schedule）不引用 `com.ray.system` 的任何类型。
- 启动类扫描 `com.ray`，Mapper 扫描 `com.ray.**.mapper`，业务实现类只要在扫描范围内即可被发现。

## 聚合机制

扩展点统一使用"契约接口 + Spring 集合注入"：

- 契约接口定义在低层模块（common 或 system-api）；
- 实现方提供 Spring Bean；
- 消费方注入 `List<契约接口>`，没有实现时注入空集合，行为保持不变；
- 检查类扩展点失败时异常向上抛，调用方事务回滚（fail closed）。

## 扩展点清单

| 扩展点 | 契约位置 | 聚合点 | 行为 |
| --- | --- | --- | --- |
| 通知发送 | `ray-common-notify` 的 `Notifier` | 按通知渠道分发 | 业务只声明通知意图，不关心渠道实现 |
| 邮件发件人 | `ray-common-notify` 的 `MailFromProvider` | `MailSenderConfiguration` | 系统模块用 `ParamService` 提供默认实现 |
| 响应翻译 | `ray-common-core` 的 `Translator` | `TranslateRegistry` | 注册为 Bean 即生效，按翻译类型分组 |
| 存储后端 | `ray-common-storage` 的 `StorageService` | `StorageAutoConfiguration` | 按配置选择 LOCAL / RUST_FS，可替换实现 |
| 文件绑定 | `ray-system-api` 的 `FileBindingService` | `FileBindings` | 业务事务中绑定/释放逻辑文件归属 |
| 文件引用检查 | `ray-system-api` 的 `FileReferenceChecker` | `FileBindings` | 文件删除与过期前核对真实业务引用 |
| 业务引用检查 | `ray-system-api` 的 `UserReferenceChecker` 等 | 用户/部门/角色/字典删除 Service | 删除前聚合检查，仍被引用则整批拒绝 |
| 定时任务 | `ray-common-core` 的 `@ScheduleTask` | 调度器按 Bean 名调用 | Bean + `sys_job` 记录即可注册 |

## 删除保护语义

用户、部门、角色、字典类型、文件的删除入口在删除任何数据之前聚合对应的引用检查器：

- 任一实现返回候选集合中仍被有效业务数据引用的 id/编码 → 抛出业务异常，**整批拒绝**，不撤销会话、不清理关联；
- 全部实现返回空 → 按原逻辑删除；
- 检查器抛出异常 → 删除事务回滚。

审计字段（`create_by`/`update_by`）、历史日志、已逻辑删除的业务数据不算引用。

实现方式、判定标准与接入清单见[业务模块接入指南](../guides/business-module-integration.md)；规范摘要见 `AGENTS.md` 的"跨模块反向依赖"一节。
