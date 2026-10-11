# Evernight 文件处理模块 API 文档

> **版本**：v1.4 | **更新时间**：2026-09-21

## 概述

Evernight 文件处理模块提供完整的文件上传、下载、管理功能，支持普通文件和大文件分片处理。

### 核心特性

- **普通文件**：直接上传下载，适合小文件（< 10MB）
- **大文件**：分片上传 + 分片下载，支持断点续传
- **文件管理**：文件列表、删除恢复、路径修改、分享功能
- **头像管理**：用户头像上传和获取

### 架构概览

```
前端
 ├─ 普通文件上传 → FileController → FileOssService → 返回预签名URL → 前端直传OSS → 确认入库
 ├─ 大文件分片上传 → BigFileController → BigFileOssService → 返回预签名URL列表 → 前端直传OSS各分片 → 上报完成 → 合并
 ├─ 断点续传恢复 → BigFileController → BigFileOssService → 通过文件信息反查uploadId → 恢复上传
 ├─ 大文件分片下载 → BigFileController → BigFileOssService → OSS
 └─ 文件管理 → FileController → FileOssService → MySQL + OSS
```

### 核心配置参数

| 配置项 | 值 | 说明 |
|--------|------|------|
| `chunk-size` | 5MB (5242880 B) | 上传分片大小 |
| `download-chunk-size` | 5MB (5242880 B) | 下载推荐分片大小 |
| `download-chunk-threshold` | 10MB (10485760 B) | 断点续传阈值，文件大小超过此值建议使用分片下载 |
| `max-file-size` | 2GB (2147483648 B) | 单文件最大大小限制 |
| 每日流量上限 | 5GB (5368709120 B) | 每用户每日上传流量限制 |
| 分片状态 TTL | 1天 (86400s) | Redis 中分片信息过期时间 |

---

## 一、文件上传

### 1.1 普通文件上传

普通文件采用前端直传OSS方式：先获取预签名URL → 前端PUT上传OSS → 确认入库

#### 1.1.1 获取上传预签名URL

**接口**：`POST /file/getUploadUrl`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileName` | String | 是 | 文件名（含扩展名） |
| `fileSize` | Long | 是 | 文件大小（字节） |
| `contentType` | String | 是 | 文件MIME类型（如 `application/pdf`） |
| `path` | String | 是 | 文件存储目录路径（自动校正格式） |

**请求头**：需要携带 JWT Token

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "uploadUrl": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/file/1/documents/test.pdf?Expires=xxx&OSSAccessKeyId=xxx&Signature=xxx",
    "ossKey": "evernight/file/1/documents/test.pdf"
  }
}
```

**响应字段说明**（`FileUploadInfo`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadUrl` | String | OSS预签名上传URL（PUT方式，1小时有效） |
| `ossKey` | String | 文件在OSS上的存储路径，确认上传时需回传 |

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 611 | 文件超出大小限制 或 当日流量超限 |

#### 1.1.2 前端直传OSS

获取到 `uploadUrl` 后，前端使用 PUT 方式直接上传文件到 OSS：

```javascript
await fetch(uploadUrl, {
  method: 'PUT',
  headers: { 'Content-Type': contentType },
  body: file
});
```

#### 1.1.3 确认上传完成

**接口**：`POST /file/confirmUpload`

**说明**：前端完成OSS上传后，调用此接口通知后端入库文件记录

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `ossKey` | String | 是 | 获取URL时返回的ossKey |
| `fileName` | String | 是 | 文件名 |
| `fileSize` | Long | 是 | 文件大小（字节） |
| `contentType` | String | 是 | 文件MIME类型 |
| `path` | String | 是 | 文件存储目录路径 |

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
| 610 | OSS上未找到文件，上传未完成 |

#### 1.1.4 前端完整示例

```javascript
async function uploadFile(file, path) {
  // 1. 获取上传URL
  const { data } = await api.post('/file/getUploadUrl', {
    fileName: file.name,
    fileSize: file.size,
    contentType: file.type,
    path
  });
  const { uploadUrl, ossKey } = data;

  // 2. 前端直传OSS（PUT方式）
  await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type },
    body: file
  });

  // 3. 确认上传完成
  await api.post('/file/confirmUpload', {
    ossKey,
    fileName: file.name,
    fileSize: file.size,
    contentType: file.type,
    path
  });
}
```

---

### 1.2 大文件分片上传

大文件采用前端直传 OSS 模式：预上传获取预签名 URL → 前端 PUT 各分片直传 OSS → 上报分片完成 → 合并文件

#### 1.2.1 预上传 — 初始化分片上传并获取预签名 URL

**接口**：`POST /file/preUploadFile`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `path` | String | 是 | 文件存储目录路径（自动校正格式） |
| `fileName` | String | 是 | 文件名（含扩展名） |
| `fileSize` | Long | 是 | 文件总大小（字节） |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "uploadId": "0004B999EF5A1A7078B2D55A",
    "chunkSize": 5242880,
    "totalChunks": 20,
    "presignedUrls": [
      "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/file/1/test.zip?uploadId=xxx&partNumber=1&Expires=xxx",
      "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/file/1/test.zip?uploadId=xxx&partNumber=2&Expires=xxx"
    ]
  }
}
```

