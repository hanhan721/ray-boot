# ray-Boot

Spring Boot 4.0.x 后台管理系统脚手架，三层架构（Controller-Service-Mapper）。

## 技术栈

Java 25 | Spring Boot 4.0.x | Spring Security 7.x + JWT | PostgreSQL 18 | MyBatis-Plus 3.5.15 | Valkey 9.1 | MapStruct | Knife4j

## Java 现代特性 (25)

项目使用 Java 25，应优先使用以下现代特性：

### Record（数据载体）
替代传统 POJO，用于不可变数据传输对象：
```java
// 替代 @Data 类，自动生成构造器、getter、equals、hashCode、toString
public record UserVO(Long id, String username, String nickname) {}

// 紧凑构造器用于校验
public record PageQuery(int page, int size) {
    public PageQuery {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
    }
}
```
**适用场景**: VO、DTO、API 响应、配置参数

### Sealed Class（密封类）
限制类的继承，配合模式匹配使用：
```java
public sealed interface Result<T> permits Success, Failure {
    record Success<T>(T data) implements Result<T> {}
    record Failure<T>(String error) implements Result<T> {}
}
```

### Pattern Matching（模式匹配）
```java
// instanceof 模式匹配
if (obj instanceof String s && s.length() > 0) {
    return s.toUpperCase();
}

// switch 模式匹配（推荐）
String result = switch (obj) {
    case Integer i -> "整数: " + i;
    case String s  -> "字符串: " + s;
    case null      -> "空值";
    default        -> "未知类型";
};

// Record 解构
if (user instanceof UserVO(var id, var name, _)) {
    log.info("用户 {} 的ID是 {}", name, id);
}
```

### Switch 表达式
```java
// 箭头语法，自动 break，可返回值
int days = switch (month) {
    case 1, 3, 5, 7, 8, 10, 12 -> 31;
    case 4, 6, 9, 11 -> 30;
    case 2 -> isLeapYear ? 29 : 28;
};
```

### Text Block（文本块）
```java
String sql = """
    SELECT id, username, nickname
    FROM sys_user
    WHERE status = 1
      AND deleted = 0
    """;

String json = """
    {"code": 200, "msg": "ok"}
    """;
```

### Virtual Thread（虚拟线程）
高并发 I/O 密集型任务首选：
```java
// 创建虚拟线程
Thread.startVirtualThread(() -> doTask());

// ExecutorService 使用虚拟线程
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> callRemoteApi());
}
```

### Sequenced Collections（有序集合）
```java
List<String> list = new ArrayList<>();
list.addFirst("first");   // 添加到开头
list.addLast("last");     // 添加到末尾
list.getFirst();          // 获取第一个
list.getLast();           // 获取最后一个
list.reversed();          // 反转视图
```

### 其他实用特性
```java
// var 局部变量类型推断
var users = userService.list();
var map = new HashMap<String, List<User>>();

// Optional 增强
optional.orElseThrow();                    // 无参抛出 NoSuchElementException
optional.ifPresentOrElse(this::use, this::handleEmpty);

// Stream 增强
list.stream().toList();                    // 替代 collect(Collectors.toList())
Stream.of(1,2,3).mapMulti((i, consumer) -> { ... });
```

## Spring Boot 4 特性

### 模块化自动配置
自动配置 JAR 拆分为更小模块，按需引入，加快启动速度。

### API 版本控制
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping(headers = "X-API-Version=1")
    public List<UserV1VO> listV1() { ... }

    @GetMapping(headers = "X-API-Version=2")
    public List<UserV2VO> listV2() { ... }
}
```

### JSpecify 空安全
使用 `@Nullable` 和 `@NonNull` 注解，IDE 和编译器可检测空指针问题：
```java
import org.jspecify.annotations.Nullable;

