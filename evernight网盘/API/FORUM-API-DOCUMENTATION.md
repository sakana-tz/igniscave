# Evernight 帖子模块 API 文档

> **版本**：v2.2 | **更新时间**：2026-09-24

## 概述

Evernight 帖子模块提供论坛帖子发布、搜索、浏览以及评论功能。帖子正文采用 Markdown 格式，存储于阿里云 OSS 中，前端可直接获取 MD 原文进行渲染和编辑，实现无感的 Markdown 显示与编辑体验。

### 核心特性

- **帖子管理**：发帖（默认草稿）、发布/撤回、编辑、删除，MD 内容存储于 OSS
- **我的帖子**：分页查看当前用户的所有帖子（草稿和已发布），支持标题模糊搜索
- **帖子封面**：以帖子 ID 为键上传/获取/删除封面图片，前端直传 OSS
- **帖子图片**：上传帖子内图片，返回永久可访问的代理 URL，可嵌入 Markdown
- **帖子列表/搜索**：统一接口，keyword 为空查全部，有值模糊搜，分页返回，含评论数和发帖用户名
- **帖子详情**：含 MD 正文内容 + 评论数 + 发帖用户名，前端可直接渲染/编辑
- **评论系统**：发表评论、评论列表（按时间升序，含评论用户名）、删除评论
- **热门帖子**：浏览量 Top 10，Redis 缓存 1 天，浏览量变化时自动清除缓存
- **计数统计**：浏览量计数、评论数统计

### 架构概览

```
前端
 ├─ 热门帖子 → ForumController → ForumService → Redis 缓存 → MySQL（浏览量Top10 + 用户名）
 ├─ 我的帖子 → ForumController → ForumService → MySQL（按userId+状态筛选帖子+评论数+用户名）
 ├─ 帖子列表/搜索 → ForumController → ForumService → MySQL（帖子+评论数+用户名）
 ├─ 帖子详情 → ForumController → ForumService → MySQL（帖子+评论数+用户名）+ OSS（MD内容）
 ├─ 发帖/编辑 → ForumController → ForumService → MySQL（帖子元数据）+ OSS（MD内容存储）
 ├─ 删除帖子 → ForumController → ForumService → MySQL + OSS（级联清理MD/封面/图片）
 ├─ 封面管理 → ForumController → ForumService → OSS（前端直传，帖子ID为键）
 ├─ 图片上传 → ForumController → ForumService → OSS（前端直传，返回代理URL）
 ├─ 图片代理 → ForumController → ForumService → OSS（302重定向到预签名URL）
 └─ 评论管理 → ForumController → ForumService → MySQL（评论+用户名）
```

### 存储设计

| 存储项 | 存储位置 | 说明 |
|--------|----------|------|
| 帖子元数据（标题、状态、浏览数等） | MySQL `forum_post` 表 | 关系型数据 |
| 帖子 MD 正文内容 | 阿里云 OSS | 键格式：`evernight/forum/post/{postId}.md` |
| 帖子封面图片 | 阿里云 OSS | 键格式：`evernight/forum/postCover/{postId}.png` |
| 帖子内图片 | 阿里云 OSS | 键格式：`evernight/forum/postImage/{postId}/{uuid}.{ext}` |
| 评论数据 | MySQL `forum_comment` 表 | 关系型数据，外键级联删除 |
| 热门帖子列表 | Redis | Key: `evernight:file:forumPost:`，过期时间 1 天 |

### 核心配置参数

| 配置项 | 值 | 说明 |
|--------|------|------|
| `forum-post` | `evernight/forum/post/` | 帖子 MD 内容 OSS 存储路径前缀 |
| `forum-post-cover` | `evernight/forum/postCover/` | 帖子封面 OSS 存储路径前缀 |
| `forum-post-image` | `evernight/forum/postImage/` | 帖子图片 OSS 存储路径前缀 |
| MD 存储键格式 | `evernight/forum/post/{postId}.md` | 以帖子主键 ID 作为存储键的一部分 |
| 封面存储键格式 | `evernight/forum/postCover/{postId}.png` | 以帖子 ID 为键，固定 PNG 格式 |
| 图片存储键格式 | `evernight/forum/postImage/{postId}/{uuid}.{ext}` | 以帖子 ID 为目录，UUID 防冲突 |
| MD Content-Type | `text/markdown; charset=utf-8` | OSS 对象的内容类型 |

### 响应格式说明

所有接口统一使用 `ResponseVO` 响应格式。由于 Redis JSON 序列化启用了 `DefaultTyping`，响应中会携带 `@class` 类型字段，前端解析时忽略 `@class` 字段即可。

