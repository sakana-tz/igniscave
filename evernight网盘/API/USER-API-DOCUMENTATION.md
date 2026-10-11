# Evernight 用户模块 API 文档

> **版本**：v1.6 | **更新时间**：2026-09-21

## 概述

Evernight 用户模块提供完整的用户注册、登录、账户管理功能，支持密码登录和邮箱验证码登录两种方式。

### 核心特性

- **用户注册**：邮箱验证码校验 + 账户创建
- **用户登录**：支持密码登录（含图像验证码）和邮箱验证码登录
- **账户管理**：修改用户名/密码、头像管理、用户信息查询
- **管理员功能**：用户列表分页查询、新增/修改/删除用户
- **会话管理**：JWT Token + Redis Session 双重认证

### 架构概览

```
前端
 ├─ 获取验证码 → AccountController → CaptchaService → Redis（存储验证码）
 ├─ 登录 → AccountController → CaptchaService（校验验证码）→ AccountService → MySQL + Redis（Session）+ JWT（Token）
 ├─ 注册 → AccountController → CaptchaService（校验邮箱验证码）→ AccountService → MySQL
 ├─ 账户管理 → AccountController → AccountService → MySQL + Redis
 └─ 管理员操作 → AdminUserController → UserService → MySQL
```

### 核心配置参数

| 配置项 | 值 | 说明 |
|--------|------|------|
| 图像验证码 TTL | 可配置 | Redis 中图像验证码过期时间 |
| 邮箱验证码 TTL | 可配置 | Redis 中邮箱验证码过期时间 |
| JWT Token 有效期 | 1 天 | JWT 签名有效时间（过期后即使 Cookie 存在也需重新登录） |
| Cookie 持久化时间 | 7 天 | `userTokenJwt` Cookie 在浏览器中的存活时间 |
| Session TTL | 1 天 | Redis 中用户 Session 过期时间 |

---

## 一、验证码

### 1.1 获取图像验证码

**接口**：`POST /account/getCaptcha`

**说明**：生成图像验证码，用于密码登录模式。返回 Base64 编码的图片，前端直接用于 `<img>` 标签显示。

**请求参数**：无

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "captchaKey": "a1b2c3d4e5f6",
    "captchaImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg..."
  }
}
```

**响应字段说明**（`CaptchaDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `captchaKey` | String | 验证码唯一标识，登录时需回传 |
| `captchaImage` | String | Base64 编码的验证码图片，直接用于 `<img src>` |

**前端示例**：

```javascript
const { data } = await api.post('/account/getCaptcha');
const { captchaKey, captchaImage } = data;

// 显示验证码图片
<img src={captchaImage} alt="验证码" />
```

**注意**：验证码 60 秒后过期，过期需重新获取。

---

### 1.2 发送邮箱验证码

**接口**：`POST /account/getEmailCaptcha`

**说明**：向指定邮箱发送验证码邮件，用于注册和邮箱验证码登录。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 目标邮箱地址（需符合邮箱格式） |

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
| 600 | 验证码已存在且未过期，请勿重复发送 |

**前端示例**：

```javascript
await api.post('/account/getEmailCaptcha', null, {
  params: { email: 'user@example.com' }
});
```

**注意**：邮箱验证码 5 分钟内有效，同一邮箱在验证码未过期前不可重复发送。

---

## 二、登录与注册

### 2.1 用户登录

**接口**：`POST /account/login`

**说明**：支持两种登录方式：
1. **密码登录**：需传 `email` + `password` + `checkCodeKey` + `checkCode`
2. **邮箱验证码登录**：需传 `email` + `emailCheckCode`

#### 方式一：密码登录

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 邮箱地址 |
| `password` | String | 是 | 密码（明文传输，后端 SHA256 校验） |
| `checkCodeKey` | String | 是 | 图像验证码 Key（从 `getCaptcha` 获取） |
| `checkCode` | String | 是 | 图像验证码答案 |