public @Nullable User findByUsername(String username) { ... }
```

### Jackson 3.x（默认 JSON 库）
Spring Boot 4 默认使用 Jackson 3，主要变更：

**包名与坐标变更**：
```java
// Jackson 2.x → Jackson 3.x
// Maven groupId: com.fasterxml.jackson → tools.jackson
// 包名: com.fasterxml.jackson → tools.jackson
import tools.jackson.databind.JsonMapper;
import tools.jackson.databind.ObjectWriter;

// 注解包保持不变（兼容 2.x），仍使用：
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
```

**JsonMapper 替代 ObjectMapper**（不可变，线程安全）：
```java
// 构建 JsonMapper（Builder 模式）
JsonMapper mapper = JsonMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build();

// Spring Boot 自动配置了 JsonMapper Bean，直接注入即可
@Autowired
private JsonMapper jsonMapper;

// 自定义配置使用 JsonMapperBuilderCustomizer
@Bean
JsonMapperBuilderCustomizer customizer() {
    return builder -> builder.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
}
```

**Java 8 模块内置**（无需手动注册）：
- `java.time` 类型（LocalDateTime 等）直接支持
- `Optional` 直接支持
- 构造器参数名自动检测

**默认行为变更**：
- 日期默认序列化为 ISO-8601 字符串（不再是时间戳）
- `Locale` 序列化格式：`zh_CN` → `zh-CN`（LanguageTag 格式）
- 异常改为非受检异常（`JacksonException extends RuntimeException`）

### 平台基线
- Jakarta EE 11（Servlet 6.1, JPA 3.2）
- Hibernate ORM 7.0
- Jackson 3.x
- Spring Security 7.x（支持多因素认证）

### RestClient（推荐）
替代 RestTemplate，更现代的 API：
```java
@Bean
RestClient restClient(RestClient.Builder builder) {
    return builder.baseUrl("https://api.example.com").build();
}

// 使用
User user = restClient.get()
    .uri("/users/{id}", 1)
    .retrieve()
    .body(User.class);
```

### 声明式 HTTP 客户端
```java
@HttpExchange("/api/users")
public interface UserClient {

    @GetExchange("/{id}")
    User getById(@PathVariable Long id);

    @PostExchange
    User create(@RequestBody User user);
}
```

### 问题详情 (RFC 9457)
标准化错误响应格式：
```java
@ExceptionHandler(EntityNotFoundException.class)
ProblemDetail handleNotFound(EntityNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problem.setTitle("资源不存在");
    problem.setDetail(ex.getMessage());
    return problem;
}
```

## 常用命令

```bash
# Maven Wrapper（无需本地安装 Maven）
./mvnw clean compile                # 编译 (Unix/macOS)
./mvnw clean package -DskipTests    # 打包 (Unix/macOS)
./mvnw checkstyle:check             # 代码检查 (Unix/macOS)

# Windows 使用 mvnw.cmd
mvnw.cmd clean compile              # 编译 (Windows)
mvnw.cmd clean package -DskipTests  # 打包 (Windows)

# Docker
docker compose up -d             # 启动容器
docker compose down              # 停止容器
```

## 项目结构

```
docs/sql/                    # 数据库表定义文件
ray-common/                # 公共模块
  ├── ray-common-core          # 核心工具、通用响应、异常定义
  ├── ray-common-valkey        # Valkey 缓存封装
  ├── ray-common-mybatis       # MyBatis-Plus 配置
  └── ray-common-security      # 安全认证、JWT
ray-framework/             # 框架模块
  ├── ray-framework-web        # Web 配置、全局异常处理
  └── ray-framework-log        # 操作日志
ray-system/                # 系统模块
  ├── ray-system-api           # 系统接口定义、Entity、VO
  └── ray-system-biz           # 系统业务实现（用户、角色、菜单、部门、字典）
ray-business/              # 业务模块（空占位，按需扩展）
ray-boot-app/              # 启动模块
```

## 模块依赖

```
ray-boot-app → ray-system-biz, ray-schedule
                        ↓
                  ray-framework
                        ↓
                  ray-common
