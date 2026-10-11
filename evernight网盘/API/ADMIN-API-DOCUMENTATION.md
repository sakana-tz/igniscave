# Evernight 管理员模块 API 文档

> **版本**：v1.2 | **更新时间**：2026-09-24

## 概述

Evernight 管理员模块提供系统仪表盘、用户管理、文件管理、论坛管理四大功能板块。所有接口均需要管理员权限（`userStatus = 0`），普通用户调用将返回 `603` 错误。

### 核心特性

- **系统仪表盘**：用户/帖子/评论总数统计、存储总量概览
- **用户管理**：分页查询、新增、查询/修改/删除用户
- **文件管理**：存储总量统计、文件总数统计（为保护用户隐私，不提供具体文件查询）
- **论坛管理**：帖子与评论的分页查询、详情查看、状态更新、删除

### 架构概览

```
前端
 ├─ 仪表盘 → AdminDashboardController → UserService / FileService / ForumPostService / ForumCommentService → MySQL
 ├─ 用户管理 → AdminUserController → UserService → MySQL
 ├─ 文件管理 → AdminFileController → FileService → MySQL
 └─ 论坛管理 → AdminForumController → ForumService → MySQL + OSS
```

---

## 一、系统仪表盘

### 1.1 获取系统概览统计

**接口**：`POST /admin/dashboard/getOverview`

**说明**：获取系统各模块的核心统计数据，用于仪表盘展示。

**请求参数**：无

**请求头**：需要携带 JWT Token（管理员）

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "userCount": 128,
    "totalStorageSize": 10737418240,
    "postCount": 256,
    "commentCount": 1893
  }
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `userCount` | Integer | 用户总数 |
| `totalStorageSize` | Long | 存储总大小（字节） |
| `postCount` | Integer | 帖子总数 |
| `commentCount` | Integer | 评论总数 |

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

**前端示例**：

```javascript
const { data } = await api.post('/admin/dashboard/getOverview');
// data.userCount → 用户总数
// data.totalStorageSize → 存储总量（字节），可转换为 GB 展示
```

---

## 二、用户管理

### 2.1 分页查询用户列表

**接口**：`POST /admin/user/loadDataList`

**说明**：根据条件分页模糊查询用户列表。

