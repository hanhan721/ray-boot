# 文档导航

项目文档按用途分为三层：**设计**说明"为什么这样设计、边界在哪里"，**指南**说明"怎么接入、怎么扩展"，**运维**说明"怎么部署、怎么配置"。写新文档前先看本页，确定归属目录。

## 设计文档（docs/design/）

| 文档 | 内容 |
| --- | --- |
| [扩展点](design/extension-points.md) | 系统预留的扩展点、聚合机制与依赖方向约束 |
| [会话管理](design/session-management.md) | RefreshToken 轮换、在线会话分页与升级说明 |

## 开发指南（docs/guides/）

| 文档 | 内容 |
| --- | --- |
| [业务模块接入](guides/business-module-integration.md) | 新建业务模块、扩展契约总览、跨模块引用检查（删除保护） |

## 部署运维（docs/operations/）

| 文档 | 内容 |
| --- | --- |
| [文件存储部署](operations/file-storage.md) | 桶权限、本地目录、签名访问与存储迁移约束 |

## 数据库与资产

- [docs/sql/](sql/)：PostgreSQL 表定义与初始化数据；Docker 初始化和应用测试会直接读取，路径不要随意调整。
- [docs/images/](images/)：README 截图。

## 约定

- 设计文档只写"为什么"，不复制实现示例；实现步骤、代码示例、检查清单放对应指南，并互相链接。
- `docs/plans/` 与 `docs/reviews/` 是历史审查的过程记录，不在本导航内，也不再继续扩展。