```

- **ray-common**: 基础层，无依赖，被所有模块引用
- **ray-framework**: 依赖 common，提供 Web、日志等框架能力
- **ray-system**: 依赖 framework，系统管理功能
- **ray-business**: 业务模块（空占位，按需添加子模块）
- **ray-boot-app**: 依赖 system + schedule，启动入口

## 核心规范

### 分层职责
- **Controller**: 参数校验 + 结果封装，禁止业务逻辑
- **Service**: 业务逻辑 + 事务控制，接口与实现分离
- **Mapper**: 数据访问，简单联表用 MPJ，复杂 SQL 写 XML

### 跨模块反向依赖
当低层模块需要检查高层模块的数据引用时，不能直接依赖高层模块（会产生循环依赖）。使用接口 + Spring 集合注入解决：
- 低层模块定义检查接口（如 `XxxReferenceChecker`）
- 高层模块提供实现
- 低层模块通过 `@Autowired(required = false) List<XxxChecker>` 注入所有实现
```java
// 低层模块定义接口
public interface XxxReferenceChecker {
    boolean isReferenced(Set<Long> ids);
}

// 高层业务模块实现
@Component
public class BizXxxReferenceChecker implements XxxReferenceChecker { ... }

// 低层模块中使用（required=false 防止无实现时启动失败）
@Autowired(required = false)
private List<XxxReferenceChecker> referenceCheckers;
```

### 联表查询规范
- **简单联表查询**: 使用 MyBatis-Plus-Join (MPJ)，无需写 XML
- **复杂 SQL**: 涉及子查询、复杂聚合、动态表名等场景才写 XML
- Mapper 继承 `MPJBaseMapper<T>` 以支持联表查询
- 使用 `MPJLambdaWrapper` 构建类型安全的联表条件

### 缓存规范
- **推荐使用 Spring Cache 注解**：`@Cacheable`、`@CacheEvict`、`@CachePut`
- **同一缓存多处使用时**：封装 Cache 类到对应业务模块（如 `UserCache`）
- **复杂场景**（分布式锁、计数器等）：直接使用 `ValkeyUtils`

### 权限控制规范
使用 `@PreAuthorize("@perm.has('permission:code')")` 进行权限检查，详见 `PermissionEvaluator` 组件。
权限与角色分离，授权变更后撤销受影响用户全部设备会话，要求重新登录。
普通已登录请求从 Token Hash 一次 HGETALL 读取身份及授权快照，认证鉴权不查询 SQL；菜单按需查询。
登录、刷新和授权变更通过 PostgreSQL 事务级 advisory lock 协调，先取得协调锁，再取得用户行锁。

### 响应字段翻译
需要在响应 JSON 中为编码、ID 等字段追加可读文本时使用字段翻译。先在 Controller 类或方法上标记 `@EnableTranslate`，再在 VO/Entity 字段上标记 `@Translate`。
翻译只在启用 `@EnableTranslate` 的接口生效；默认追加字段名为 `原字段名 + Text`，也可通过 `target` 指定。
```java
@EnableTranslate
@GetMapping("/page")
public R<IPage<OrderVO>> page(OrderPageQuery query) {
    return R.ok(orderService.page(query));
}

public class OrderVO {
    // 字典翻译：输出 status 和 statusText
    @Translate(type = "dict", value = "order_status")
    private Integer status;

    // 实体翻译：按 createBy 查询 SysUser.nickname，输出 createByText
    @Translate(type = "entity", entity = SysUser.class, valueField = "nickname")
    private Long createBy;

    // 指定输出字段名：输出 ownerName
    @Translate(type = "entity", entity = SysUser.class, valueField = "nickname", target = "ownerName")
    private Long ownerId;
}
```
内置翻译类型：
- `dict`: `value` 填字典类型，使用 `DictDataService#listByDictType` 查询。
- `entity`: `entity` 填实体类，`valueField` 填展示字段，`keyField` 默认 `id`。