**请求参数**（`UserQuery`）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNo` | Integer | 否 | 页码（默认 1） |
| `pageSize` | Integer | 否 | 每页条数（默认 10） |
| `userId` | Integer | 否 | 用户 ID（精确匹配） |
| `username` | String | 否 | 用户名（精确匹配） |
| `usernameFuzzy` | String | 否 | 用户名（模糊匹配） |
| `email` | String | 否 | 邮箱（精确匹配） |
| `emailFuzzy` | String | 否 | 邮箱（模糊匹配） |
| `userStatus` | Integer | 否 | 用户状态：0=管理员，1=禁用，2=普通用户 |
| `isDeleted` | Integer | 否 | 是否删除：0=正常，1=已删除 |

**请求头**：需要携带 JWT Token（管理员）

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "list": [
      {
        "userId": 1,
        "username": "admin",
        "email": "admin@example.com",
        "createdTime": "2026-09-01 10:00:00",
        "updatedTime": "2026-09-15 14:30:00",
        "userStatus": 0,
        "isDeleted": 0
      }
    ],
    "totalCount": 128,
    "pageNo": 1,
    "pageSize": 10,
    "pageTotal": 13
  }
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

### 2.2 新增用户

**接口**：`POST /admin/user/add`

**说明**：管理员手动新增用户。

**请求参数**（JSON Body，`User` 对象）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | String | 是 | 用户名 |
| `password` | String | 是 | 密码（明文传输，后端自动 SHA256 加密） |
| `email` | String | 是 | 邮箱 |
| `userStatus` | Integer | 否 | 用户状态：0=管理员，2=普通用户（默认 2） |

**请求示例**：

```javascript
await api.post('/admin/user/add', {
  username: '新用户',
  password: 'password123',
  email: 'newuser@example.com',
  userStatus: 2
});
```

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

### 2.3 根据 UserId 查询用户

**接口**：`POST /admin/user/getUserByUserId`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | Integer | 是 | 用户 ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "userId": 2,
    "username": "张三",
    "password": "sha256hashed...",
    "email": "zhangsan@example.com",
    "createdTime": "2026-09-10 08:00:00",
    "updatedTime": null,
    "userStatus": 2,
    "isDeleted": 0
  }
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

### 2.4 根据 UserId 修改用户

**接口**：`POST /admin/user/updateUserByUserId`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | Integer | 是 | 目标用户 ID（Query 参数） |
| Body（JSON） | Object | 是 | 需修改的字段（`User` 对象，仅传需修改的字段） |

**Body 字段说明**（可选传）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `username` | String | 新用户名 |
| `password` | String | 新密码（明文传输，后端自动加密） |
| `email` | String | 新邮箱 |
| `userStatus` | Integer | 用户状态：0=管理员，1=禁用，2=普通用户 |
| `isDeleted` | Integer | 是否删除：0=正常，1=已删除 |

**请求示例**：

```javascript
await api.post('/admin/user/updateUserByUserId?userId=2', {
  username: '新用户名',
  userStatus: 1  // 禁用该用户
});
```

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

### 2.5 根据 UserId 删除用户

**接口**：`POST /admin/user/deleteUserByUserId`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | Integer | 是 | 目标用户 ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

## 三、文件管理

### 3.1 查询存储总文件大小

**接口**：`POST /admin/file/totalFileSize`

**说明**：查询系统中所有文件的存储总大小。

**请求参数**：无

**请求头**：需要携带 JWT Token（管理员）

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": 10737418240
}
```

**响应说明**：
- `data`：存储总大小，单位字节（Long 类型）

> **隐私说明**：为保护用户隐私，管理员仅可查看存储总量和文件总数，无法查看、查询或删除用户具体文件。

---

### 3.2 查询文件总数

**接口**：`POST /admin/file/fileTotalCount`

**说明**：查询系统中所有文件的数量（仅统计未删除的文件）。

**请求参数**：无

**请求头**：需要携带 JWT Token（管理员）

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": 1024
}
```

**响应说明**：
- `data`：文件总数（Long 类型）

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

## 四、论坛管理

### 4.1 分页查询帖子列表

**接口**：`POST /admin/forum/post/loadDataList`

**说明**：分页查询帖子列表（含发帖用户名和评论数），不限帖子状态。

**请求参数**（`ForumPostQuery`）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNo` | Integer | 否 | 页码（默认 1） |
| `pageSize` | Integer | 否 | 每页条数（默认 10） |
| `postId` | Integer | 否 | 帖子 ID（精确匹配） |
| `userId` | Integer | 否 | 发帖用户 ID（精确匹配） |
| `title` | String | 否 | 标题（精确匹配） |
| `titleFuzzy` | String | 否 | 标题（模糊匹配） |
| `postStatus` | Integer | 否 | 帖子状态：0=草稿，1=发布 |

**请求头**：需要携带 JWT Token（管理员）

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "list": [
      {
        "postId": 1,
        "userId": 2,
        "username": "张三",
        "title": "我的第一篇帖子",
        "viewCount": 156,
        "commentCount": 12,
        "postStatus": 1,
        "updatedTime": "2026-09-20 15:00:00"
      }
    ],
    "total": 256,
    "pageNo": 1,
    "pageSize": 10
  }
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

### 4.2 根据 PostId 查询帖子详情

**接口**：`POST /admin/forum/post/getPostByPostId`

