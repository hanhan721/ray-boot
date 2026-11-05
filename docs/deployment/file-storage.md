# 文件存储部署

本期仅支持配置中的一个后端。业务保存逻辑文件 ID，上传后由业务服务绑定。逻辑删除不可恢复；物理保留期不是回收站。

## 运行条件

- RustFS 使用专用私有桶，预先创建，禁用版本控制，不能使用已启用后又 Suspended 的桶。启动检查桶 ACL、版本状态与桶策略，不自动修改；本期要求无桶策略，通过账号权限授权。
- 应用账号需要 HeadBucket/ListBucket、GetBucketAcl、GetBucketPolicy、GetBucketVersioning、GetObject、PutObject、DeleteObject 权限，不需要创建桶或改桶配置权限。
- LOCAL 根目录为应用专用，进程需要读写权限，内部 `.tmp` 与对象目录处于同一文件系统，禁止外部程序修改、覆盖、添加符号链接或清理对象。
- 存储身份由 LOCAL 绝对真实根路径，或 RustFS 内部 endpoint 与 bucket 计算，不包含凭证和公开地址。所有 AVAILABLE/DELETING 对象必须与配置一致；变更后端或物理位置需先完成独立迁移。
- RustFS `endpoint` 用于应用读写，`public-endpoint` 用于签名；后者为空则使用前者。公网地址必须能从浏览器访问。生产配置 HTTPS，并保持签名时的 Host、路径与查询参数，代理不可重写签名路径。
- LOCAL 的 `access.public-base-url` 是应用公网地址，`access.sign-secret` 必须为各实例共用的稳定密钥。URL 按文件 ID、用途和到期时间签名。签发前鉴权，已签发链接可能持续有效至到期。
- 50MB 单文件、100MB 单请求；`ray.storage.upload-concurrency` 默认 8，超限立即拒绝，不排队；`operation-timeout` 默认 2 分钟，可配置为大于 0 且不超过 10 分钟。SDK 请求有界超时，LOCAL 写入逐块检查耗时。Multipart 使用磁盘临时文件（阈值 0），部署需提供足够临时磁盘空间。
- 签名 URL 使用有界进程缓存，每次使用仍检查权限和文件状态。浏览器只使用私有缓存；RustFS 覆盖响应为 `private, max-age=0`，LOCAL 缓存不超过链接剩余寿命。仅 PNG、JPEG、GIF、WebP inline，其余二进制附件。链接寿命档位不代表授权。

## 清理任务

初始化 SQL 默认启用 `fileCleanupTask.execute`，按应用时区每天 03:00 执行，任务管理可手动触发；周日附加孤儿扫描。Valkey 五分钟租约、每分钟续期只减少重复劳动，正确性由 PostgreSQL 逐对象锁和状态检查保证。

每轮读取系统参数，缺失使用默认值，格式错误或低于下限终止本轮：

| 参数 | 默认值 | 下限 |
| --- | --- | --- |
| file.unbound.expireHours | 24 小时 | 1 小时 |
| file.object.retentionDays | 3 天 | 1 天 |
| file.orphan.retentionDays | 7 天 | 7 天 |

上传开始时读取孤儿保留期，超期拒绝发布。接受跨越该期限恢复的极端暂停与清理之间的小窗口，不使用 key 锁或上传登记。关注任务结果中的失败数、字节缺失和业务引用异常；持续失败会在每轮日志中报告，DELETING 对象自动重试。逻辑文件与对象历史行保留，存储 key 不复用。

## 开发数据重建

旧路径字段和 API 已移除，没有兼容层。已有开发环境请先备份需要的数据，显式重建开发数据库并按文件名顺序执行 `docs/sql`，使用新的空存储根目录/桶；旧目录的废弃字节由操作者另行清理。应用启动不会删库、清空目录或自动迁移。不要在仍有其他实例使用的开发库中逐表执行重建。

## 可选部署增强

已有 Nginx 时可为文件响应增加：

```nginx
add_header X-Content-Type-Options nosniff always;
```

未添加此响应头不影响文件功能；应用仍执行图片白名单和其他文件附件下载规则。无需为此专门引入反向代理，应用不对此做启动检查或告警。