**响应字段说明**（`MultipartUploadInitDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadId` | String | 分片上传 ID，后续操作都需要此 ID |
| `chunkSize` | long | 设定的单个分片大小（字节） |
| `totalChunks` | int | 分片总数 |
| `presignedUrls` | List\<String\> | 每个分片的预签名上传 URL 列表（PUT 方式，1小时有效），下标 0 对应 partNumber=1 |

**后端处理逻辑**：

1. 检查文件大小是否超过 `max-file-size`（2GB）
2. 检查并累加用户当日流量（上限 5GB/天）
3. 调用 OSS `initMultipartUpload` 获取 `uploadId`
4. 为每个分片生成预签名 PUT URL（1小时有效）
5. 将分片元信息（`ChunkFileDto`）存入 Redis，TTL = 1天
6. 存储 `ossKey → uploadId` 索引映射到 Redis（用于页面刷新后断点续传查询）
7. 返回 `MultipartUploadInitDto`

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 611 | 文件超出大小限制 或 当日流量超限 |

#### 1.2.2 前端直传 OSS 分片

前端使用预签名 URL 通过 PUT 方式直接将各分片上传到 OSS，**不经过后端**：

```javascript
// presignedUrls[i] 对应 partNumber = i + 1
for (let i = 0; i < totalChunks; i++) {
  const chunk = file.slice(i * chunkSize, Math.min((i + 1) * chunkSize, file.size));
  await fetch(presignedUrls[i], {
    method: 'PUT',
    body: chunk
  });
}
```

#### 1.2.3 上报分片上传完成

**接口**：`POST /file/reportUploadedParts`

**说明**：前端使用预签名 URL 将分片直接上传到 OSS 后，调用此方法上报已成功上传的分片编号，后端更新 Redis 分片状态

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `uploadId` | String | 是 | 上传 ID |
| `uploadedParts` | List\<Integer\> | 是 | 已成功上传的分片编号列表（从 1 开始） |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**后端处理逻辑**：

1. 从 Redis 读取 `ChunkFileDto`，校验 `uploadId` 有效性
2. 在 Redis BitMap（`chunkStatus`）中标记已上传的分片
3. 续期 Redis Key 的 TTL

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 400 | uploadId 无效或已过期 |

#### 1.2.4 合并文件

**接口**：`POST /file/mergeFile`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `uploadId` | String | 是 | 上传ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**后端处理逻辑**：

1. 从 Redis 读取 `ChunkFileDto`，校验有效性
2. 通过 BitMap 计数校验所有分片是否都已上传
3. 调用 OSS `listParts` 获取各分片 ETag，然后调用 `completeMultipartUpload` 合并文件
4. 清理 Redis 中所有分片相关 Key（chunkInfo / chunkStatus / uploadIndex）
5. 查询 OSS 获取合并后文件大小
6. 向 MySQL 插入文件记录
7. 清理 Redis 文件列表缓存

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 610 | 分片不完整，合并失败 |

#### 1.2.5 查询上传状态

**接口**：`POST /file/getUploadStatus`