**说明**：获取帖子详情，包含 Markdown 内容（从 OSS 读取）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "postId": 1,
    "userId": 2,
    "username": "张三",
    "title": "我的第一篇帖子",
    "viewCount": 156,
    "commentCount": 12,
    "postStatus": 1,
    "updatedTime": "2026-09-20 15:00:00",
    "content": "# 标题\n\n这是帖子的 Markdown 内容..."
  }
}
```

**响应字段说明**（`PostDetailDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 发帖用户 ID |
| `username` | String | 发帖用户名 |
| `title` | String | 帖子标题 |
| `viewCount` | Long | 浏览量 |
| `commentCount` | Integer | 评论数 |
| `postStatus` | Integer | 帖子状态：0=草稿，1=发布 |
| `updatedTime` | String | 最后更新时间 |
| `content` | String | 帖子 Markdown 内容 |

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 602 | 帖子不存在 |
| 603 | 权限不足（非管理员） |

---

### 4.3 根据 PostId 更新帖子状态

**接口**：`POST /admin/forum/post/updatePostStatus`

**说明**：管理员更新帖子状态（发布或撤回为草稿）。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |
| `postStatus` | Integer | 是 | 目标状态：0=草稿，1=发布 |

**请求示例**：

```javascript
await api.post('/admin/forum/post/updatePostStatus', {
  postId: 1,
  postStatus: 1  // 发布帖子
});
```

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 602 | 帖子不存在 |
| 603 | 权限不足（非管理员） |

---

### 4.4 根据 PostId 删除帖子

**接口**：`POST /admin/forum/post/deletePostByPostId`

**说明**：管理员删除帖子，同时删除 OSS 中的 Markdown 内容，评论通过外键 CASCADE 自动删除。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 602 | 帖子不存在 |
| 603 | 权限不足（非管理员） |

---

### 4.5 分页查询评论列表

**接口**：`POST /admin/forum/comment/loadDataList`

**说明**：分页查询评论列表（含评论用户名）。

**请求参数**（`ForumCommentQuery`）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pageNo` | Integer | 否 | 页码（默认 1） |
| `pageSize` | Integer | 否 | 每页条数（默认 10） |
| `commentId` | Integer | 否 | 评论 ID（精确匹配） |
| `postId` | Integer | 否 | 所属帖子 ID（精确匹配） |
| `userId` | Integer | 否 | 评论用户 ID（精确匹配） |
| `contentFuzzy` | String | 否 | 评论内容（模糊匹配） |

**请求头**：需要携带 JWT Token（管理员）

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "list": [
      {
        "commentId": 1,
        "postId": 1,
        "userId": 3,
        "username": "李四",
        "content": "写得很好！",
        "createdTime": "2026-09-20 16:00:00"
      }
    ],
    "total": 1893,
    "pageNo": 1,
    "pageSize": 10
  }
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

### 4.6 根据 CommentId 删除评论

**接口**：`POST /admin/forum/comment/deleteCommentByCommentId`

**说明**：管理员删除评论，不限用户归属。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `commentId` | Integer | 是 | 评论 ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 602 | 评论不存在 |
| 603 | 权限不足（非管理员） |

---

## 五、统一响应格式

所有接口统一使用 `ResponseVO` 响应格式：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": { ... }
}
```

### 管理员模块相关状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 602 | 数据不存在 |
| 603 | 权限不足（非管理员） |

---

## 六、认证方式

### JWT Token 认证

所有管理员接口通过 `@UserInfoValidation` 注解自动从请求头解析用户信息，并校验 `userStatus == 0`。

**Token 传递方式**（优先级从高到低）：

1. **Cookie**（推荐）：
```
Cookie: userTokenJwt=eyJhbGciOiJIUzI1NiJ9...
```

2. **Authorization Header**：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

3. **自定义 Header**：
```
userTokenJwt: eyJhbGciOiJIUzI1NiJ9...
```

**Token 有效期**：1 天

---

## 七、数据模型