---

## 一、帖子管理

### 1.1 创建帖子

**接口**：`POST /forum/createPost`

**说明**：创建新帖子（默认草稿状态），同时在 OSS 中初始化 MD 内容。返回新建帖子 ID。创建后需调用 `publishPost` 发布。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `title` | String | 是 | 帖子标题（限 128 字符） |
| `content` | String | 否 | 帖子 MD 内容（默认为空字符串，后续可通过 `updatePost` 保存） |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": 1
}
```

**响应说明**：
- `data`：新建帖子的 `postId`（Integer），状态为草稿（`postStatus=0`）

**后端处理逻辑**：

1. 在 MySQL 中插入帖子记录（状态默认 `0`=草稿）
2. 使用自增主键 `postId` 构建 OSS 存储键：`evernight/forum/post/{postId}.md`
3. 将 MD 内容上传到 OSS
4. 返回新建帖子 ID

**前端示例**：

```javascript
async function createPost(title, mdContent) {
  const { data: postId } = await api.post('/forum/createPost', null, {
    params: { title, content: mdContent }
  });
  return postId; // 草稿状态，需后续调用 publishPost 发布
}
```

---

### 1.2 发布帖子

**接口**：`POST /forum/publishPost`

**说明**：将草稿帖子发布（状态从 `0` 改为 `1`）。仅帖子作者可操作。已发布的帖子重复调用无副作用。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
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
| 603 | 权限不足（非帖子作者） |

**前端示例**：

```javascript
async function publishPost(postId) {
  await api.post('/forum/publishPost', null, {
    params: { postId }
  });
}
```

---

### 1.3 更新帖子状态

**接口**：`POST /forum/updatePostStatus`

**说明**：更新帖子状态，支持草稿与发布之间双向切换。仅帖子作者可操作。状态变化时会清除热门帖子缓存。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |
| `postStatus` | Integer | 是 | 目标状态：0=草稿，1=发布 |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
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
| 603 | 权限不足（非帖子作者） |

**前端示例**：

```javascript
// 发布帖子
await api.post('/forum/updatePostStatus', null, {
  params: { postId: 1, postStatus: 1 }
});

// 撤回为草稿
await api.post('/forum/updatePostStatus', null, {
  params: { postId: 1, postStatus: 0 }
});
```

---

### 1.4 获取帖子列表（含搜索）

**接口**：`POST /forum/getPostList`

**说明**：分页获取已发布的帖子列表（仅 `postStatus=1`），按更新时间倒序排列，每条帖子含评论数和发帖用户名。`keyword` 为空时查全部帖子，有值时按标题模糊搜索。通过联表查询 `forum_post` 和 `user` 表获取发帖用户名。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 搜索关键词（模糊匹配标题），不传或为空则查全部 |
| `pageNo` | Integer | 否 | 页码（默认 1） |
| `pageSize` | Integer | 否 | 每页数量（默认 20） |

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": {
        "@class": "java.util.HashMap",
        "total": 1,
        "pageNo": 1,
        "pageSize": 20,
        "list": [
            "java.util.ArrayList",
            [
                {
                    "@class": "com.evernight.base.entity.po.ForumPost",
                    "postId": 1,
                    "userId": 1,
                    "username": "张三",
                    "title": "test1",
                    "viewCount": 4,
                    "postStatus": 1,
                    "updatedTime": "2026-09-20 13:29:28",
                    "commentCount": 1
                }
            ]
        ]
    }
}
```

**data 字段说明**（`HashMap`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `list` | List\<ForumPost\> | 帖子列表（Jackson 序列化为 `["java.util.ArrayList", [...]]` 格式） |
| `total` | Integer | 符合条件的帖子总数 |
| `pageNo` | Integer | 当前页码 |
| `pageSize` | Integer | 每页数量 |

**list 中每项字段说明**（`ForumPost`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 发帖用户 ID |
| `username` | String | 发帖用户名（联表查询自 `user` 表） |
| `title` | String | 帖子标题 |
| `viewCount` | Long | 浏览数 |
| `postStatus` | Integer | 帖子状态：0=草稿，1=已发布 |
| `updatedTime` | String | 最后更新时间（格式：yyyy-MM-dd HH:mm:ss） |
| `commentCount` | Integer | 评论数（SQL 子查询统计） |

**前端示例**：

```javascript
// 查全部帖子
const { data } = await api.post('/forum/getPostList', null, {
  params: { pageNo: 1, pageSize: 20 }
});

// 搜索帖子
const { data } = await api.post('/forum/getPostList', null, {
  params: { keyword: 'Spring', pageNo: 1 }
});

// 前端解析时忽略 @class 字段，直接访问数据
data.list.forEach(post => {
  console.log(post.title, post.commentCount);
});
```