**说明**：查询某个 `uploadId` 的分片上传进度，用于断点续传恢复。**不要频繁调用，不要每上传一次分片就调用一次。**

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `uploadId` | String | 是 | 上传ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "uploadId": "0004B999EF5A1A7078B2D55A",
    "filename": "example.zip",
    "stChunkSize": 5242880,
    "totalChunks": 20,
    "chunkStatusList": [1, 2, 3, 5, 7]
  }
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadId` | String | 上传ID |
| `filename` | String | 文件名 |
| `stChunkSize` | long | 设定的单个分片大小（字节） |
| `totalChunks` | Integer | 分片总数 |
| `chunkStatusList` | List\<Integer\> | 已上传完成的分片编号列表 |

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 400 | uploadId 无效或已过期 |

#### 1.2.6 检查待恢复的上传（页面刷新后断点续传）

**接口**：`POST /file/checkPendingUpload`

**说明**：页面刷新后，前端通过文件路径和文件名查询是否存在未完成的上传任务。后端通过 Redis 中存储的 `ossKey → uploadId` 索引反查，如果存在则返回 `uploadId` 和分片上传状态，前端可据此恢复上传。**无需依赖 localStorage 保存 uploadId。**

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `path` | String | 是 | 文件存储目录路径 |
| `fileName` | String | 是 | 文件名（含扩展名） |

**响应示例（存在待恢复上传）**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "uploadId": "0004B999EF5A1A7078B2D55A",
    "filename": "example.zip",
    "stChunkSize": 5242880,
    "totalChunks": 20,
    "chunkStatusList": [1, 2, 3, 5, 7]
  }
}
```

**响应示例（无待恢复上传）**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

**响应字段说明**（`ChunkStatusDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadId` | String | 上传ID，用于后续 `refreshPresignedUrls` 和 `reportUploadedParts` |
| `filename` | String | 文件名 |
| `stChunkSize` | long | 设定的单个分片大小（字节） |
| `totalChunks` | Integer | 分片总数 |
| `chunkStatusList` | List\<Integer\> | 已上传完成的分片编号列表 |

**后端处理逻辑**：

1. 根据 `path + fileName` 构建 ossKey，通过 `uploadIndex` 索引查找 `uploadId`
2. 如果索引不存在，返回 `null`（表示没有待恢复的上传）
3. 验证 `ChunkFileDto` 是否仍然存在（TTL 未过期）
4. 如果 `ChunkFileDto` 已过期，清理残留索引并返回 `null`
5. 查询 BitMap 获取已上传分片列表，返回 `ChunkStatusDto`

**恢复上传流程**：

```
页面刷新 → 调用 checkPendingUpload(path, fileName)
  ├─ 返回 null → 走正常 preUploadFile 新上传流程
  └─ 返回 ChunkStatusDto → 拿到 uploadId + chunkStatusList
       → 计算未完成分片 → 调用 refreshPresignedUrls 获取新预签名URL
       → 继续上传未完成分片 → reportUploadedParts → mergeFile
```

---

### 1.3 分片上传完整示例

