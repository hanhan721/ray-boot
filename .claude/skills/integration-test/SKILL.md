---
name: integration-test
description: Controller 集成测试。当用户要求测试 Controller 接口功能、验证 API 端到端行为时使用。通过 HTTP 请求测试 Controller，使用 postgres-mcp 工具验证数据库数据正确性。
disable-model-invocation: true
---

# Controller 集成测试

## 概述

对 Controller 层进行端到端集成测试：发送 HTTP 请求 → 验证响应 → 通过 postgres-mcp 验证数据库状态。

**测试范围**: 用户手动指定的 Controller 方法或功能。

账号密码: admin/123456

## 测试流程

### 1. 准备阶段

- 阅读目标 Controller 代码，理解接口签名、URL、请求/响应格式
- 阅读对应 Service 实现，理解业务逻辑和数据库操作
- 阅读对应 Entity，了解表结构和字段
- 使用 `mcp__postgres-mcp__search_objects` 查看相关表结构

### 2. 数据准备

通过调用业务 API 接口准备测试数据：

- 调用创建接口（POST）插入测试所需的前置数据
- **禁止**直接使用 `mcp__postgres-mcp__execute_sql` 执行 INSERT/UPDATE/DELETE 语句
- 通过 API 调用可以确保业务逻辑被正确执行（如数据验证、默认值处理、日志记录等）

### 3. 执行测试

通过 curl 或应用启动后的 HTTP 请求测试接口。

**注意**: 项目使用 Spring Security，大部分接口需要认证。有两种方式：
- `/api/test/**` 路径已放行，可用于未认证测试
- 需认证接口：先调用登录接口获取 token，后续请求携带 Authorization header

### 4. 验证阶段

- 验证 HTTP 响应码和响应体
- 使用 `mcp__postgres-mcp__execute_sql` 查询数据库，验证数据变更是否正确
- 检查关联表数据是否正确维护

## 技术规范

### 项目技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 4.0.x | 基于 Spring Framework 7 |
| JUnit | 6 (Jupiter) | 2025-09 GA，Spring Boot 4 默认 |
| Java | 25 | |
| Mockito | 5.20+ | Spring Boot 4 内置 |
| MyBatis-Plus | 3.5.15 | |
| Spring Security | 7.x | |
| Jackson | 3.x | 包名 `tools.jackson`，注解兼容 |

### JUnit 6 要点

JUnit 6（2025-09-30 GA）是 Spring Boot 4 的默认测试框架，相比 JUnit 5：

**包名不变**: 核心注解仍在 `org.junit.jupiter.api`，无需改包名。

**统一版本号**: Platform / Jupiter / Vintage 统一版本（如 `6.0.2`），BOM 管理更简单。

**JSpecify 空安全**: 所有 API 使用 `@Nullable`、`@NonNull`、`@NullMarked` 注解。

**已移除的 API**（从 JUnit 5 迁移需注意）:
- `MethodOrderer.Alphanumeric` → 使用 `MethodOrderer.MethodName`
- `ReflectionSupport.loadClass(String)` → 使用带 ClassLoader 参数的重载
- `ExtensionContext.Store.getOrComputeIfAbsent` → 使用 `computeIfAbsent`
- `junit-platform-runner` 模块已移除
- Vintage 引擎已废弃（仅用于 JUnit 4 过渡）

**CSV 解析变更**: `@CsvSource`、`@CsvFileSource` 迁移到 FastCSV，`lineSeparator` 属性已移除。

### Spring Boot 4 测试变更

**`@MockBean` / `@SpyBean` 已移除**，替代方案：

| 旧注解 (Spring Boot 3.x) | 新注解 (Spring Framework 7) |
|---------------------------|----------------------------|
| `@MockBean` | `@MockitoBean` |
| `@SpyBean` | `@MockitoSpyBean` |
| — | `@TestBean`（静态工厂方法覆盖 Bean） |

```java
// 旧 (已移除)
import org.springframework.boot.test.mock.mockito.MockBean;
@MockBean private UserService userService;

// 新
import org.springframework.test.context.bean.override.mockito.MockitoBean;
@MockitoBean private UserService userService;
```