---

### 1.5 获取我的帖子列表

**接口**：`POST /forum/getMyPostList`

**说明**：分页获取当前用户的所有帖子（含草稿和已发布），按更新时间倒序排列，每条帖子含评论数和发帖用户名。`keyword` 为空时不模糊搜索，有值时按标题模糊搜索。仅返回当前登录用户的帖子。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 否 | 搜索关键词（模糊匹配标题），不传或为空则不搜索 |
| `pageNo` | Integer | 否 | 页码（默认 1） |
| `pageSize` | Integer | 否 | 每页数量（默认 20） |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": {
        "@class": "java.util.HashMap",
        "total": 2,
        "pageNo": 1,
        "pageSize": 20,
        "list": [
            "java.util.ArrayList",
            [
                {
                    "@class": "com.evernight.base.entity.po.ForumPost",
                    "postId": 2,
                    "userId": 1,
                    "username": "张三",
                    "title": "草稿帖子",
                    "viewCount": 0,
                    "postStatus": 0,
                    "updatedTime": "2026-09-22 10:00:00",
                    "commentCount": 0
                },
                {
                    "@class": "com.evernight.base.entity.po.ForumPost",
                    "postId": 1,
                    "userId": 1,
                    "username": "张三",
                    "title": "已发布帖子",
                    "viewCount": 4,
                    "postStatus": 1,
                    "updatedTime": "2026-09-20 13:29:28",
                    "commentCount": 1
                }
            ]
        ]
    }
}
```

**data 字段说明**（`HashMap`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `list` | List\<ForumPost\> | 帖子列表（Jackson 序列化为 `["java.util.ArrayList", [...]]` 格式） |
| `total` | Integer | 符合条件的帖子总数 |
| `pageNo` | Integer | 当前页码 |
| `pageSize` | Integer | 每页数量 |

**list 中每项字段说明**（`ForumPost`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 发帖用户 ID |
| `username` | String | 发帖用户名（联表查询自 `user` 表） |
| `title` | String | 帖子标题 |
| `viewCount` | Long | 浏览数 |
| `postStatus` | Integer | 帖子状态：0=草稿，1=已发布 |
| `updatedTime` | String | 最后更新时间（格式：yyyy-MM-dd HH:mm:ss） |
| `commentCount` | Integer | 评论数（SQL 子查询统计） |

**前端示例**：

```javascript
// 查看我的所有帖子（含草稿和已发布）
const { data } = await api.post('/forum/getMyPostList', null, {
  params: { pageNo: 1, pageSize: 20 }
});

// 搜索我的帖子
const { data } = await api.post('/forum/getMyPostList', null, {
  params: { keyword: 'Spring' }
});

// 前端可根据 postStatus 区分显示草稿/已发布标识
data.list.forEach(post => {
  const statusLabel = post.postStatus === 0 ? '草稿' : '已发布';
  console.log(`[${statusLabel}] ${post.title}`);
});
```

---

### 1.6 获取帖子详情

**接口**：`POST /forum/getPostDetail`

**说明**：获取帖子完整详情，包含 MD 正文内容、评论数和发帖用户名。前端可直接使用 `content` 字段进行 Markdown 渲染或编辑，实现无感 MD 显示与编辑。通过联表查询 `forum_post` 和 `user` 表获取发帖用户名。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": {
        "@class": "com.evernight.entity.dto.PostDetailDto",
        "postId": 1,
        "userId": 1,
        "username": "张三",
        "title": "test1",
        "viewCount": 4,
        "commentCount": 1,
        "postStatus": 1,
        "updatedTime": "2026-09-20 13:29:28",
        "content": "## **喂喂喂，123，321**\n"
    }
}
```

