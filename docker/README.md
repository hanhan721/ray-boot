# 中间件快速部署

使用 Docker Compose 快速部署项目所需的中间件服务：

- **PostgreSQL 18** - 关系型数据库
- **Valkey 9.1** - 兼容 Redis 的缓存服务
- **RustFS 1.0.4** - 对象存储服务

## 快速开始

### 1. 配置环境变量

```bash
# 复制配置模板
cd docker
cp .env.production .env

# 编辑配置文件，修改密码等敏感信息
vim .env
```

### 2. 启动服务

```bash
# 启动所有中间件
docker compose up -d

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 3. 连接信息

| 服务 | 本地端口 | 说明 |
|------|---------|------|
| PostgreSQL | 5432 | 数据库 |
| Valkey | 6379 | 缓存 |
| RustFS API | 9000 | 对象存储 API |
| RustFS Console | 9001 | 对象存储控制台 |

> 以上为 `.env.production` 中的默认端口，可通过 `.env` 中的 `PG_PORT`、`VALKEY_PORT`、`RUSTFS_API_PORT`、`RUSTFS_CONSOLE_PORT` 修改。

### 4. 停止服务

```bash
# 停止服务
docker compose down

# 停止服务并删除数据卷（谨慎操作）
docker compose down -v
```

## 数据持久化

所有数据通过 Docker volumes 持久化存储：

- `postgres-data` - PostgreSQL 数据
- `valkey-data` - Valkey 数据
- `rustfs-data` - RustFS 数据

## 初始化数据库

首次启动 PostgreSQL 时会自动按文件名顺序执行 `docs/sql/` 目录下的 SQL 脚本进行初始化（表结构 `sys_*.sql` 在前，初始化数据 `zz_init_data.sql` 在后，请保持该命名顺序）。
