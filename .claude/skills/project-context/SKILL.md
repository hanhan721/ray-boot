---
name: project-context
description: Ray-Boot 项目上下文感知。当开始新对话、编写代码、或需要了解项目规范时自动应用。帮助 Claude 理解项目结构、技术栈和编码规范。
---

# Ray-Boot 项目上下文

## 项目概述

Ray-Boot 是基于 Spring Boot 4.0.x 的后台管理系统脚手架，采用经典三层架构。

## 技术栈速查

| 类别 | 选型 |
|------|------|
| 语言 | Java 25 (LTS) |
| 框架 | Spring Boot 4.0.x / Spring Framework 7.x |
| 安全 | Spring Security 7.x + JWT |
| 数据库 | PostgreSQL 17 |
| ORM | MyBatis-Plus 3.5.15 + MyBatis-Plus-Join |
| 缓存 | Valkey 9.0 |
| 对象转换 | MapStruct 1.5.x |
| API 文档 | Knife4j 4.x |

## 项目结构

```
ray-boot/
├── ray-common/          # 公共模块
│   ├── ray-common-core/      # 核心工具、常量、异常
│   ├── ray-common-valkey/    # Valkey 缓存封装
│   ├── ray-common-mybatis/   # MyBatis-Plus 配置
│   └── ray-common-security/  # Security 配置
├── ray-framework/       # 框架模块
│   ├── ray-framework-web/    # Web 配置
│   └── ray-framework-log/    # 日志模块
├── ray-system/          # 系统管理模块（用户、角色、菜单、部门、字典、日志）
│   ├── ray-system-api/       # API 定义
│   └── ray-system-biz/       # 业务实现
├── ray-business/        # 业务模块（业务功能实现）
└── ray-boot-app/        # 启动模块
```

## 模块依赖

```
ray-boot-app → ray-system-biz, ray-business
                        ↓
                 ray-framework
                        ↓
                  ray-common
```

- **common**: 基础层，无依赖，被所有模块引用
- **framework**: 依赖 common，提供 Web、日志等框架能力
- **system**: 依赖 framework，系统管理功能
- **business**: 依赖 framework，业务功能实现
- **boot**: 依赖 system + business，启动入口

## 包结构规范

```
com.ray.{module}
├── controller/       # REST 接口
├── service/          # 服务接口
│   └── impl/         # 服务实现
├── mapper/           # 数据访问
├── entity/           # 实体类
├── vo/               # 视图对象（按需）
├── dto/              # DTO（仅复杂场景）
├── convert/          # MapStruct 转换器（按需）
└── enums/            # 枚举
```

## 核心编码规范

### 简单优先原则

| 场景 | 入参 | 出参 | 说明 |
|------|------|------|------|
| 简单 CRUD | Entity | Entity | 直接使用实体类 |
| 隐藏敏感字段 | Entity | VO | 如密码、密钥 |
| 复杂业务 | DTO | VO | 入参与实体差异大 |

- **数据库有 DEFAULT 的字段，Java 不设默认值**，避免冗余

### 分层职责

- **Controller**: 只做参数校验 + 结果封装，禁止业务逻辑
- **Service**: 业务逻辑 + 事务控制，接口与实现分离
- **Mapper**: 数据访问，继承 `MPJBaseMapper`，简单联表用 MPJ，复杂 SQL 写 XML

### 命名规范

| 类型 | 风格 | 示例 |
|------|------|------|
| 类名 | PascalCase | `UserController`, `SysUser` |
| 方法 | camelCase | `getUserById` |
| 常量 | UPPER_SNAKE | `MAX_PAGE_SIZE` |
| 数据库表 | 小写下划线 | `sys_user` |

### 方法调用规范

- 调用**父类方法**使用 `super.method()`
- 调用**本类方法**使用 `this.method()`

### RESTful API 规范

| 操作 | 方法 | URL | Body |
|------|------|-----|------|
| 创建 | POST | `/api/{module}/{entity}` | Entity |
| 更新 | PUT | `/api/{module}/{entity}` | Entity |
| 删除 | DELETE | `/api/{module}/{entity}` | Set\<Long\> |
| 详情 | GET | `/api/{module}/{entity}/{id}` | - |
| 分页 | GET | `/api/{module}/{entity}/page` | - |
| 列表 | GET | `/api/{module}/{entity}/list` | - |

### 统一响应

```java
{
  "code": 200,    // 200成功, 401未登录, 403无权限, 555业务错误
  "msg": "ok",
  "data": {}
}
```

## 常用注解

### Controller 层
```java
@Tag(name = "用户管理")                    // Swagger 分组
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:user:query')")  // 权限
```

### Service 层
```java
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)  // 写操作加事务
```

### Entity 层
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class User extends BaseEntity { }
```

## 在编写代码时

1. 优先使用 Entity，避免过度封装
2. 使用 Lombok 减少样板代码
3. 需要对象转换时用 MapStruct
4. Controller 不写业务逻辑
5. Service 处理事务和业务
6. **简单联表查询使用 MyBatis-Plus-Join (MPJ)**，无需写 XML
7. 复杂 SQL（子查询、复杂聚合、动态表名）才写 Mapper XML

## 联表查询规范

**使用 MPJ 的场景（推荐）:**
- 简单的 LEFT JOIN / INNER JOIN
- 关联查询少量字段（如获取关联表的名称）
- 条件过滤关联表字段

**使用 XML 的场景:**
- 复杂子查询
- 多层嵌套查询
- 复杂聚合统计
- 动态表名/动态列

**示例代码:**
```java
// Mapper 继承 MPJBaseMapper
public interface UserMapper extends MPJBaseMapper<User> { }

// Service 中使用 MPJLambdaWrapper 进行联表查询
MPJLambdaWrapper<User> wrapper = new MPJLambdaWrapper<User>()
    .selectAll(User.class)
    .select(Dept::getName)
    .leftJoin(Dept.class, Dept::getId, User::getDeptId)
    .eq(User::getStatus, 1);
List<UserVO> list = userMapper.selectJoinList(UserVO.class, wrapper);
```

## 分页规范

- **直接使用 MyBatis-Plus 的 `IPage`**
- 入参使用 `PageQuery`，通过 `PageUtils.buildPage(query)` 转换
- Service 返回 `IPage<T>`，Controller 直接返回 `R<IPage<T>>`

**示例代码:**
```java
// Controller
@GetMapping("/page")
public R<IPage<User>> page(@Valid PageQuery query) {
    return R.ok(userService.page(query));
}

// Service
public IPage<User> page(PageQuery query) {
    return page(PageUtils.buildPage(query), new LambdaQueryWrapper<>());
}
```