**data 字段说明**（`PostDetailDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 发帖用户 ID |
| `username` | String | 发帖用户名（联表查询自 `user` 表） |
| `title` | String | 帖子标题 |
| `viewCount` | Long | 浏览数 |
| `commentCount` | Integer | 评论数（SQL 子查询实时统计） |
| `postStatus` | Integer | 帖子状态：0=草稿，1=已发布 |
| `updatedTime` | String | 最后更新时间（格式：yyyy-MM-dd HH:mm:ss） |
| `content` | String | 帖子 MD 正文内容（从 OSS 获取，可直接渲染/编辑） |

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 602 | 帖子不存在 |

**前端示例**：

```javascript
async function viewPost(postId) {
  const { data } = await api.post('/forum/getPostDetail', null, {
    params: { postId }
  });

  // data.content 就是 MD 原文，可直接传给 Markdown 渲染器
  renderMarkdown(data.content);

  // 如果需要编辑，直接将 content 填入编辑器
  editor.setValue(data.content);
}
```

---

### 1.7 更新帖子

**接口**：`POST /forum/updatePost`

**说明**：更新帖子标题和 MD 内容。仅帖子作者可操作。MD 内容会覆盖写入 OSS。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |
| `title` | String | 是 | 新标题 |
| `content` | String | 否 | 新 MD 内容（不传则不更新内容） |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
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
| 603 | 权限不足（非帖子作者） |

**前端示例**：

```javascript
async function savePost(postId, title, mdContent) {
  await api.post('/forum/updatePost', null, {
    params: { postId, title, content: mdContent }
  });
}
```

---

### 1.8 删除帖子

**接口**：`POST /forum/deletePost`

**说明**：删除帖子，同时清理 OSS 中的 MD 文件、封面图片和帖子内图片。评论通过数据库外键 CASCADE 自动删除。仅帖子作者可操作。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
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
| 603 | 权限不足（非帖子作者） |

**后端处理逻辑**：

1. 校验帖子存在且为当前用户所有
2. 删除 MySQL 帖子记录（评论通过外键 CASCADE 自动删除）
3. 检查并删除 OSS 中的 MD 文件
4. 删除 OSS 中的封面图片（`evernight/forum/postCover/{postId}.png`）
5. 批量删除 OSS 中的帖子图片（`evernight/forum/postImage/{postId}/` 目录下所有文件）

---

### 1.9 增加浏览量

**接口**：`POST /forum/incrementViewCount`

**说明**：帖子浏览量 +1。前端进入帖子详情页时调用。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
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

---

### 1.10 获取热门帖子列表

**接口**：`POST /forum/getHotPostList`

**说明**：获取浏览量前 10 的热门帖子列表（仅已发布帖子），按浏览量倒序排列，含发帖用户名和评论数。通过联表查询 `forum_post` 和 `user` 表获取发帖用户名。结果缓存于 Redis，过期时间 1 天；当帖子浏览量发生变化时自动清除缓存。

**请求参数**：无

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": [
        "java.util.ArrayList",
        [
            {
                "@class": "com.evernight.entity.dto.HotPostDto",
                "postId": 3,
                "userId": 1,
                "username": "张三",
                "title": "热门技术分享",
                "viewCount": 1024,
                "commentCount": 56,
                "postStatus": 1,
                "updatedTime": "2026-09-21 10:00:00"
            },
            {
                "@class": "com.evernight.entity.dto.HotPostDto",
                "postId": 1,
                "userId": 2,
                "username": "李四",
                "title": "另一篇热帖",
                "viewCount": 512,
                "commentCount": 23,
                "postStatus": 1,
                "updatedTime": "2026-09-20 18:30:00"
            }
        ]
    ]
}
```

**data 中每项字段说明**（`HotPostDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 发帖用户 ID |
| `username` | String | 发帖用户名（联表查询自 `user` 表） |
| `title` | String | 帖子标题 |
| `viewCount` | Long | 浏览数 |
| `commentCount` | Integer | 评论数（SQL 子查询统计） |
| `postStatus` | Integer | 帖子状态：0=草稿，1=已发布 |
| `updatedTime` | String | 最后更新时间（格式：yyyy-MM-dd HH:mm:ss） |

**缓存策略**：

| 项目 | 说明 |
|------|------|
| Redis Key | `evernight:file:forumPost:` |
| 过期时间 | 1 天 |
| 缓存清除时机 | 调用 `incrementViewCount` 增加浏览量时自动清除 |

**前端示例**：

```javascript
async function getHotPosts() {
  const { data } = await api.post('/forum/getHotPostList');
  // data 是列表，直接遍历
  data.forEach(post => {
    console.log(post.title, post.username, post.viewCount);
  });
}
```

---

## 二、封面与图片管理

### 2.1 获取封面上传 URL

**接口**：`POST /forum/getCoverUploadUrl`

**说明**：获取帖子封面上传预签名 URL，前端直传 OSS。以帖子 ID 为键，封面固定为 PNG 格式。草稿帖子仅本人可操作。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/forum/postCover/1.png?OSSAccessKeyId=..."
}
```

**响应说明**：
- `data`：预签名上传 URL（PUT 方式，1 小时有效）

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 602 | 帖子不存在 |
| 603 | 权限不足（草稿帖子非本人操作） |

**前端示例**：

