# MeterSphere 权限模型完整解析

## 📊 核心概念

MeterSphere 采用 **RBAC（基于角色的访问控制）** 模型，通过三个维度控制用户权限：

```
用户 (User) + 用户组 (Group) + 资源 (Resource) = 权限 (Permission)
     ↓             ↓                ↓
  user_id      group_id         source_id
```

---

## 🗂️ 数据库表结构

### 1. user 表（用户）

```sql
CREATE TABLE `user` (
  `id` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  ...
  PRIMARY KEY (`id`)
);
```

**作用**：存储用户基本信息

---

### 2. group 表（用户组/角色）

```sql
CREATE TABLE `group` (
  `id` varchar(64) NOT NULL,
  `name` varchar(64) NOT NULL,
  `type` varchar(20) NOT NULL,      -- SYSTEM/WORKSPACE/PROJECT
  `scope_id` varchar(64) NOT NULL,  -- system/global/<工作空间ID>
  `system` tinyint(1) NOT NULL,     -- 是否系统预置
  ...
  PRIMARY KEY (`id`)
);
```

**关键字段**：
- `type`：用户组类型（SYSTEM/WORKSPACE/PROJECT）
- `scope_id`：用户组的作用域（决定在哪里可以被使用）
- `system`：是否系统预置（1=系统预置，0=用户创建）

**scope_id 的三种值**：
| 值 | 含义 | 示例 |
|---|------|------|
| `system` | 系统级别 | 超级管理员 |
| `global` | 全局级别 | 系统管理员、项目管理员、开发人员组、测试人员组 |
| `<工作空间UUID>` | 工作空间级别 | 某个工作空间的自定义用户组 |

---

### 3. user_group 表（用户-用户组-资源关联）

```sql
CREATE TABLE `user_group` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,    -- 用户ID
  `group_id` varchar(64) NOT NULL,   -- 用户组ID
  `source_id` varchar(64) NOT NULL,  -- 资源ID（项目/工作空间/系统）
  `create_time` bigint NOT NULL,
  `update_time` bigint NOT NULL,
  PRIMARY KEY (`id`)
);
```

**关键字段**：
- `user_id`：用户ID
- `group_id`：用户组ID
- `source_id`：**资源ID**（决定用户在哪个资源上拥有该用户组的权限）

**source_id 的含义**：
- 如果 `group.type = 'PROJECT'`，则 `source_id` 是**项目ID**
- 如果 `group.type = 'WORKSPACE'`，则 `source_id` 是**工作空间ID**
- 如果 `group.type = 'SYSTEM'`，则 `source_id` 是 `'system'`

---

### 4. user_group_permission 表（用户组权限）

```sql
CREATE TABLE `user_group_permission` (
  `id` varchar(64) NOT NULL,
  `group_id` varchar(64) NOT NULL,      -- 用户组ID
  `permission_id` varchar(128) NOT NULL, -- 权限ID
  `module_id` varchar(64) NOT NULL,     -- 功能模块ID
  PRIMARY KEY (`id`)
);
```

**作用**：定义每个用户组拥有哪些具体权限

---

## 🎯 权限判断逻辑

### 核心公式

```
用户在某个资源上的权限 = 
  SELECT ugp.permission_id
  FROM user_group ug
  INNER JOIN user_group_permission ugp ON ug.group_id = ugp.group_id
  WHERE ug.user_id = '用户ID'
    AND ug.source_id = '资源ID'
```

### 判断流程

```
1. 用户访问某个项目
   ↓
2. 查询 user_group 表
   WHERE user_id = '当前用户'
     AND source_id = '当前项目'
   ↓
3. 获取用户在该项目的所有用户组
   ↓
4. 查询 user_group_permission 表
   WHERE group_id IN (用户的用户组列表)
   ↓
5. 获取用户在该项目的所有权限
   ↓
6. 判断用户是否有权限执行当前操作
```

---

## 📋 实际案例分析

### 案例1：admin 用户的权限分配

从测试环境查询到的数据：