### User — 用户表实体

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | Integer | 用户 ID（主键自增） |
| `username` | String | 用户名 |
| `password` | String | 加密密码（SHA256） |
| `email` | String | 邮箱 |
| `createdTime` | LocalDateTime | 注册时间 |
| `updatedTime` | LocalDateTime | 最后更新时间 |
| `userStatus` | Integer | 用户状态：0=管理员，1=禁用，2=普通用户 |
| `isDeleted` | Integer | 是否删除：0=正常，1=已删除 |

### File — 文件表实体

| 字段 | 类型 | 说明 |
|------|------|------|
| `fileId` | Long | 文件 ID（主键） |
| `userId` | Integer | 所属用户 ID |
| `fileName` | String | 文件名 |
| `filePath` | String | 存储路径 |
| `fileType` | String | 文件类型 |
| `fileSize` | Long | 文件大小（字节） |
| `fileHash` | String | 文件哈希（用于秒传） |
| `isDeleted` | Integer | 是否删除：0=正常，1=已删除 |
| `createdTime` | LocalDateTime | 创建时间 |
| `updatedTime` | LocalDateTime | 更新时间 |

### ForumPost — 论坛帖子表实体

| 字段 | 类型 | 说明 |
|------|------|------|
| `postId` | Integer | 帖子 ID（主键自增） |
| `userId` | Integer | 发帖用户 ID |
| `title` | String | 标题 |
| `viewCount` | Long | 浏览数 |
| `postStatus` | Integer | 帖子状态：0=草稿，1=发布 |
| `updatedTime` | LocalDateTime | 最后更新时间 |
| `commentCount` | Integer | 评论数（非数据库字段，联表查询） |
| `username` | String | 发帖用户名（非数据库字段，联表查询） |

### ForumComment — 论坛评论表实体

| 字段 | 类型 | 说明 |
|------|------|------|
| `commentId` | Integer | 评论 ID（主键自增） |
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 评论用户 ID |
| `content` | String | 评论内容（限 500 字） |
| `createdTime` | LocalDateTime | 创建时间 |
| `username` | String | 评论用户名（非数据库字段，联表查询） |

---

## 八、API 接口汇总

### 系统仪表盘

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/admin/dashboard/getOverview` | POST | 获取系统概览统计 | 是（管理员） |

### 用户管理

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/admin/user/loadDataList` | POST | 分页查询用户列表 | 是（管理员） |
| `/admin/user/add` | POST | 新增用户 | 是（管理员） |
| `/admin/user/getUserByUserId` | POST | 根据 ID 查询用户 | 是（管理员） |
| `/admin/user/updateUserByUserId` | POST | 根据 ID 修改用户 | 是（管理员） |
| `/admin/user/deleteUserByUserId` | POST | 根据 ID 删除用户 | 是（管理员） |

### 文件管理

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/admin/file/totalFileSize` | POST | 查询存储总大小 | 是（管理员） |
| `/admin/file/fileTotalCount` | POST | 查询文件总数 | 是（管理员） |

> **隐私说明**：为保护用户隐私，文件管理仅提供存储总量和文件总数查询，不提供具体文件的查询/删除功能。

### 论坛管理 — 帖子

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/admin/forum/post/loadDataList` | POST | 分页查询帖子列表 | 是（管理员） |
| `/admin/forum/post/getPostByPostId` | POST | 根据 ID 查询帖子详情 | 是（管理员） |
| `/admin/forum/post/updatePostStatus` | POST | 根据 ID 更新帖子状态 | 是（管理员） |
| `/admin/forum/post/deletePostByPostId` | POST | 根据 ID 删除帖子 | 是（管理员） |

### 论坛管理 — 评论

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/admin/forum/comment/loadDataList` | POST | 分页查询评论列表 | 是（管理员） |
| `/admin/forum/comment/deleteCommentByCommentId` | POST | 根据 ID 删除评论 | 是（管理员） |