```javascript
async function uploadCover(postId, coverFile) {
  // 1. 获取上传 URL
  const { data: uploadUrl } = await api.post('/forum/getCoverUploadUrl', null, {
    params: { postId }
  });

  // 2. 前端直传 OSS（PUT 方式）
  await fetch(uploadUrl, {
    method: 'PUT',
    body: coverFile,
    headers: { 'Content-Type': 'image/png' }
  });
}
```

---

### 2.2 获取封面 URL

**接口**：`POST /forum/getCoverUrl`

**说明**：获取帖子封面的访问 URL（预签名 URL，1 小时有效）。无需登录。若封面不存在返回 `null`。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/forum/postCover/1.png?OSSAccessKeyId=..."
}
```

**响应说明**：
- `data`：封面图片 URL（预签名，1 小时有效），若封面不存在则为 `null`

**前端示例**：

```javascript
async function getCoverUrl(postId) {
  const { data } = await api.post('/forum/getCoverUrl', null, {
    params: { postId }
  });
  if (data) {
    document.getElementById('cover-img').src = data;
  }
}
```

---

### 2.3 上传帖子图片

**接口**：`POST /forum/uploadPostImage`

**说明**：上传帖子内图片（用于嵌入 Markdown），返回上传 URL 和永久可访问的代理 URL。前端先获取上传 URL 直传 OSS，然后将代理 URL 嵌入 Markdown 内容。草稿帖子仅本人可操作。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |
| `fileName` | String | 是 | 文件名（含扩展名，如 `image.png`） |
| `contentType` | String | 是 | 文件 MIME 类型（如 `image/png`） |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": {
        "@class": "java.util.HashMap",
        "uploadUrl": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/forum/postImage/1/a1b2c3d4.png?OSSAccessKeyId=...",
        "proxyUrl": "http://localhost:9092/forum/getForumImage/ZXZlcm5pZ2h0L2ZvcnVtL3Bvc3RJbWFnZS8xL2ExYjJjM2Q0LnBuZw"
    }
}
```

**data 字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadUrl` | String | 预签名上传 URL（PUT 方式，1 小时有效），前端直传 OSS |
| `proxyUrl` | String | 永久可访问的图片代理 URL，嵌入 Markdown 使用 |

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 602 | 帖子不存在 |
| 603 | 权限不足（草稿帖子非本人操作） |

**前端示例**：

```javascript
async function uploadPostImage(postId, imageFile) {
  // 1. 获取上传 URL 和代理 URL
  const { data } = await api.post('/forum/uploadPostImage', null, {
    params: { postId, fileName: imageFile.name, contentType: imageFile.type }
  });

  // 2. 前端直传 OSS（PUT 方式）
  await fetch(data.uploadUrl, {
    method: 'PUT',
    body: imageFile,
    headers: { 'Content-Type': imageFile.type }
  });

  // 3. 将代理 URL 嵌入 Markdown
  const markdownImage = `![图片描述](${data.proxyUrl})`;
  editor.insertContent(markdownImage);

  return data.proxyUrl;
}
```

---

### 2.4 帖子图片代理访问

**接口**：`GET /forum/getForumImage/{encryptedKey}`

**说明**：帖子图片代理访问端点。前端在 Markdown 中嵌入的代理 URL 会请求此端点，后端解密 OSS 键后生成预签名 URL 并 302 重定向。无需登录，每次访问动态生成新的预签名 URL（1 小时有效），因此代理 URL 永久有效。

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `encryptedKey` | String | Base64 URL 编码的 OSS 存储键 |

**响应**：302 重定向到 OSS 预签名 URL

**工作原理**：

```
浏览器请求代理 URL
    ↓
后端解密 encryptedKey → 得到 OSS 键
    ↓
生成 1 小时有效的预签名 URL
    ↓
302 重定向 → 浏览器直接从 OSS 加载图片
```

---

## 三、评论管理

### 3.1 添加评论

**接口**：`POST /forum/addComment`

**说明**：为指定帖子发表评论，评论内容限 500 字。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |
| `content` | String | 是 | 评论内容（限 500 字） |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
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

---

### 3.2 获取评论列表

**接口**：`POST /forum/getCommentList`

**说明**：获取指定帖子的评论列表，按发布时间升序排列（最早的评论在最前面）。通过联表查询 `forum_comment` 和 `user` 表获取评论用户名。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `postId` | Integer | 是 | 帖子 ID |

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success",
    "code": 200,
    "info": "请求成功",
    "data": [
        "java.util.ArrayList",
        [
            {
                "@class": "com.evernight.base.entity.po.ForumComment",
                "commentId": 1,
                "postId": 1,
                "userId": 1,
                "username": "张三",
                "content": "写得很好，学到了！",
                "createdTime": "2026-09-20 15:00:00"
            },
            {
                "@class": "com.evernight.base.entity.po.ForumComment",
                "commentId": 2,
                "postId": 1,
                "userId": 3,
                "username": "王五",
                "content": "感谢分享",
                "createdTime": "2026-09-20 15:30:00"
            }
        ]
    ]
}
```