**请求示例**：

```javascript
await api.post('/account/login', null, {
  params: {
    email: 'user@example.com',
    password: 'myPassword123',
    checkCodeKey: 'a1b2c3d4e5f6',
    checkCode: 'Xk9m'
  }
});
```

#### 方式二：邮箱验证码登录

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | String | 是 | 邮箱地址 |
| `emailCheckCode` | String | 是 | 邮箱验证码（从邮件中获取） |

**请求示例**：

```javascript
await api.post('/account/login', null, {
  params: {
    email: 'user@example.com',
    emailCheckCode: 'A3B7K'
  }
});
```

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**响应说明**：
- `data`：JWT Token 字符串，同时通过 `Set-Cookie` 写入 `userTokenJwt`（HttpOnly，Cookie 持久化 7 天，但 JWT 本身 1 天后过期需重新登录）

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数不完整 |
| 604 | 账户已被禁用 |
| 605 | 验证码错误 |
| 606 | 账户或密码错误 |

**前端示例**：

```javascript
// 密码登录
async function loginWithPassword(email, password, captchaKey, captchaCode) {
  const { data: token } = await api.post('/account/login', null, {
    params: { email, password, checkCodeKey: captchaKey, checkCode: captchaCode }
  });
  // Token 已自动通过 Set-Cookie 写入，也可手动保存
  localStorage.setItem('token', token);
}

// 邮箱验证码登录
async function loginWithEmail(email, emailCode) {
  const { data: token } = await api.post('/account/login', null, {
    params: { email, emailCheckCode: emailCode }
  });
  localStorage.setItem('token', token);
}
```

---

### 2.2 用户注册

**接口**：`POST /account/register`

**说明**：用户注册，需先获取邮箱验证码。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | String | 是 | 用户名（2-30 字符） |
| `password` | String | 是 | 密码（6-42 字符） |
| `email` | String | 是 | 邮箱地址（需符合邮箱格式） |
| `emailCheckCode` | String | 是 | 邮箱验证码 |

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
| 601 | 邮箱已注册（数据已存在） |
| 605 | 邮箱验证码错误 |

**前端示例**：

```javascript
async function register(username, password, email, emailCode) {
  await api.post('/account/register', null, {
    params: { username, password, email, emailCheckCode: emailCode }
  });
  // 注册成功后跳转登录页
}
```

---

### 2.3 自动登录

**接口**：`POST /account/autoLogin`

**说明**：用于前端刷新页面后验证已有 Token 是否有效，无需传参，自动从 Cookie/Header 解析 Token。

**请求参数**：无（自动从请求头获取用户信息）

**请求头**：需要携带 JWT Token

**响应示例（Token 有效）**：

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
| 401 | Token 无效、已过期或 Session 已失效 |

**前端示例**：

```javascript
// 页面加载时检查登录状态
async function checkLoginStatus() {
  try {
    await api.post('/account/autoLogin');
    // 已登录，进入主页
  } catch (error) {
    if (error.response?.data?.code === 401) {
      // 未登录或 Token 过期，跳转登录页
    }
  }
}
```

---

### 2.4 登出

**接口**：`POST /account/logout`

**说明**：清除 Cookie 中的 Token，并删除 Redis 中的 Session。

**请求参数**：无（自动从请求头获取用户信息）

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**前端示例**：

```javascript
async function logout() {
  await api.post('/account/logout');
  localStorage.removeItem('token');
  // 跳转登录页
}
```

---

## 三、账户管理

### 3.1 修改账户信息

**接口**：`POST /account/updateAccount`

**说明**：修改用户名和/或密码。修改成功后会自动登出（清除 Cookie 和 Session），需重新登录。

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | String | 否 | 新用户名（2-30 字符） |
| `password` | String | 否 | 新密码（6-42 字符） |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**注意**：修改成功后会自动登出，前端需清除本地 Token 并跳转至登录页。

**前端示例**：

