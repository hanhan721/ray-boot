---
name: feature-dev
description: 端到端功能开发。当用户描述一个新功能需求时使用，自动完成从数据库设计到 API 实现的完整开发流程。
---

# 端到端功能开发

接收功能需求描述，自动完成完整的开发流程。

## 使用方式

```
/feature-dev <功能描述>
```

**示例:**
```
/feature-dev 实现文章管理功能，支持发布、编辑、删除、分页查询
/feature-dev 添加系统通知功能，支持发送通知给指定用户或全体用户
/feature-dev 实现数据字典管理，支持字典类型和字典数据的 CRUD
```

## 开发流程

### 第一步：需求分析

1. 理解功能需求
2. 确定所属模块（system/新模块）
3. 识别核心实体和关系
4. 确定需要的 API 接口

### 第二步：数据库设计

1. 设计表结构
2. 生成 SQL 文件到 `docs/sql/{table_name}.sql`
3. 命名规范: 表名小写下划线，如 `sys_role.sql`

**表结构模板:**
```sql
CREATE TABLE {table_name} (
    id              BIGINT          NOT NULL,                               -- 主键
    -- 业务字段

    create_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 创建时间
    create_by       BIGINT,                                                 -- 创建人ID
    update_time     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,    -- 更新时间
    update_by       BIGINT,                                                 -- 更新人ID
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,                 -- 逻辑删除
    PRIMARY KEY (id)
);

-- 索引...
```

### 第三步：生成 Entity

位置: `ray-system/ray-system-biz/src/main/java/com/ray/system/entity/`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("表名")
public class EntityName extends BaseEntity {

    @TableField("column_name")
    private String fieldName;
}
```

**类型映射:**
| PostgreSQL | Java |
|------------|------|
| bigint | Long |
| varchar/text | String |
| boolean | Boolean |
| timestamp | LocalDateTime |
| int | Integer |
| numeric | BigDecimal |
| jsonb | String |

### 第四步：生成 Mapper

```java
@Mapper
public interface EntityMapper extends MPJBaseMapper<Entity> {
    // 继承 MPJBaseMapper 以支持联表查询
    // 简单联表查询使用 MPJ，无需写 XML
    // 复杂 SQL（子查询、复杂聚合等）才写在 XML 中
}
```

**联表查询示例:**
```java
// 简单联表查询，使用 MPJLambdaWrapper
MPJLambdaWrapper<User> wrapper = new MPJLambdaWrapper<User>()
    .selectAll(User.class)
    .select(Dept::getName)  // 关联查询部门名称
    .leftJoin(Dept.class, Dept::getId, User::getDeptId)
    .eq(User::getStatus, 1);
List<UserVO> list = userMapper.selectJoinList(UserVO.class, wrapper);
```

### 第五步：生成 Service

**接口:**
```java
public interface EntityService {
    Long create(Entity entity);
    void update(Entity entity);
    void delete(Set<Long> ids);
    Entity getById(Long id);
    IPage<Entity> page(PageQuery query);
    List<Entity> list();
}
```

**实现:**
```java
@Service
@RequiredArgsConstructor
public class EntityServiceImpl extends ServiceImpl<EntityMapper, Entity> implements EntityService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Entity entity) {
        this.checkUnique(entity);  // 调用本类方法用 this
        super.save(entity);        // 调用父类方法用 super
        return entity.getId();
    }

    @Override
    public IPage<Entity> page(PageQuery query) {
        return super.page(PageUtils.buildPage(query), new LambdaQueryWrapper<>());
    }
    // 其他方法...
}
```

### 第六步：生成 Controller

```java
@Tag(name = "实体管理")
@RestController
@RequestMapping("/api/system/entity")
@RequiredArgsConstructor
public class EntityController {

    private final EntityService entityService;

    @Operation(summary = "创建")
    @PostMapping
    @PreAuthorize("hasAuthority('system:entity:create')")
    public Result<Long> create(@Valid @RequestBody Entity entity) {
        return Result.success(entityService.create(entity));
    }

    // 其他接口...
}
```

### 第七步：按需生成 VO

仅当需要隐藏敏感字段或展开关联字段时生成。

```java
@Data
@Schema(description = "实体VO")
public class EntityVO {
    // 排除敏感字段
    // 展开关联字段（如 deptId -> deptName）
}
```

### 第八步：权限配置

1. 在 `sys_menu` 表添加菜单和按钮权限
2. 权限标识格式: `{module}:{entity}:{action}`
   - `system:user:create`
   - `system:user:update`
   - `system:user:delete`
   - `system:user:query`

## 输出清单

完成后列出所有生成的文件:

```
✅ 已生成文件:
├── docs/sql/xxx.sql
├── entity/Xxx.java
├── mapper/XxxMapper.java
├── service/XxxService.java
├── service/impl/XxxServiceImpl.java
├── controller/XxxController.java
└── vo/XxxVO.java (如有)

📋 待手动操作:
- [ ] 执行数据库 SQL
- [ ] 配置菜单权限
- [ ] 前端页面开发
```

## 注意事项

1. 简单 CRUD 直接用 Entity，不要过度封装
2. **简单联表查询使用 MyBatis-Plus-Join (MPJ)**，不要写 XML
3. 复杂 SQL（子查询、复杂聚合、动态表名）才写 Mapper XML
4. Mapper 继承 `MPJBaseMapper<T>` 以支持联表查询
5. 事务注解加在 Service 实现类的写方法上
6. Controller 只做校验和封装，不写业务逻辑
7. 权限注解使用 `@PreAuthorize`
8. 调用父类方法使用 `super.method()`，调用本类方法使用 `this.method()`
9. 数据库有 DEFAULT 的字段，Java 不设默认值