**data 中每项字段说明**（`ForumComment`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `commentId` | Integer | 评论 ID |
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 评论用户 ID |
| `username` | String | 评论用户名（联表查询自 `user` 表） |
| `content` | String | 评论内容 |
| `createdTime` | String | 评论时间（格式：yyyy-MM-dd HH:mm:ss） |

---

### 3.3 删除评论

**接口**：`POST /forum/deleteComment`

**说明**：删除指定评论。仅评论作者可操作。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `commentId` | Integer | 是 | 评论 ID |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
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
| 603 | 权限不足（非评论作者） |

---

## 四、统一响应格式

所有接口统一使用 `ResponseVO` 响应格式：

```json
{
    "@class": "com.evernight.base.entity.vo.ResponseVO",
    "status": "success | error",
    "code": 200,
    "info": "请求成功",
    "data": "..."
}
```

> **关于 `@class` 字段**：由于 Redis 序列化启用了 `DefaultTyping`，响应 JSON 中会包含 `@class` 类型描述字段。前端解析时直接忽略该字段即可，不影响业务数据的使用。列表类型数据会被序列化为 `["java.util.ArrayList", [...]]` 格式，前端取 `[1]` 即可获取实际数组。

### 帖子模块相关状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权或授权失效 |
| 602 | 数据不存在（帖子/评论不存在） |
| 603 | 权限不足（非作者本人操作） |

---

## 五、认证方式

需要认证的接口通过 `@UserInfoValidation` 注解自动从请求头解析用户信息。

**需要认证的接口**：

| 接口 | 说明 |
|------|------|
| `/forum/createPost` | 创建帖子 |
| `/forum/publishPost` | 发布帖子 |
| `/forum/updatePostStatus` | 更新帖子状态 |
| `/forum/getMyPostList` | 我的帖子列表 |
| `/forum/updatePost` | 更新帖子 |
| `/forum/deletePost` | 删除帖子 |
| `/forum/getCoverUploadUrl` | 获取封面上传 URL |
| `/forum/uploadPostImage` | 上传帖子图片 |
| `/forum/addComment` | 添加评论 |
| `/forum/deleteComment` | 删除评论 |

**无需认证的接口**：

| 接口 | 说明 |
|------|------|
| `/forum/getPostList` | 帖子列表/搜索 |
| `/forum/getPostDetail` | 帖子详情 |
| `/forum/getCommentList` | 评论列表 |
| `/forum/getHotPostList` | 热门帖子列表 |
| `/forum/incrementViewCount` | 增加浏览量 |
| `/forum/getCoverUrl` | 获取封面 URL |
| `/forum/getForumImage/{encryptedKey}` | 图片代理访问 |

**Token 传递方式**（优先级从高到低）：

1. **Cookie**：`Cookie: userTokenJwt=xxx`
2. **Authorization Header**：`Authorization: Bearer xxx`
3. **自定义 Header**：`userTokenJwt: xxx`

---

## 六、数据模型

### forum_post — 论坛帖子表

| 字段 | 类型 | 说明 |
|------|------|------|
| `post_id` | INT UNSIGNED | 帖子 ID（主键自增） |
| `user_id` | INT UNSIGNED | 发帖用户 ID（外键 → user） |
| `title` | VARCHAR(128) | 标题 |
| `view_count` | BIGINT UNSIGNED | 浏览数（默认 0） |
| `post_status` | TINYINT UNSIGNED | 帖子状态：0=草稿，1=发布（默认 0） |
| `updated_time` | DATETIME | 最后更新时间（自动更新） |

**索引**：

| 索引名 | 字段 | 说明 |
|--------|------|------|
| PRIMARY | `post_id` | 主键 |
| `idx_user_id` | `user_id` | 发帖用户索引 |
| `idx_status_deleted` | `post_status` | 状态索引 |

### forum_comment — 论坛评论表

| 字段 | 类型 | 说明 |
|------|------|------|
| `comment_id` | INT UNSIGNED | 评论 ID（主键自增） |
| `post_id` | INT UNSIGNED | 帖子 ID（外键 → forum_post） |
| `user_id` | INT UNSIGNED | 评论用户 ID（外键 → user） |
| `content` | VARCHAR(500) | 评论内容（限 500 字） |
| `created_time` | DATETIME | 创建时间 |

