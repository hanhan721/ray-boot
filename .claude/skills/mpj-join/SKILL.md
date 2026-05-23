---
name: mpj-join
description: MyBatis-Plus-Join (MPJ) 联表查询指南。当需要编写多表联查、关联查询代码时自动应用。提供 MPJ 的正确用法、常见模式和最佳实践。
---

# MyBatis-Plus-Join (MPJ) 联表查询指南

## 概述

MPJ 是 MyBatis-Plus 的增强工具，支持 MP 风格的连表操作，无需编写 XML 即可实现多表联查。

**官方文档**: https://mybatis-plus-join.github.io/

## 核心规则

### 1. Mapper 必须继承 MPJBaseMapper

```java
@Mapper
public interface UserMapper extends MPJBaseMapper<User> {
}
```

### 2. MPJLambdaWrapper 泛型必须是主表

**关键**: 泛型必须是主表实体类，且必须使用主表的 Mapper 调用查询方法。

```java
// 正确 ✓ - 泛型是主表 User，使用 userMapper 调用
var wrapper = new MPJLambdaWrapper<User>()
    .selectAll(User.class)
    .leftJoin(Dept.class, Dept::getId, User::getDeptId);
userMapper.selectJoinList(UserVO.class, wrapper);

// 错误 ✗ - 泛型与 Mapper 不匹配
var wrapper = new MPJLambdaWrapper<Dept>()  // 泛型是 Dept
    ...
userMapper.selectJoinList(...);  // 但用的是 userMapper
```

## 常用方法

### 查询字段选择

```java
new MPJLambdaWrapper<User>()
    .selectAll(User.class)                    // 查询主表全部字段
    .select(Dept::getName)                    // 查询关联表单个字段
    .select(Dept::getName, Dept::getCode)     // 查询关联表多个字段
    .selectAs(Dept::getName, UserVO::getDeptName)  // 别名映射
```

### 连接方式

```java
// LEFT JOIN
.leftJoin(Dept.class, Dept::getId, User::getDeptId)

// INNER JOIN
.innerJoin(Dept.class, Dept::getId, User::getDeptId)

// RIGHT JOIN
.rightJoin(Dept.class, Dept::getId, User::getDeptId)
```

### 条件过滤

```java
.eq(User::getStatus, 1)                    // 主表条件
.eq(Dept::getEnabled, true)                // 关联表条件
.like(User::getName, "张")
.in(User::getRoleId, roleIds)
.isNotNull(User::getEmail)
```

## 查询方法

### 查询列表

```java
List<UserVO> list = userMapper.selectJoinList(UserVO.class, wrapper);
```

### 查询单条

```java
UserVO user = userMapper.selectJoinOne(UserVO.class, wrapper);
```

### 分页查询

```java
IPage<UserVO> page = userMapper.selectJoinPage(
    new Page<>(1, 10),
    UserVO.class,
    wrapper
);
```

### 查询数量

```java
Long count = userMapper.selectJoinCount(wrapper);
```

## 常见场景示例

### 场景 1: 用户关联部门

```java
// 查询用户列表，同时获取部门名称
var wrapper = new MPJLambdaWrapper<User>()
    .selectAll(User.class)
    .selectAs(Dept::getName, UserVO::getDeptName)
    .leftJoin(Dept.class, Dept::getId, User::getDeptId)
    .eq(User::getEnabled, true)
    .orderByDesc(User::getCreateTime);

List<UserVO> users = userMapper.selectJoinList(UserVO.class, wrapper);
```

### 场景 2: 多表联查（3 表）

```java
// 查询用户权限：User -> UserRole -> Role
var wrapper = new MPJLambdaWrapper<User>()
    .select(User::getId, User::getUsername)
    .select(Role::getRoleName, Role::getRoleKey)
    .innerJoin(UserRole.class, UserRole::getUserId, User::getId)
    .innerJoin(Role.class, Role::getId, UserRole::getRoleId)
    .eq(User::getId, userId);

List<UserRoleVO> roles = userMapper.selectJoinList(UserRoleVO.class, wrapper);
```

### 场景 3: 从主表反向联表

```java
// 查询权限编码：Permission -> RolePermission -> UserRole -> 过滤用户
// 主表是 Permission，使用 permissionMapper
var wrapper = new MPJLambdaWrapper<Permission>()
    .select(Permission::getCode)
    .innerJoin(RolePermission.class, RolePermission::getPermissionId, Permission::getId)
    .innerJoin(UserRole.class, UserRole::getRoleId, RolePermission::getRoleId)
    .eq(UserRole::getUserId, userId)
    .eq(Permission::getEnabled, true)
    .isNotNull(Permission::getCode);

List<Permission> permissions = permissionMapper.selectJoinList(Permission.class, wrapper);
Set<String> codes = permissions.stream()
    .map(Permission::getCode)
    .collect(Collectors.toSet());
```

### 场景 4: 分页 + 联表 + 条件

```java
public IPage<UserVO> pageWithDept(PageQuery query, String deptName) {
    var wrapper = new MPJLambdaWrapper<User>()
        .selectAll(User.class)
        .selectAs(Dept::getName, UserVO::getDeptName)
        .leftJoin(Dept.class, Dept::getId, User::getDeptId)
        .like(StringUtils.isNotBlank(deptName), Dept::getName, deptName)
        .orderByDesc(User::getCreateTime);

    return userMapper.selectJoinPage(PageUtils.buildPage(query), UserVO.class, wrapper);
}
```

## VO 类定义

联表查询返回的 VO 类通常继承主表实体或单独定义：

```java
// 方式 1: 继承主表实体
@Data
@EqualsAndHashCode(callSuper = true)
public class UserVO extends User {
    private String deptName;
    private String roleName;
}

// 方式 2: 独立定义
@Data
public class UserVO {
    private Long id;
    private String username;
    private String deptName;  // 关联字段
}
```

## 注意事项

1. **泛型一致性**: MPJLambdaWrapper 的泛型必须与调用的 Mapper 对应的实体类一致
2. **字段映射**: 使用 `selectAs` 时，VO 类必须有对应的字段
3. **避免 N+1**: 联表查询比循环单表查询性能更好
4. **复杂查询**: 涉及子查询、复杂聚合时，建议使用 XML

## 何时使用 MPJ vs XML

| 场景 | 推荐 |
|------|------|
| 简单 LEFT/INNER JOIN | MPJ |
| 关联查询少量字段 | MPJ |
| 条件过滤关联表 | MPJ |
| 复杂子查询 | XML |
| 动态表名/列名 | XML |
| 复杂聚合统计 | XML |
| 递归查询 | XML |