```javascript
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB，与后端 chunk-size 一致

async function uploadFile(file, path) {
  let uploadId, chunkSize, totalChunks, presignedUrls, uploadedParts;

  // 1. 页面刷新后，先检查是否有待恢复的上传（通过后端 Redis 索引查询）
  const { data: pending } = await api.post('/file/checkPendingUpload', {
    path,
    fileName: file.name
  });

  if (pending) {
    // 存在待恢复的上传
    uploadId = pending.uploadId;
    chunkSize = pending.stChunkSize;
    totalChunks = pending.totalChunks;
    uploadedParts = new Set(pending.chunkStatusList || []);

    // 计算未完成的分片编号，重新获取这些分片的预签名URL
    const remainingParts = [];
    for (let i = 1; i <= totalChunks; i++) {
      if (!uploadedParts.has(i)) remainingParts.push(i);
    }

    if (remainingParts.length > 0) {
      // 调用 refreshPresignedUrls 获取未完成分片的新预签名URL
      const { data: refreshed } = await api.post('/file/refreshPresignedUrls', {
        uploadId,
        partNumbers: remainingParts
      });
      presignedUrls = buildPresignedUrlsMap(remainingParts, refreshed.presignedUrls);
    } else {
      presignedUrls = {}; // 所有分片已完成，直接合并
    }
  } else {
    // 2. 无待恢复上传，走正常预上传流程
    const { data: initInfo } = await api.post('/file/preUploadFile', {
      path,
      fileName: file.name,
      fileSize: file.size
    });
    uploadId = initInfo.uploadId;
    chunkSize = initInfo.chunkSize;
    totalChunks = initInfo.totalChunks;

    // 构建 presignedUrls 映射：partNumber -> url
    presignedUrls = {};
    initInfo.presignedUrls.forEach((url, index) => {
      presignedUrls[index + 1] = url;
    });
    uploadedParts = new Set();
  }

  // 3. 逐片上传（跳过已完成的分片，使用预签名 URL 直传 OSS）
  const uploadedPartNumbers = [];
  for (let partNumber = 1; partNumber <= totalChunks; partNumber++) {
    if (uploadedParts.has(partNumber)) continue;

    const start = (partNumber - 1) * chunkSize;
    const end = Math.min(start + chunkSize, file.size);
    const chunk = file.slice(start, end);

    // 前端直接 PUT 到 OSS，不经过后端
    const response = await fetch(presignedUrls[partNumber], {
      method: 'PUT',
      body: chunk
    });

    if (!response.ok) {
      // 如果预签名URL过期（403），刷新URL后重试
      if (response.status === 403) {
        const { data: refreshed } = await api.post('/file/refreshPresignedUrls', {
          uploadId,
          partNumbers: [partNumber]
        });
        presignedUrls[partNumber] = refreshed.presignedUrls[0];

        // 重试上传
        await fetch(presignedUrls[partNumber], {
          method: 'PUT',
          body: chunk
        });
      } else {
        throw new Error(`分片 ${partNumber} 上传失败: ${response.status}`);
      }
    }

    uploadedPartNumbers.push(partNumber);
  }

  // 4. 上报分片上传完成
  if (uploadedPartNumbers.length > 0) {
    await api.post('/file/reportUploadedParts', {
      uploadId,
      uploadedParts: uploadedPartNumbers
    });
  }

  // 5. 合并文件
  await api.post('/file/mergeFile', { uploadId });
}

/**
 * 将分片编号列表和对应的URL列表构建为映射
 */
function buildPresignedUrlsMap(partNumbers, urls) {
  const map = {};
  partNumbers.forEach((num, index) => {
    map[num] = urls[index];
  });
  return map;
}
```

---

## 二、文件下载

### 2.1 普通文件下载

**接口**：`POST /file/getUserFile`

**适用场景**：小文件（< 10MB）

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | Long | 是 | 文件ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "url": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/file/1/test.pdf?Expires=xxx",
    "fileSize": 3145728,
    "contentType": "image/png",
    "chunkSize": null,
    "chunkThreshold": null
  }
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `url` | String | OSS 预签名下载 URL（**1小时有效**） |
| `fileSize` | Long | 文件总大小（字节） |
| `contentType` | String | 文件 MIME 类型 |
| `chunkSize` | Long | 推荐分片下载大小（小文件为 null） |
| `chunkThreshold` | Long | 断点续传阈值（小文件为 null） |

**前端示例**：

```javascript
const response = await api.post('/file/getUserFile', { fileId: 123 });
const { url, fileSize, contentType } = response.data;

// 方式1：直接打开URL下载
window.open(url, '_blank');

// 方式2：使用a标签下载
<a href={url} download="filename.pdf">下载文件</a>
```

---

### 2.2 大文件分片下载

**接口**：`POST /file/downloadBigFile`

**适用场景**：大文件（≥ 10MB），支持断点续传

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | Long | 是 | 文件ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "url": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/file/1/...",
    "fileSize": 52428800,
    "contentType": "video/mp4",
    "chunkSize": 5242880,
    "chunkThreshold": 10485760
  }
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `url` | String | OSS 预签名下载 URL（**1小时有效**） |
| `fileSize` | Long | 文件总大小（字节） |
| `contentType` | String | 文件 MIME 类型 |
| `chunkSize` | Long | 推荐分片下载大小（字节） |
| `chunkThreshold` | Long | 断点续传阈值（字节） |

**前端分片下载示例**：