**索引**：

| 索引名 | 字段 | 说明 |
|--------|------|------|
| PRIMARY | `comment_id` | 主键 |
| `idx_post_comment` | `post_id, comment_id` | 帖子评论游标分页索引 |
| `idx_user_id` | `user_id` | 评论用户索引 |
| `idx_created_time` | `created_time` | 评论时间索引 |

**外键约束**：

| 约束名 | 字段 | 引用 | 级联规则 |
|--------|------|------|----------|
| `forum_comment_ibfk_1` | `user_id` | `user(user_id)` | ON DELETE CASCADE ON UPDATE CASCADE |
| `forum_comment_ibfk_2` | `post_id` | `forum_post(post_id)` | ON DELETE CASCADE ON UPDATE CASCADE |

### PostDetailDto — 帖子详情响应

| 字段 | 类型 | 说明 |
|------|------|------|
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 发帖用户 ID |
| `username` | String | 发帖用户名（联表查询自 `user` 表） |
| `title` | String | 标题 |
| `viewCount` | Long | 浏览数 |
| `commentCount` | Integer | 评论数（非 DB 字段，SQL 子查询填充） |
| `postStatus` | Integer | 帖子状态 |
| `updatedTime` | String | 最后更新时间 |
| `content` | String | MD 正文内容（从 OSS 获取） |

### HotPostDto — 热门帖子响应

| 字段 | 类型 | 说明 |
|------|------|------|
| `postId` | Integer | 帖子 ID |
| `userId` | Integer | 发帖用户 ID |
| `username` | String | 发帖用户名（联表查询自 `user` 表） |
| `title` | String | 标题 |
| `viewCount` | Long | 浏览数 |
| `commentCount` | Integer | 评论数（SQL 子查询填充） |
| `postStatus` | Integer | 帖子状态 |
| `updatedTime` | String | 最后更新时间 |

---

## 七、前端 Markdown 集成指南

### 无感 MD 显示与编辑

帖子内容以 Markdown 格式存储于 OSS，前端获取到 `content` 字段后即可直接用于 MD 渲染和编辑：

```javascript
// 1. 获取帖子详情（content 就是 MD 原文）
const { data } = await api.post('/forum/getPostDetail', null, {
  params: { postId }
});

// 2. 渲染显示（使用任意 MD 渲染库，如 marked / markdown-it）
const htmlContent = marked.parse(data.content);
document.getElementById('post-content').innerHTML = htmlContent;

// 3. 编辑模式（使用 MD 编辑器，如 md-editor-v3 / bytemd）
editor.setValue(data.content);  // 加载 MD 原文到编辑器

// 4. 保存编辑后的内容
const newContent = editor.getValue();
await api.post('/forum/updatePost', null, {
  params: { postId, title: data.title, content: newContent }
});
```

### 发帖流程

```javascript
// 1. 创建帖子（默认草稿）
const postId = await createPost('我的帖子', '# Hello\n\n初始内容...');

// 2. 上传封面
await uploadCover(postId, coverFile);

// 3. 上传帖子内图片并嵌入 Markdown
const imageUrl = await uploadPostImage(postId, imageFile);

// 4. 发布帖子
await publishPost(postId);
```

---

## 八、API 接口汇总

### 帖子管理

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/forum/createPost` | POST | 创建帖子（默认草稿） | 是 |
| `/forum/publishPost` | POST | 发布帖子 | 是 |
| `/forum/updatePostStatus` | POST | 更新帖子状态（草稿/发布切换） | 是 |
| `/forum/getMyPostList` | POST | 我的帖子列表（含所有状态） | 是 |
| `/forum/getPostList` | POST | 帖子列表/搜索（keyword 可选） | 否 |
| `/forum/getPostDetail` | POST | 帖子详情（含MD内容） | 否 |
| `/forum/updatePost` | POST | 更新帖子 | 是 |
| `/forum/deletePost` | POST | 删除帖子（含封面/图片） | 是 |
| `/forum/getHotPostList` | POST | 热门帖子（浏览量Top10，Redis缓存） | 否 |
| `/forum/incrementViewCount` | POST | 增加浏览量 | 否 |

### 封面与图片

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/forum/getCoverUploadUrl` | POST | 获取封面上传 URL | 是 |
| `/forum/getCoverUrl` | POST | 获取封面 URL | 否 |
| `/forum/uploadPostImage` | POST | 上传帖子图片，返回代理 URL | 是 |
| `/forum/getForumImage/{encryptedKey}` | GET | 图片代理访问（302重定向） | 否 |