**注意**: `@MockitoBean` 不是 `@MockBean` 的 1:1 替换，行为有差异：
- `@MockitoBean` 在 `@TestConfiguration` + `@Import` 场景下行为不同
- Bean 覆盖机制基于 `@BeanOverride` 注解，支持非 singleton 作用域

**`MockitoTestExecutionListener` 已移除**: 必须使用 `@ExtendWith(MockitoExtension.class)` 来初始化 `@Mock` 和 `@Captor`。

### API 约定

- 基础路径: `/api/{module}/{entity}`
- 统一响应格式: `{"code": 200, "msg": "ok", "data": {}}`
- 成功码: 200 | 业务错误: 555 | 未登录: 401 | 无权限: 403

### 数据库约定

- 数据库: `ray` (PostgreSQL 18)
- ID 策略: 雪花算法 (assign_id)
- 逻辑删除: `deleted` 字段 (false=未删除, true=已删除)
- 表名前缀: `sys_` (系统模块)

## 测试验证清单

每个接口测试应覆盖：

### 正向测试
- [ ] 正常请求返回 200 + 正确数据
- [ ] 数据库记录正确创建/更新/删除

### 反向测试
- [ ] 参数校验失败返回对应错误
- [ ] 业务规则违反返回 555 + 错误信息

### 数据验证 (使用 postgres-mcp)
- [ ] 新增: `SELECT` 验证记录已插入，各字段值正确
- [ ] 更新: `SELECT` 验证字段已变更，未修改字段不变
- [ ] 删除: `SELECT` 验证逻辑删除字段已设置（或记录已物理删除）
- [ ] 关联表: 检查关联数据的一致性

## postgres-mcp 使用模式

### 查看表结构
```
mcp__postgres-mcp__search_objects: object_type=table, schema=ray
mcp__postgres-mcp__search_objects: object_type=column, schema=ray, table=sys_user, detail_level=full
```

### 数据准备

**通过 API 调用创建测试数据**（而不是直接执行 SQL）：

```bash
# 示例：通过创建用户接口插入测试数据
curl -X POST http://localhost:8080/api/system/user \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "username": "test_user1",
    "password": "123456",
    "nickname": "测试用户1",
    "enabled": 1
  }'

# 如果是公开接口，也可以直接调用登录、注册等接口
```

**禁止使用 SQL 直接插入数据**：
```sql
-- ❌ 不要这样做！
DELETE FROM sys_user WHERE username LIKE 'test_%';
INSERT INTO sys_user VALUES (...);
```

**原因**：
- 直接 SQL 绕过业务逻辑验证
- 无法测试数据验证、默认值处理、日志记录等完整流程
- API 集成测试应基于实际业务接口行为

### 验证数据
```sql
-- 验证新增
SELECT id, username, nickname, enabled, deleted FROM sys_user WHERE username = 'test_user1';

-- 验证更新
SELECT nickname, updated_at FROM sys_user WHERE id = 999001;

-- 验证逻辑删除
SELECT id, deleted FROM sys_user WHERE id = 999001;
-- deleted 应为 true 表示已删除

-- 验证关联数据
SELECT user_id, role_id FROM sys_user_role WHERE user_id = 999001;
```

## 执行步骤模板

当用户指定测试某个 Controller 功能时，按以下步骤执行：

1. **读取代码**: 读取 Controller → Service → Entity 代码
2. **查看表结构**: 用 postgres-mcp 确认表结构
3. **确认应用状态**: 确认应用是否已启动，确定请求地址和端口
4. **准备数据**: 通过调用业务 API 接口创建前置数据（不要直接执行 SQL）
5. **发送请求**: 用 curl 调用接口
6. **验证响应**: 检查 HTTP 状态码和响应体
7. **验证数据库**: 用 postgres-mcp 查询验证数据变更（仅查询，不执行 DML）
8. **报告结果**: 汇总测试结果，标注通过/失败

## 注意事项

- 雪花算法 ID 很长（如 1892736425163800577），SQL 查询时注意精度
- 逻辑删除后 `deleted` 字段值为 `true`
- 密码字段经过 BCrypt 加密，不能直接比较明文
- 分页查询默认 page=1, size=10
- JSON 请求需要 `Content-Type: application/json`
- 时间字段使用 `Asia/Shanghai` 时区