```javascript
async function downloadBigFile(fileId) {
  // 1. 获取下载信息
  const { data } = await api.post('/file/downloadBigFile', { fileId });
  const { url, fileSize, chunkSize, contentType } = data;

  const storageKey = `download_${fileId}`;

  // 2. 计算分片范围
  const totalChunks = Math.ceil(fileSize / chunkSize);
  const chunks = [];
  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize;
    const end = Math.min(start + chunkSize - 1, fileSize - 1);
    chunks.push({ start, end });
  }

  // 3. 读取已下载进度
  const downloadedParts = new Set(
    JSON.parse(localStorage.getItem(storageKey) || '[]')
  );

  // 4. 逐片下载（跳过已完成的分片）
  const parts = new Array(totalChunks);
  for (let i = 0; i < totalChunks; i++) {
    if (downloadedParts.has(i)) continue;

    const { start, end } = chunks[i];
    const response = await fetch(url, {
      headers: { 'Range': `bytes=${start}-${end}` }
    });
    parts[i] = await response.arrayBuffer();

    // 记录进度
    downloadedParts.add(i);
    localStorage.setItem(storageKey, JSON.stringify([...downloadedParts]));
  }

  // 5. 合并 Blob 并触发下载
  const blob = new Blob(parts, { type: contentType });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = 'downloaded_file';
  link.click();
  URL.revokeObjectURL(link.href);

  // 6. 清理进度
  localStorage.removeItem(storageKey);
}
```

---

### 2.3 下载策略选择

| 文件大小 | 推荐接口 | 下载方式 |
|----------|----------|----------|
| < 10MB | `/file/getUserFile` | 直接访问 URL 下载 |
| ≥ 10MB | `/file/downloadBigFile` | Range 分片下载，支持断点续传 |

---

## 三、文件管理

### 3.1 获取用户文件列表

**接口**：`POST /file/getUserFileList`

**请求参数**：无（自动从请求头获取用户信息）

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": [
    {
      "fileId": 1,
      "isDel": false,
      "label": "test.pdf",
      "path": "documents/test.pdf",
      "type": 0,
      "size": 1048576,
      "updateTime": "2026-09-19T10:30:00",
      "children": []
    },
    {
      "fileId": null,
      "isDel": false,
      "label": "documents",
      "path": "documents",
      "type": 1,
      "size": null,
      "updateTime": null,
      "children": [...]
    }
  ]
}
```

**响应字段说明**（`FileTreeDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `fileId` | Long | 文件ID（文件夹为 null） |
| `isDel` | boolean | 是否在回收站 |
| `label` | String | 节点显示名称 |
| `path` | String | 完整路径 |
| `type` | Integer | 类型：0=文件，1=文件夹 |
| `size` | Long | 文件大小（字节，文件夹为 null） |
| `updateTime` | LocalDateTime | 更新时间（文件夹为 null） |
| `children` | List\<FileTreeDto\> | 子节点列表 |

---

### 3.2 删除或恢复文件

**接口**：`POST /file/delOrResUserFile`

**说明**：将文件移入回收站，或从回收站恢复

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | Long | 是 | 文件ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

---

### 3.3 彻底删除文件

**接口**：`POST /file/deleteFileByFileId`

**说明**：从 OSS 和数据库中彻底删除文件，不可恢复

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | Long | 是 | 文件ID |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

---

### 3.4 修改文件路径

**接口**：`POST /file/updateUserFilePath`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | Long | 是 | 文件ID |
| `path` | String | 是 | 新的存储目录路径（自动校正格式） |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": null
}
```

---

### 3.5 分享文件

**接口**：`POST /file/shareUserFile`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `fileId` | Long | 是 | 文件ID |
| `limitSecond` | Integer | 是 | 分享有效期（秒） |
| `limitCount` | Integer | 是 | 最大下载次数 |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "shareKey": "abc123def456",
    "limitShareCount": 10,
    "shareCount": 0,
    "expire": 86400,
    "fileId": 123,
    "masterId": 1,
    "remainSeconds": 86400
  }
}
```