### 评论管理

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/forum/addComment` | POST | 添加评论 | 是 |
| `/forum/getCommentList` | POST | 评论列表 | 否 |
| `/forum/deleteComment` | POST | 删除评论 | 是 |

---

## 九、常见问题

### Q1: 帖子 MD 内容的存储键是什么格式？

**答**：`evernight/forum/post/{postId}.md`，其中 `postId` 为帖子主键 ID。例如帖子 ID 为 42 时，存储键为 `evernight/forum/post/42.md`。

### Q2: 前端如何渲染 Markdown 内容？

**答**：调用 `getPostDetail` 获取 `content` 字段，该字段即为 MD 原文字符串。前端可使用 `marked`、`markdown-it`、`md-editor-v3` 等任意 Markdown 库直接渲染或编辑。

### Q3: 删除帖子后评论和图片会怎样？

**答**：评论通过数据库外键 `ON DELETE CASCADE` 自动级联删除，无需手动处理。同时 OSS 中的 MD 文件、封面图片和帖子内图片也会被全部清理。

### Q4: 评论数是如何统计的？

**答**：帖子详情通过 SQL 子查询 `(SELECT COUNT(1) FROM forum_comment WHERE post_id = ?)` 实时统计评论数，保证数据一致性。列表接口中通过遍历查询各帖子评论数。

### Q5: 只有帖子作者才能编辑/删除吗？

**答**：是的。`updatePost`、`deletePost`、`deleteComment` 接口都会校验当前登录用户是否为操作对象的作者，非作者操作将返回 `603 权限不足`。

### Q6: 响应中的 `@class` 字段是什么？

**答**：由于 Redis JSON 序列化启用了 `DefaultTyping`，响应中会携带 `@class` 类型描述字段。前端解析时忽略即可，不影响业务数据。列表类型会被序列化为 `["java.util.ArrayList", [...]]` 格式，前端通过索引 `[1]` 获取实际数组。

### Q7: 搜索和列表是同一个接口吗？

**答**：是的。`/forum/getPostList` 的 `keyword` 参数为可选，不传或为空时返回全部已发布帖子，有值时按标题模糊匹配搜索。

### Q8: 热门帖子列表的缓存机制是怎样的？

**答**：`/forum/getHotPostList` 查询浏览量前 10 的帖子后，结果会缓存到 Redis（Key: `evernight:file:forumPost:`），过期时间 1 天。当调用 `incrementViewCount` 增加帖子浏览量时，会自动清除该缓存，下次请求时重新从数据库查询并缓存。前端无需关心缓存逻辑，正常调用接口即可。

### Q9: 帖子列表、详情和评论列表中的 `username` 字段是怎么来的？

**答**：帖子列表、帖子详情和评论列表接口均通过联表查询（`LEFT JOIN user`）同时获取用户名。帖子列表和详情返回发帖用户名，评论列表返回评论用户名。热门帖子接口返回 `HotPostDto` 同样包含发帖用户名。前端可直接展示发帖人/评论人信息，无需额外请求用户接口。

### Q10: 帖子的草稿和发布状态有什么区别？

**答**：
- **草稿**（`postStatus=0`）：创建帖子后默认为草稿状态，仅作者本人可查看和操作（编辑、上传封面/图片等），不会出现在帖子列表和热门帖子中
- **已发布**（`postStatus=1`）：调用 `publishPost` 后发布，所有人可见，会出现在帖子列表和热门帖子中
- 已发布的帖子可以编辑内容，也可以通过 `updatePostStatus` 撤回为草稿状态（`postStatus=0`），撤回后不会出现在帖子列表和热门帖子中

### Q11: 帖子图片的代理 URL 是如何工作的？

**答**：由于 OSS 存储桶为私有，图片无法直接公开访问。上传图片时后端返回一个代理 URL（如 `http://localhost:9092/forum/getForumImage/xxx`），该 URL 永久有效。当浏览器请求该 URL 时：
1. 后端解密得到 OSS 存储键
2. 生成 1 小时有效的预签名 URL
3. 302 重定向到 OSS，浏览器直接从 OSS 加载图片

因此 Markdown 中嵌入的代理 URL 永久有效，每次访问都会动态生成新的预签名 URL。

### Q12: 封面图片的存储键是什么格式？

**答**：`evernight/forum/postCover/{postId}.png`，其中 `postId` 为帖子 ID。每个帖子只能有一张封面，重复上传会覆盖原有封面。删除帖子时封面也会被清理。