### 字段级权限
需要按权限隐藏响应字段时，在 VO/Entity 字段上标记 `@FieldPermission("permission:code")`。无权限时该字段不会出现在 JSON 中，不是输出 `null`。
```java
public class UserVO {
    private Long id;
    private String username;

    @FieldPermission("system:user:phone")
    private String phone;

    @FieldPermission("system:user:salary")
    private BigDecimal salary;
}
```
`@FieldPermission` 可与 `@Translate` 配合使用；无权限时原字段和翻译字段都会被跳过，翻译预扫描也不会触发该字段或受保护嵌套对象内的翻译查询。

### 分页规范
- **直接使用 MyBatis-Plus 的 `IPage`**
- 入参使用 `PageQuery`，通过 `PageUtils.buildPage(query)` 转换
- Service 返回 `IPage<T>`，Controller 直接返回 `R<IPage<T>>`
```java
// Controller
public R<IPage<User>> page(@Valid PageQuery query) {
    return R.ok(userService.page(query));
}

// Service
public IPage<User> page(PageQuery query) {
    return page(PageUtils.buildPage(query), new LambdaQueryWrapper<>());
}
```

### 数据对象原则
- 简单 CRUD 直接用 Entity，避免过度封装
- 需要隐藏敏感字段时用 VO
- 仅复杂业务场景使用 DTO
- 简单赋值直接用 getter/setter，复杂对象转换用 MapStruct
- 对象赋值推荐使用链式 set 或 Builder 模式，避免多行独立 set
- **数据库有 DEFAULT 的字段，Java 不设默认值**，避免冗余

### 对象构建规范
```
// 推荐：Builder 模式（创建新对象）
User user = User.builder()
    .username("admin")
    .nickname("管理员")
    .status(1)
    .build();

// 推荐：链式 set（@Accessors(chain = true)）
user.setUsername("admin")
    .setNickname("管理员")
    .setStatus(1);
```

### 命名规范
- 类名: PascalCase (`UserController`)
- 方法: camelCase (`getUserById`)
- 常量: UPPER_SNAKE (`MAX_PAGE_SIZE`)
- 数据库: 小写下划线 (`sys_user`)

### 方法调用规范
- 调用**父类方法**使用 `super.method()`
- 调用**本类方法**使用 `this.method()`
```java
// 调用父类方法
super.save(entity);
super.updateById(entity);
super.removeByIds(ids);

// 调用本类方法
this.checkUnique(name, excludeId);
this.buildQueryWrapper(query);
```

### 工具类使用规范
- **字符串判空**: 使用 `org.springframework.util.StringUtils`
- **集合判空**: 使用 `org.springframework.util.CollectionUtils`
```java
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;

// 字符串判空
if (StringUtils.hasText(str)) { ... }      // 非空且非空白
if (StringUtils.hasLength(str)) { ... }    // 非空（可能是空白）

// 集合判空
if (CollectionUtils.isEmpty(list)) { ... }     // 为空或 null
if (!CollectionUtils.isEmpty(list)) { ... }    // 非空
```

### API 规范
| 操作 | 方法 | URL | Body |
|------|------|-----|------|
| 创建 | POST | `/api/{module}/{entity}` | Entity |
| 更新 | PUT | `/api/{module}/{entity}` | Entity |
| 删除 | DELETE | `/api/{module}/{entity}` | Set\<Long\> |
| 详情 | GET | `/api/{module}/{entity}/{id}` | - |
| 分页 | GET | `/api/{module}/{entity}/page` | - |

### 响应格式
```json
{"code": 200, "msg": "ok", "data": {}}
```
错误码: 200 成功 | 401 未登录 | 403 无权限 | 555 业务错误

### Git 提交

- 单元测试仅保留在本地，通过 `.git/info/exclude` 排除，不纳入 Git 跟踪或提交，不使用 `.gitignore` 排除。

```
<type>(<scope>): <subject>
```
type: feat | fix | docs | style | refactor | perf | test | chore
