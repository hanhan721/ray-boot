---
name: skill-guide
description: Claude Code SKILL 编写指南。当需要创建新的 SKILL、修改现有 SKILL、或了解 SKILL 配置时使用。
disable-model-invocation: true
---

# Claude Code SKILL 编写指南

## 概述

Skills 是 Claude Code 的扩展机制，通过 SKILL.md 文件为 Claude 提供特定领域的知识和指令。

**官方文档**: https://code.claude.com/docs/en/skills

## 文件结构

```
.claude/skills/
└── skill-name/
    ├── SKILL.md          # 必需，技能定义文件
    ├── templates/        # 可选，模板文件
    ├── examples/         # 可选，示例代码
    └── scripts/          # 可选，辅助脚本
```

## SKILL.md 结构

SKILL.md 由两部分组成：**YAML frontmatter** + **Markdown 内容**

```markdown
---
name: skill-name
description: 技能描述，Claude 根据此描述自动匹配触发
allowed-tools: Read, Grep, Glob
model: claude-sonnet-4-20250514
disable-model-invocation: false
---

# 技能标题

正文内容...
```

## Frontmatter 配置项

| 字段 | 必需 | 说明 |
|------|------|------|
| `name` | 是 | 技能名称，作为 `/slash-command` 使用 |
| `description` | 是 | 技能描述，Claude 用于语义匹配自动触发 |
| `allowed-tools` | 否 | 允许使用的工具列表，逗号分隔 |
| `model` | 否 | 指定模型，如 `claude-sonnet-4-20250514` 或 `inherit` |
| `disable-model-invocation` | 否 | 设为 `true` 时只能手动 `/skill-name` 调用 |
| `version` | 否 | 版本号，用于跟踪变更 |

## 存储位置与优先级

| 位置 | 路径 | 范围 |
|------|------|------|
| 企业级 | 企业配置 | 全组织共享 |
| 个人级 | `~/.claude/skills/` | 个人所有项目 |
| 项目级 | `.claude/skills/` | 当前项目 |

**优先级**: 企业 > 个人 > 项目（同名时高优先级覆盖）

## Description 编写技巧

Description 是 Claude 自动触发技能的关键，应包含：

1. **核心功能**：技能做什么
2. **触发场景**：何时应该使用
3. **关键词**：相关的技术术语

### 好的 Description 示例

```yaml
# 触发丰富，场景明确
description: MyBatis-Plus-Join (MPJ) 联表查询指南。当需要编写多表联查、关联查询代码时自动应用。提供 MPJ 的正确用法、常见模式和最佳实践。

# 多场景覆盖
description: 代码重构助手。当需要优化代码质量、识别代码坏味道、或重构现有代码时使用。

# 项目上下文
description: Ray-Boot 项目上下文感知。当开始新对话、编写代码、或需要了解项目规范时自动应用。
```

### 差的 Description 示例

```yaml
# 太简短，缺少触发场景
description: MPJ 用法

# 太模糊
description: 帮助编写代码
```

## Markdown 内容结构建议

```markdown
# 技能标题

## 概述
简要说明技能用途和适用场景

## 核心规则/原则
最重要的规则，必须遵守

## 常用方法/模式
具体的使用方法和代码模式

## 示例
实际的代码示例，展示正确用法

## 注意事项
常见错误和避免方法

## 何时使用/不使用
明确边界，避免误用
```

## 最佳实践

### 1. 保持简洁

- SKILL.md 控制在 **500 行以内**
- 聚焦单一主题，避免大而全
- 使用表格、列表提高可读性

### 2. 提供具体示例

```markdown
## 正确示例 ✓
\`\`\`java
var wrapper = new MPJLambdaWrapper<User>()
    .selectAll(User.class)
    .leftJoin(Dept.class, Dept::getId, User::getDeptId);
\`\`\`

## 错误示例 ✗
\`\`\`java
// 泛型与 Mapper 不匹配
var wrapper = new MPJLambdaWrapper<Dept>()
    ...
userMapper.selectJoinList(...);
\`\`\`
```

### 3. 使用表格对比

```markdown
| 场景 | 推荐方案 | 原因 |
|------|----------|------|
| 简单联表 | MPJ | 无需 XML |
| 复杂子查询 | XML | MPJ 不支持 |
```

### 4. 引用其他技能

```markdown
更多项目规范请参考 `/project-context` 技能。
```

### 5. 版本控制

```yaml
---
name: my-skill
version: 1.0.0
description: ...
---
```

## 调用方式

### 自动调用

Claude 根据 `description` 语义匹配，自动加载相关技能。

### 手动调用

```
/skill-name
```

### 禁止自动调用

```yaml
disable-model-invocation: true
```

设置后只能通过 `/skill-name` 手动调用。

## 完整示例

```markdown
---
name: api-design
description: RESTful API 设计指南。当设计新接口、编写 Controller、或讨论 API 规范时自动应用。
allowed-tools: Read, Grep, Glob
---

# RESTful API 设计指南

## 核心原则

1. 使用 HTTP 动词表达操作意图
2. URL 使用名词复数形式
3. 返回合适的 HTTP 状态码

## URL 设计规范

| 操作 | 方法 | URL | 示例 |
|------|------|-----|------|
| 创建 | POST | `/resources` | POST /users |
| 查询列表 | GET | `/resources` | GET /users |
| 查询详情 | GET | `/resources/{id}` | GET /users/1 |
| 更新 | PUT | `/resources/{id}` | PUT /users/1 |
| 删除 | DELETE | `/resources/{id}` | DELETE /users/1 |

## 响应格式

\`\`\`json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
\`\`\`

## 注意事项

- 避免在 URL 中使用动词
- 使用连字符 `-` 而非下划线 `_`
- 版本号放在 Header 而非 URL
```

## 调试技巧

1. **检查触发**: 观察 Claude 是否在相关任务中加载了技能
2. **手动测试**: 使用 `/skill-name` 确认技能可用
3. **查看日志**: 检查是否有语法错误
4. **简化测试**: 先用最小配置验证，再逐步添加内容
