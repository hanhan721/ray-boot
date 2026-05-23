---
name: refactor
description: 代码重构助手。当需要优化代码质量、识别代码坏味道、或重构现有代码时使用。
---

# 代码重构助手

识别代码坏味道，提供重构方案并实施。

## 使用方式

```
/refactor [文件路径或目录]
```

**示例:**
```
/refactor UserServiceImpl.java              # 重构指定文件
/refactor src/main/java/com/ray/system/  # 重构整个模块
/refactor                                    # 重构最近修改的文件
```

## 识别的代码坏味道

### 1. 架构层面

#### Controller 包含业务逻辑
```java
// ❌ 坏味道
@PostMapping
public Result<Void> create(@RequestBody User user) {
    if (userMapper.selectByUsername(user.getUsername()) != null) {
        return Result.error("用户名已存在");
    }
    userMapper.insert(user);
    return Result.success();
}

// ✅ 重构后
@PostMapping
public Result<Long> create(@Valid @RequestBody User user) {
    return Result.success(userService.create(user));
}
```

#### Service 直接返回 Entity 中的敏感字段
```java
// ❌ 坏味道：返回包含密码的 User
public User getById(Long id) {
    return userMapper.selectById(id);
}

// ✅ 重构后：使用 VO 隐藏敏感字段
public UserVO getById(Long id) {
    User user = userMapper.selectById(id);
    return userConvert.toVO(user);
}
```

### 2. 代码质量

#### 过度封装（简单场景用了 DTO）
```java
// ❌ 坏味道：简单 CRUD 却创建了 DTO
public Long create(UserCreateDTO dto) {
    User user = userConvert.toEntity(dto);
    userMapper.insert(user);
    return user.getId();
}

// ✅ 重构后：直接用 Entity
public Long create(User user) {
    userMapper.insert(user);
    return user.getId();
}
```

#### 手动对象复制（应该用 MapStruct）
```java
// ❌ 坏味道
UserVO vo = new UserVO();
vo.setId(user.getId());
vo.setUsername(user.getUsername());
vo.setNickname(user.getNickname());

// ✅ 重构后
UserVO vo = userConvert.toVO(user);
```

#### 魔法值
```java
// ❌ 坏味道
if (user.getStatus() == 0) { ... }

// ✅ 重构后
if (UserStatus.DISABLED.getCode().equals(user.getStatus())) { ... }
// 或使用常量
if (user.getStatus().equals(UserConstants.STATUS_DISABLED)) { ... }
```

### 3. 性能问题

#### N+1 查询
```java
// ❌ 坏味道
List<User> users = userMapper.selectList();
for (User user : users) {
    Dept dept = deptMapper.selectById(user.getDeptId());
    user.setDeptName(dept.getName());
}

// ✅ 重构方案 1：关联查询
List<UserVO> users = userMapper.selectWithDept();

// ✅ 重构方案 2：批量查询
List<User> users = userMapper.selectList();
Set<Long> deptIds = users.stream().map(User::getDeptId).collect(Collectors.toSet());
Map<Long, Dept> deptMap = deptMapper.selectBatchIds(deptIds)
    .stream().collect(Collectors.toMap(Dept::getId, Function.identity()));
users.forEach(u -> u.setDeptName(deptMap.get(u.getDeptId()).getName()));
```

#### 循环内数据库操作
```java
// ❌ 坏味道
for (User user : users) {
    userMapper.insert(user);
}

// ✅ 重构后
userMapper.insertBatch(users);
// 或使用 MyBatis-Plus
saveBatch(users);
```

### 4. 事务问题

#### 事务范围过大
```java
// ❌ 坏味道
@Transactional
public void process(Order order) {
    orderMapper.insert(order);
    sendEmail(order);  // IO 操作不应在事务中
    sendSms(order);
}

// ✅ 重构后
public void process(Order order) {
    createOrder(order);
    sendEmail(order);
    sendSms(order);
}

@Transactional(rollbackFor = Exception.class)
public void createOrder(Order order) {
    orderMapper.insert(order);
}
```

#### 同类方法调用事务失效
```java
// ❌ 坏味道
public void batchCreate(List<User> users) {
    for (User user : users) {
        create(user);  // 事务失效！
    }
}

@Transactional
public void create(User user) {
    userMapper.insert(user);
}

// ✅ 重构后：注入自身
@Autowired
private UserService self;

public void batchCreate(List<User> users) {
    for (User user : users) {
        self.create(user);  // 通过代理调用
    }
}
```

### 5. 安全问题

#### SQL 注入风险
```java
// ❌ 坏味道
@Select("SELECT * FROM user WHERE name LIKE '%" + name + "%'")

// ✅ 重构后
@Select("SELECT * FROM user WHERE name LIKE CONCAT('%', #{name}, '%')")
```

#### 敏感信息日志
```java
// ❌ 坏味道
log.info("用户登录: {}", user);  // 可能打印密码

// ✅ 重构后
log.info("用户登录: id={}, username={}", user.getId(), user.getUsername());
```

## 输出格式

```markdown
## 重构报告

### 文件: {文件路径}

#### 发现的问题

| # | 类型 | 位置 | 问题描述 |
|---|------|------|----------|
| 1 | 架构 | 第 xx 行 | Controller 包含业务逻辑 |
| 2 | 性能 | 第 xx 行 | N+1 查询问题 |

#### 重构方案

**问题 1: Controller 包含业务逻辑**

原代码:
```java
// 原代码
```

重构后:
```java
// 重构后代码
```

### 重构清单

- [ ] 问题 1: 将业务逻辑移到 Service
- [ ] 问题 2: 优化 N+1 查询
```

## 执行步骤

1. 读取目标文件
2. 按坏味道清单逐项检查
3. 生成重构报告
4. 询问用户是否执行重构
5. 实施代码修改
6. 验证修改正确性