```sql
-- admin 用户有 16 条权限记录
user_id: admin
user_name: Administrator

-- 记录1：项目管理员 @ 项目1
group_id: project_admin (项目管理员)
group_type: PROJECT
scope_id: global
source_id: 1ad3f2bb-2cd9-4ca4-990d-d44824a57ffd (项目1)

-- 记录2：项目管理员 @ 项目2
group_id: project_admin (项目管理员)
group_type: PROJECT
scope_id: global
source_id: 50ecbcda-2f25-40c2-b9cc-cf8eb27f4403 (项目2)

-- ... 共13个项目的项目管理员权限

-- 记录14：系统管理员 @ 系统
group_id: admin (系统管理员)
group_type: SYSTEM
scope_id: global
source_id: system

-- 记录15：超级管理员 @ 系统
group_id: super_group (超级管理员)
group_type: SYSTEM
scope_id: system
source_id: system

-- 记录16：工作空间管理员 @ 工作空间1
group_id: ws_admin (工作空间管理员)
group_type: WORKSPACE
scope_id: global
source_id: 698604cf-d1a4-11f0-a2f8-cead5f5242ae (工作空间1)

-- 记录17：工作空间管理员 @ 工作空间2
group_id: ws_admin (工作空间管理员)
group_type: WORKSPACE
scope_id: global
source_id: 6af8d61e-e164-4d08-b52d-eae829ea0ab2 (工作空间2)
```

**权限解读**：
- ✅ admin 在 13 个项目中拥有"项目管理员"权限
- ✅ admin 在 2 个工作空间中拥有"工作空间管理员"权限
- ✅ admin 在系统级别拥有"系统管理员"和"超级管理员"权限
- ❌ admin 不能访问其他未分配的项目

---

### 案例2：一个用户被分配多个项目

**场景**：用户 `user123` 被分配为 3 个项目的开发人员

```sql
-- 记录1
user_id: user123
group_id: developer (开发人员组)
source_id: project_a (项目A)

-- 记录2
user_id: user123
group_id: developer (开发人员组)
source_id: project_b (项目B)

-- 记录3
user_id: user123
group_id: developer (开发人员组)
source_id: project_c (项目C)
```

**结果**：
- ✅ 生成 **3 条记录**
- ✅ 用户可以访问项目A、B、C
- ❌ 用户不能访问项目D、E、F

---

### 案例3：一个用户在同一个项目有多个角色

**场景**：用户 `user456` 在项目A中既是开发人员，又是测试人员

```sql
-- 记录1
user_id: user456
group_id: developer (开发人员组)
source_id: project_a (项目A)

-- 记录2
user_id: user456
group_id: tester (测试人员组)
source_id: project_a (项目A)
```

**结果**：
- ✅ 生成 **2 条记录**
- ✅ 用户在项目A拥有两个用户组的权限
- ✅ 权限是**叠加**的（开发人员权限 + 测试人员权限）

---

## 🔍 group.scope_id 的真正作用

### ❌ 错误理解

> scope_id 决定用户的权限范围

### ✅ 正确理解

> scope_id 决定用户组在哪里可以被**选择和分配**

### 详细说明

| scope_id | 作用 | 不影响 |
|----------|------|--------|
| `system` | 只有超级管理员能看到和分配 | ❌ 不影响用户的实际权限 |
| `global` | 所有工作空间都能看到和分配 | ❌ 不影响用户的实际权限 |
| `<工作空间ID>` | 只在该工作空间能看到和分配 | ❌ 不影响用户的实际权限 |

### 示例

**用户组**：
```sql
group.id = 'dev_group'
group.name = '开发人员组'
group.scope_id = 'workspace_a'  -- 工作空间A
```

**权限分配**：
```sql
-- 在工作空间A中分配
user_group.user_id = 'user123'
user_group.group_id = 'dev_group'
user_group.source_id = 'project_1'  -- 项目1（属于工作空间A）

-- 在工作空间B中分配（假设修改了 scope_id 为 global）
user_group.user_id = 'user456'
user_group.group_id = 'dev_group'
user_group.source_id = 'project_2'  -- 项目2（属于工作空间B）
```