**响应字段说明**（`ShareFileDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `shareKey` | String | 分享密钥（用于获取分享链接） |
| `limitShareCount` | Integer | 最大下载次数 |
| `shareCount` | Integer | 已下载次数 |
| `expire` | Long | 过期时间（秒） |
| `fileId` | Long | 文件ID |
| `masterId` | Integer | 分享者用户ID |
| `remainSeconds` | Long | 剩余有效时间（秒，计算属性） |

---

### 3.6 获取分享文件

**接口**：`GET /file/getShareFileUrl/{shareKey}`

**说明**：无需登录，通过分享密钥获取文件下载链接

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `shareKey` | String | 是 | 分享密钥 |

**响应示例**：

```json
{
  "status": "success",
  "code": 200,
  "info": "请求成功",
  "data": {
    "shareKey": "abc123def456",
    "shareLimitCount": 10,
    "shareCount": 1,
    "expire": 86400,
    "masterId": 1,
    "fileName": "test.pdf",
    "fileUrl": "https://igniscave.oss-cn-hangzhou.aliyuncs.com/evernight/file/1/test.pdf?Expires=xxx",
    "remainSeconds": 86400
  }
}
```

**响应字段说明**（`ShareFileToUserDto`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `shareKey` | String | 分享密钥 |
| `shareLimitCount` | Integer | 最大下载次数 |
| `shareCount` | Integer | 已下载次数 |
| `expire` | Long | 过期时间（秒） |
| `masterId` | Integer | 分享者用户ID |
| `fileName` | String | 文件名 |
| `fileUrl` | String | OSS 预签名下载 URL |
| `remainSeconds` | Long | 剩余有效时间（秒，计算属性） |

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 602 | 分享链接不存在或已失效 |

---

## 四、头像管理

### 4.1 上传头像

头像上传采用前端直传OSS方式：先获取预签名URL → 前端PUT上传OSS → 确认上传

#### 4.1.1 获取头像上传预签名URL

**接口**：`POST /account/getAvatarUploadUrl`

**请求参数**：无（自动从请求头获取用户信息）

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
- `data`：OSS预签名上传URL（PUT方式，1小时有效），前端使用 PUT 方式上传，Content-Type 固定为 `image/png`

#### 4.1.2 前端直传OSS

```javascript
await fetch(uploadUrl, {
  method: 'PUT',
  headers: { 'Content-Type': 'image/png' },
  body: avatarFile
});
```

#### 4.1.3 确认头像上传完成

**接口**：`POST /account/confirmAvatarUpload`

**请求参数**：无（自动从请求头获取用户信息）

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
- `data`：头像访问URL，可直接用于 `<img>` 标签显示

**错误码**：

| 错误码 | 说明 |
|--------|------|
| 610 | OSS上未找到文件，上传未完成 |

#### 4.1.4 前端完整示例

```javascript
async function uploadAvatar(avatarFile) {
  // 1. 获取上传URL
  const { data: uploadUrl } = await api.post('/account/getAvatarUploadUrl');

  // 2. 直传OSS
  await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'image/png' },
    body: avatarFile
  });

  // 3. 确认上传
  const { data: avatarUrl } = await api.post('/account/confirmAvatarUpload');

  // 使用头像URL
  <img src={avatarUrl} alt="用户头像" />
}
```

---

### 4.2 获取头像

**接口**：`POST /account/getAvatar`

**请求参数**：无（自动从请求头获取用户信息）

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
const response = await api.post('/account/getAvatar');
const avatarUrl = response.data;

// 直接用于 img 标签
<img src={avatarUrl} alt="用户头像" />
```

**注意**：头像 URL 有效期为 1 小时，过期后需重新调用接口获取新 URL。

---

## 五、统一响应格式

所有接口统一使用 `ResponseVO` 响应格式：

```json
{
  "status": "success",      // 状态：success / error
  "code": 200,              // 状态码
  "info": "请求成功",        // 提示信息
  "data": { ... }           // 响应数据（具体结构因接口而异）
}
```

### 常见状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权或授权失效 |
| 602 | 数据不存在 |
| 610 | 文件操作异常 |
| 611 | 文件超出限制 |

---

## 六、认证方式

### JWT Token 认证

需要认证的接口会通过 `@UserInfoValidation` 注解自动从请求头解析用户信息。

**请求头格式**：