```javascript
async function updateAccount(username, password) {
  await api.post('/account/updateAccount', null, {
    params: { username, password }
  });
  // 修改成功，自动登出
  localStorage.removeItem('token');
  // 跳转登录页
}
```

---

### 3.2 获取用户信息

**接口**：`POST /account/getUserInfo`

**说明**：获取当前登录用户的基本信息。

**请求参数**：无（自动从请求头获取用户信息）

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "userId": 1,
    "username": "张三",
    "email": "zhangsan@example.com",
    "userStatus": 2,
    "userIp": "203.0.113.50",
    "expire": 1726905600000
  }
}
```

**响应字段说明**（`UserSessionDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | Integer | 用户 ID |
| `username` | String | 用户名 |
| `email` | String | 邮箱 |
| `userStatus` | Integer | 用户状态：0=管理员，1=禁用，2=普通用户 |
| `userIp` | String | 登录 IP 地址 |
| `expire` | Long | Session 过期时间（毫秒级绝对时间戳） |

---

## 四、头像管理

### 4.1 获取头像上传预签名URL

**接口**：`POST /account/getAvatarUploadUrl`

**说明**：获取 OSS 预签名上传 URL，前端使用 PUT 方式直传头像到 OSS。

**请求参数**：无（自动从请求头获取用户信息）

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/avatar/1.png?Expires=xxx&OSSAccessKeyId=xxx&Signature=xxx"
}
```

**响应说明**：
- `data`：OSS 预签名上传 URL（PUT 方式，1 小时有效），Content-Type 固定为 `image/png`

---

### 4.2 确认头像上传完成

**接口**：`POST /account/confirmAvatarUpload`

**说明**：前端完成 OSS 上传后，调用此接口确认上传完成并获取头像访问 URL。

**请求参数**：无（自动从请求头获取用户信息）

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/avatar/1.png?Expires=xxx"
}
```

**响应说明**：
- `data`：头像访问 URL，可直接用于 `<img>` 标签显示

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 610 | OSS 上未找到文件，上传未完成 |

---

### 4.3 获取头像

**接口**：`POST /account/getAvatar`

**说明**：获取当前用户头像的访问 URL。

**请求参数**：无（自动从请求头获取用户信息）

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/avatar/1.png?Expires=xxx"
}
```

**前端示例**：

```javascript
const { data: avatarUrl } = await api.post('/account/getAvatar');
<img src={avatarUrl} alt="用户头像" />
```

**注意**：头像 URL 有效期为 1 小时，过期后需重新调用接口获取新 URL。

---

### 4.4 头像上传完整示例

```javascript
async function uploadAvatar(avatarFile) {
  // 1. 获取上传URL
  const { data: uploadUrl } = await api.post('/account/getAvatarUploadUrl');

  // 2. 前端直传OSS（PUT方式）
  await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'image/png' },
    body: avatarFile
  });

  // 3. 确认上传完成，获取头像访问URL
  const { data: avatarUrl } = await api.post('/account/confirmAvatarUpload');

  // 4. 使用头像URL
  document.getElementById('avatar').src = avatarUrl;
}
```

---

## 五、管理员 — 用户管理

> 以下接口均需要管理员权限（`userStatus = 0`），普通用户调用将返回 603 错误。

### 5.1 分页查询用户列表

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

**请求头**：需要携带 JWT Token

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
      },
      {
        "userId": 2,
        "username": "张三",
        "email": "zhangsan@example.com",
        "createdTime": "2026-09-10 08:00:00",
        "updatedTime": null,
        "userStatus": 2,
        "isDeleted": 0
      }
    ],
    "totalCount": 25
  }
}
```

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 603 | 权限不足（非管理员） |

---

### 5.2 新增用户

**接口**：`POST /admin/user/add`

**说明**：管理员手动新增用户。

**请求参数**（JSON Body，`User` 对象）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | String | 是 | 用户名 |
| `password` | String | 是 | 密码（明文传输，后端自动加密） |
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