**结果**：
- user123 只能访问项目1
- user456 只能访问项目2
- 两个用户的权限范围由 `source_id` 决定，与 `scope_id` 无关

---

## 📊 权限层级

### 三层权限体系

```
┌─────────────────────────────────────────┐
│  系统级别 (SYSTEM)                       │
│  - 超级管理员、系统管理员                │
│  - source_id = 'system'                 │
│  - 可以管理所有工作空间和项目            │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  工作空间级别 (WORKSPACE)                │
│  - 工作空间管理员、工作空间成员          │
│  - source_id = <工作空间ID>             │
│  - 可以管理该工作空间下的所有项目        │
└─────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────┐
│  项目级别 (PROJECT)                      │
│  - 项目管理员、开发人员、测试人员        │
│  - source_id = <项目ID>                 │
│  - 只能访问该项目                        │
└─────────────────────────────────────────┘
```

---

## 🎯 关键结论

### 1. 多项目分配会生成多条记录

**问题**：用户被分配给多个项目，会不会生成多条记录？

**答案**：✅ **会**，每个项目生成一条记录

**示例**：
```sql
-- 用户在 3 个项目中都是开发人员 = 3 条记录
user_id: user123, group_id: developer, source_id: project_a
user_id: user123, group_id: developer, source_id: project_b
user_id: user123, group_id: developer, source_id: project_c
```

---

### 2. 权限由 source_id 决定，不是 scope_id

**关键点**：
- ✅ `user_group.source_id` 决定用户能访问哪些资源
- ❌ `group.scope_id` 不决定用户权限，只决定用户组的可见性

**验证**：
```sql
-- 修改 scope_id 不会改变用户权限
UPDATE `group` SET scope_id = 'global' WHERE id = 'dev_group';

-- 用户的权限仍然由 user_group.source_id 决定
SELECT * FROM user_group WHERE group_id = 'dev_group';
-- 结果：用户仍然只能访问 source_id 指定的项目
```

---

### 3. 用户可以有多个用户组

**场景**：
- 用户在项目A是开发人员
- 用户在项目B是测试人员
- 用户在工作空间C是工作空间管理员
- 用户在系统级别是系统管理员

**结果**：
- ✅ 生成 4 条记录
- ✅ 权限是叠加的
- ✅ 在不同资源上有不同的角色

---

### 4. 权限是细粒度的

**特点**：
- ✅ 用户在每个项目的权限是独立的
- ✅ 用户在项目A的权限不影响项目B
- ✅ 用户必须被显式分配到每个项目

**示例**：
```sql
-- 用户在项目A是管理员
user_group: user_id=user123, group_id=project_admin, source_id=project_a

-- 用户在项目B没有权限（没有记录）
-- 结果：用户不能访问项目B
```

---

## 📝 总结

### 权限模型公式

```
用户权限 = Σ (用户组权限 × 资源范围)

其中：
- 用户组权限：由 user_group_permission 定义
- 资源范围：由 user_group.source_id 定义
- group.scope_id：只影响用户组的可见性，不影响权限
```

### 核心表关系

```
user (用户)
  ↓ 1:N
user_group (用户-用户组-资源关联)
  ↓ N:1
group (用户组)
  ↓ 1:N
user_group_permission (用户组权限)
```

### 权限判断流程

```
1. 用户访问资源
2. 查询 user_group 表（user_id + source_id）
3. 获取用户在该资源的所有用户组
4. 查询 user_group_permission 表
5. 获取所有权限
6. 判断是否有权限执行操作
```

### 关键点

| 问题 | 答案 |
|------|------|
| 多项目分配会生成多条记录吗？ | ✅ 会，每个项目一条 |
| 权限由什么决定？ | ✅ user_group.source_id |
| scope_id 决定权限吗？ | ❌ 不决定，只决定可见性 |
| 用户可以有多个用户组吗？ | ✅ 可以，权限叠加 |
| 权限是细粒度的吗？ | ✅ 是，每个资源独立 |

---

**文档版本**：v1.0  
**更新日期**：2026-01-26  
**适用版本**：MeterSphere 2.10