```
Cookie: userTokenJwt=eyJhbGciOiJIUzI1NiJ9...
```

或

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Token 有效期**：1 天

**Token 过期处理**：重新登录获取新 Token

---

## 七、预签名 URL 注意事项

### 7.1 有效期

- **下载 URL**：1 小时有效期
- **上传 URL**：根据业务需求设置

### 7.2 URL 刷新

如果 URL 过期，需要重新调用接口获取新的 URL：

```javascript
async function getFreshUrl(fileId) {
  const response = await api.post('/file/getUserFile', { fileId });
  return response.data.url;
}

// 下载前检查 URL 是否过期
async function downloadFile(fileId) {
  let url = getCachedUrl(fileId);
  
  if (!url || isUrlExpired(url)) {
    url = await getFreshUrl(fileId);
    cacheUrl(fileId, url);
  }
  
  window.open(url);
}
```

### 7.3 跨域问题

OSS 已配置 CORS，前端可直接访问。如遇到跨域问题，请检查：
- OSS Bucket 的 CORS 配置
- 预签名 URL 是否正确

### 7.4 文件名问题

OSS 预签名 URL 默认使用 inline 方式打开，如果需要下载并保留文件名，使用 `<a>` 标签的 `download` 属性：

```html
<a href={url} download="filename.pdf">下载文件</a>
```

---

## 八、Redis 数据结构（大文件上传）

分片上传过程中，后端使用以下 Redis Key 存储上传状态：

| Key 模式 | 数据类型 | 内容 | TTL |
|----------|----------|------|-----|
| `file:chunkFile:{userId}:{uploadId}:chunkInfo` | String (JSON) | `ChunkFileDto` 对象 | 1天 |
| `file:chunkFile:{userId}:{uploadId}:chunkStatus` | BitMap | offset=分片序号, value=0/1 | 1天 |
| `file:chunkFile:{userId}:uploadIndex:{ossKey}` | String | `uploadId`（文件→上传ID索引） | 1天 |

**生命周期**：
- **创建**：`preUploadFile` 时写入 `chunkInfo` 和 `uploadIndex`
- **更新**：`reportUploadedParts` 标记 BitMap + 续期 TTL
- **清理**：`mergeFile` 成功后删除所有 Key（chunkInfo / chunkStatus / uploadIndex）

---

## 九、数据模型

### ChunkFileDto — 分片上传元信息（Redis 存储）

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadId` | String | OSS 分片上传ID |
| `filename` | String | 原始文件名 |
| `stChunkSize` | long | 设定的单个分片大小（字节） |
| `totalChunks` | Integer | 分片总数 |
| `downloadKey` | String | OSS 存储路径（不含用户前缀） |

### ChunkStatusDto — 上传状态查询响应

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadId` | String | 上传ID |
| `filename` | String | 文件名 |
| `stChunkSize` | long | 单个分片大小 |
| `totalChunks` | Integer | 分片总数 |
| `chunkStatusList` | List\<Integer\> | 已上传完成的分片编号列表 |