### 5.3 根据 UserId 查询用户

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

### 5.4 根据 UserId 修改用户

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
| `password` | String | 新密码 |
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

### 5.5 根据 UserId 删除用户

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

## 六、统一响应格式

所有接口统一使用 `ResponseVO` 响应格式：

```json
{
  "status": "success",      // 状态：success / error
  "code": 200,              // 状态码
  "info": "请求成功",        // 提示信息
  "data": { ... }           // 响应数据（具体结构因接口而异）
}
```

### 用户模块相关状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权或授权失效 |
| 600 | 业务繁忙，请稍后再试（验证码重复发送） |
| 601 | 数据已存在（邮箱已注册） |
| 603 | 权限不足（非管理员） |
| 604 | 账户已被禁用 |
| 605 | 验证码错误 |
| 606 | 账户或密码错误 |

---

## 七、认证方式

### JWT Token 认证

需要认证的接口会通过 `@UserInfoValidation` 注解自动从请求头解析用户信息。

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

**Token 有效期**：JWT 签名 1 天（Cookie 持久化 7 天，但 JWT 过期后需重新登录）

**Token 特性**：
- Cookie 设置了 `HttpOnly`，防止 XSS 攻击窃取
- Cookie 持久化时间为 7 天，但 JWT 过期后 Cookie 中的 Token 也无效
- 登录时自动通过 `Set-Cookie` 写入

### 会话校验流程

```
请求 → UserInfoCheckController（参数解析器）
  ├─ 从 Cookie/Header 获取 JWT Token
  ├─ 解析 JWT 获取 userId
  ├─ 从 Redis 获取 UserSessionDto
  ├─ 校验用户 IP 是否一致（防止 Token 盗用）
  └─ 校验用户状态是否正常（是否被禁用）
```

### 无需认证的接口

| 接口 | 说明 |
|------|------|
| `/account/getCaptcha` | 获取图像验证码 |
| `/account/getEmailCaptcha` | 发送邮箱验证码 |
| `/account/login` | 登录 |
| `/account/register` | 注册 |

---

## 八、Redis 数据结构（用户模块）

用户模块使用以下 Redis Key：

| Key 模式 | 数据类型 | 内容 | TTL |
|----------|----------|------|-----|
| `{session-key}:{userId}` | String (JSON) | `UserSessionDto` 对象 | 可配置 |
| `{captcha-key}:{captchaKey}` | String | 图像验证码答案 | 60 秒 |
| `{captcha-key}:{email}` | String | 邮箱验证码 | 5 分钟 |

**生命周期**：
- **Session**：登录时创建（`login`），登出时删除（`logout`），修改账户时删除（`updateAccount`）
- **图像验证码**：生成时创建（`getCaptcha`），校验后删除（`login`）
- **邮箱验证码**：发送时创建（`getEmailCaptcha`），校验后删除（`login` / `register`）

---

## 九、数据模型

### User — 用户表实体

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | Integer | 用户 ID（主键自增） |
| `username` | String | 用户名 |
| `password` | String | 加密密码（SHA256） |
| `email` | String | 邮箱 |
| `createdTime` | LocalDateTime | 注册时间（格式：yyyy-MM-dd HH:mm:ss） |
| `updatedTime` | LocalDateTime | 最后更新时间 |
| `userStatus` | Integer | 用户状态：0=管理员，1=禁用，2=普通用户 |
| `isDeleted` | Integer | 是否删除：0=正常，1=已删除 |

### UserSessionDto — 用户会话信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | Integer | 用户 ID |
| `username` | String | 用户名 |
| `email` | String | 邮箱 |
| `userStatus` | Integer | 用户状态：0=管理员，1=禁用，2=普通用户 |
| `userIp` | String | 登录 IP 地址 |
| `expire` | Long | Session 过期时间（毫秒级绝对时间戳） |

### CaptchaDto — 验证码响应

| 字段 | 类型 | 说明 |
|------|------|------|
| `captchaKey` | String | 验证码唯一标识 |
| `captchaImage` | String | Base64 编码的验证码图片 |

### UserQuery — 用户查询参数

| 字段 | 类型 | 说明 |
|------|------|------|
| `pageNo` | Integer | 页码 |
| `pageSize` | Integer | 每页条数 |
| `userId` | Integer | 用户 ID（精确匹配） |
| `username` | String | 用户名（精确匹配） |
| `usernameFuzzy` | String | 用户名（模糊匹配） |
| `email` | String | 邮箱（精确匹配） |
| `emailFuzzy` | String | 邮箱（模糊匹配） |
| `userStatus` | Integer | 用户状态 |
| `isDeleted` | Integer | 是否删除 |

---

## 十、常见问题

### Q1: 登录后 Token 存在哪里？

**答**：Token 同时存在于两个位置：
1. **Cookie**：`userTokenJwt`（HttpOnly，自动携带）
2. **响应体**：`data` 字段返回 JWT 字符串（前端可手动存储）

### Q2: Token 过期了怎么办？

**答**：Token 有效期 1 天，过期后需重新登录。前端可通过 `/account/autoLogin` 接口检测 Token 是否有效，若返回 401 则跳转登录页。

### Q3: 修改账户信息后为什么需要重新登录？

**答**：修改用户名或密码后，后端会主动清除 Session 和 Cookie，确保安全。前端需清除本地 Token 并跳转登录页。

### Q4: 图像验证码和邮箱验证码的区别？

**答**：
- **图像验证码**：用于密码登录模式，Base64 图片形式，过期后需重新获取
- **邮箱验证码**：用于注册和邮箱登录，发送到邮箱，过期后需重新发送

### Q5: 管理员接口如何鉴权？

**答**：管理员接口（`/admin/user/*`）通过 `@UserInfoValidation` 获取用户 Session，检查 `userStatus` 是否为 0（管理员），非管理员返回 603 错误。

### Q6: 头像 URL 为什么有时无法显示？

**答**：头像 URL 为 OSS 预签名 URL，有效期 1 小时。过期后需重新调用 `/account/getAvatar` 获取新 URL。

---

## 十一、API 接口汇总

### 验证码

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/account/getCaptcha` | POST | 获取图像验证码 | 否 |
| `/account/getEmailCaptcha` | POST | 发送邮箱验证码 | 否 |

### 登录注册

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/account/login` | POST | 用户登录 | 否 |
| `/account/register` | POST | 用户注册 | 否 |
| `/account/autoLogin` | POST | 自动登录（Token 校验） | 是 |
| `/account/logout` | POST | 登出 | 是 |

### 账户管理

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/account/updateAccount` | POST | 修改账户信息 | 是 |
| `/account/getUserInfo` | POST | 获取用户信息 | 是 |

### 头像管理

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/account/getAvatarUploadUrl` | POST | 获取头像上传预签名URL | 是 |
| `/account/confirmAvatarUpload` | POST | 确认头像上传完成 | 是 |
| `/account/getAvatar` | POST | 获取头像 | 是 |

### 管理员 — 用户管理

| 接口 | 方法 | 说明 | 认证 |
|------|------|------|------|
| `/admin/user/loadDataList` | POST | 分页查询用户列表 | 是（管理员） |
| `/admin/user/add` | POST | 新增用户 | 是（管理员） |
| `/admin/user/getUserByUserId` | POST | 根据ID查询用户 | 是（管理员） |
| `/admin/user/updateUserByUserId` | POST | 根据ID修改用户 | 是（管理员） |
| `/admin/user/deleteUserByUserId` | POST | 根据ID删除用户 | 是（管理员） |

---

**文档版本**：v1.6  
**更新时间**：2026-09-21  
**更新内容**：补充 Cookie 持久化时间（7 天）与 JWT 有效期（1 天）的区分说明  
**技术支持**：如有问题，请联系后端开发团队