### MultipartUploadInitDto — 分片上传初始化响应

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadId` | String | 分片上传 ID |
| `chunkSize` | long | 设定的单个分片大小（字节） |
| `totalChunks` | int | 分片总数 |
| `presignedUrls` | List\<String\> | 每个分片的预签名上传 URL 列表（PUT 方式，1小时有效），下标 0 对应 partNumber=1 |

### FileUploadInfo — 文件上传信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `uploadUrl` | String | OSS预签名上传URL（PUT方式，1小时有效） |
| `ossKey` | String | 文件在OSS上的存储路径 |

### FileDownloadInfo — 文件下载信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `url` | String | OSS 预签名下载 URL |
| `fileSize` | Long | 文件大小（字节） |
| `contentType` | String | MIME 类型 |
| `chunkSize` | Long | 推荐下载分片大小（大文件接口返回） |
| `chunkThreshold` | Long | 断点续传阈值（大文件接口返回） |

### FileTreeDto — 文件树节点

| 字段 | 类型 | 说明 |
|------|------|------|
| `fileId` | Long | 文件ID |
| `isDel` | boolean | 是否在回收站 |
| `label` | String | 节点显示名称 |
| `path` | String | 完整路径 |
| `type` | Integer | 类型：0=文件，1=文件夹 |
| `size` | Long | 文件大小（字节） |
| `updateTime` | LocalDateTime | 更新时间 |
| `children` | List\<FileTreeDto\> | 子节点列表 |

### ShareFileDto — 分享文件信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `shareKey` | String | 分享密钥 |
| `limitShareCount` | Integer | 最大下载次数 |
| `shareCount` | Integer | 已下载次数 |
| `expire` | Long | 过期时间（秒） |
| `fileId` | Long | 文件ID |
| `masterId` | Integer | 分享者用户ID |
| `remainSeconds` | Long | 剩余有效时间（秒） |

### ShareFileToUserDto — 分享文件下载信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `shareKey` | String | 分享密钥 |
| `shareLimitCount` | Integer | 最大下载次数 |
| `shareCount` | Integer | 已下载次数 |
| `expire` | Long | 过期时间（秒） |
| `masterId` | Integer | 分享者用户ID |
| `fileName` | String | 文件名 |
| `fileUrl` | String | OSS 预签名下载 URL |
| `remainSeconds` | Long | 剩余有效时间（秒） |

---

## 十、常见问题

### Q1: 预签名 URL 访问返回 403

**原因**：URL 已过期或签名错误

**解决**：重新调用接口获取新的 URL

### Q2: 下载文件没有文件名

**原因**：OSS 预签名 URL 默认使用 inline 方式打开

**解决**：使用 `<a>` 标签的 `download` 属性指定文件名

```html
<a href={url} download="filename.pdf">下载</a>
```

### Q3: 大文件下载很慢

**原因**：单线程下载

**解决**：使用分片下载（Range 请求），参考上文分片下载示例

### Q4: 分片上传失败或页面刷新后如何恢复

**解决**：页面刷新后，前端调用 `checkPendingUpload(path, fileName)` 通过后端 Redis 索引反查 `uploadId` 和已上传分片状态，然后调用 `refreshPresignedUrls` 获取新的预签名 URL 继续上传。无需依赖 localStorage 保存上传状态。如果 Redis 中的分片信息已过期（TTL = 1天），则需重新调用 `preUploadFile` 开始新上传。

### Q5: 头像显示失败

**原因**：头像 URL 过期（有效期 1 小时）

**解决**：重新调用 `/account/getAvatar` 获取新 URL

---

## 十一、API 接口汇总

### 文件上传

| 接口 | 方法 | 说明 |
|------|------|------|
| `/file/getUploadUrl` | POST | 获取普通文件上传预签名URL |
| `/file/confirmUpload` | POST | 确认文件上传完成 |
| `/file/preUploadFile` | POST | 大文件预上传（返回预签名 URL 列表） |
| `/file/reportUploadedParts` | POST | 上报分片上传完成 |
| `/file/mergeFile` | POST | 合并文件 |
| `/file/getUploadStatus` | POST | 查询上传状态 |
| `/file/checkPendingUpload` | POST | 检查待恢复的上传（页面刷新后断点续传） |
| `/file/refreshPresignedUrls` | POST | 刷新分片预签名URL |

### 文件下载

| 接口 | 方法 | 说明 |
|------|------|------|
| `/file/getUserFile` | POST | 普通文件下载 |
| `/file/downloadBigFile` | POST | 大文件分片下载 |

### 文件管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/file/getUserFileList` | POST | 获取文件列表 |
| `/file/delOrResUserFile` | POST | 删除或恢复文件 |
| `/file/deleteFileByFileId` | POST | 彻底删除文件 |
| `/file/updateUserFilePath` | POST | 修改文件路径 |
| `/file/shareUserFile` | POST | 分享文件 |
| `/file/getShareFileUrl/{shareKey}` | GET | 获取分享文件 |

### 头像管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/account/getAvatarUploadUrl` | POST | 获取头像上传预签名URL |
| `/account/confirmAvatarUpload` | POST | 确认头像上传完成 |
| `/account/getAvatar` | POST | 获取头像 |

---

**文档版本**：v1.4  
**更新时间**：2026-09-21  
**更新内容**：移除不存在的“广场帖子”功能模块，重新编排章节序号  
**技术支持**：如有问题，请联系后端开发团队
