import request from '../utils/request'

// 将 List 参数序列化为重复 query 键（如 uploadedParts=1&uploadedParts=2），
// 匹配后端 @RequestParam List<Integer>（axios 默认的 key[]= 后端收不到）
const repeatParamsSerializer = (listKey) => ({
  serialize: (params) => {
    const search = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (key === listKey) {
        ;(value || []).forEach(item => search.append(listKey, item))
      } else {
        search.append(key, value)
      }
    })
    return search.toString()
  }
})

// 获取普通文件上传预签名URL（前端 PUT 直传 OSS）。标量参数走 query
export const getUploadUrl = (fileName, fileSize, contentType, path) => {
  return request({
    url: '/file/getUploadUrl',
    method: 'post',
    params: { fileName, fileSize, contentType, path }
  })
}

// 确认普通文件上传完成（OSS 直传后调用）。标量参数走 query
export const confirmUpload = (ossKey, fileName, fileSize, contentType, path) => {
  return request({
    url: '/file/confirmUpload',
    method: 'post',
    params: { ossKey, fileName, fileSize, contentType, path }
  })
}

// 大文件预上传，返回 MultipartUploadInitDto：
// { uploadId, chunkSize, totalChunks, presignedUrls }。注意：后端用 @RequestParam 接收，参数走 query
export const preUploadFile = (path, fileName, fileSize) => {
  return request({
    url: '/file/preUploadFile',
    method: 'post',
    params: { path, fileName, fileSize }
  })
}

// 上报已直传 OSS 的分片编号（从 1 开始）。uploadedParts 为 List<Integer>，
// 用重复 query 键（uploadedParts=1&uploadedParts=2）匹配后端 @RequestParam List<Integer>
export const reportUploadedParts = (uploadId, uploadedParts) => {
  return request({
    url: '/file/reportUploadedParts',
    method: 'post',
    params: { uploadId, uploadedParts },
    paramsSerializer: repeatParamsSerializer('uploadedParts')
  })
}

// 刷新指定分片的预签名上传 URL（URL 1 小时过期后无需重新 preUploadFile，
// 保留原 uploadId 与已传分片进度）。partNumbers 为 List<Integer>（从 1 开始），
// 返回 data.presignedUrls 与 partNumbers 顺序一一对应
export const refreshPresignedUrls = (uploadId, partNumbers) => {
  return request({
    url: '/file/refreshPresignedUrls',
    method: 'post',
    params: { uploadId, partNumbers },
    paramsSerializer: repeatParamsSerializer('partNumbers')
  })
}

// 合并文件。后端用 @RequestParam 接收 uploadId，走 query
export const mergeFile = (uploadId) => {
  return request({
    url: '/file/mergeFile',
    method: 'post',
    params: { uploadId }
  })
}

// 查询上传状态。后端用 @RequestParam 接收 uploadId，走 query
export const getUploadStatus = (uploadId) => {
  return request({
    url: '/file/getUploadStatus',
    method: 'post',
    params: { uploadId }
  })
}

// 检查待恢复的上传（页面刷新后断点续传）。
// 后端通过 Redis 的 ossKey → uploadId 索引反查，返回 ChunkStatusDto 或 null。
// 前端无需依赖 localStorage 保存 uploadId。标量参数走 query
export const checkPendingUpload = (path, fileName) => {
  return request({
    url: '/file/checkPendingUpload',
    method: 'post',
    params: { path, fileName }
  })
}

// 获取用户文件列表（无参）
export const getUserFileList = () => {
  return request({
    url: '/file/getUserFileList',
    method: 'post'
  })
}

// 普通文件下载信息。后端用 @RequestParam 接收 fileId，走 query
export const getUserFile = (fileId) => {
  return request({
    url: '/file/getUserFile',
    method: 'post',
    params: { fileId }
  })
}

// 大文件分片下载信息。后端用 @RequestParam 接收 fileId，走 query
export const downloadBigFile = (fileId) => {
  return request({
    url: '/file/downloadBigFile',
    method: 'post',
    params: { fileId }
  })
}

// 删除或恢复文件（移入回收站/从回收站恢复）。后端用 @RequestParam，走 query
export const deleteUserFile = (fileId) => {
  return request({
    url: '/file/delOrResUserFile',
    method: 'post',
    params: { fileId }
  })
}

export const recoverUserFile = (fileId) => {
  return request({
    url: '/file/delOrResUserFile',
    method: 'post',
    params: { fileId }
  })
}

// 彻底删除文件。后端用 @RequestParam，走 query
export const deleteFileByFileId = (fileId) => {
  return request({
    url: '/file/deleteFileByFileId',
    method: 'post',
    params: { fileId }
  })
}

// 修改文件路径。后端用 @RequestParam，走 query
export const updateUserFilePath = (fileId, path) => {
  return request({
    url: '/file/updateUserFilePath',
    method: 'post',
    params: { fileId, path }
  })
}

// 分享文件。后端用 @RequestParam，走 query
export const shareUserFile = (fileId, limitSecond, limitCount) => {
  return request({
    url: '/file/shareUserFile',
    method: 'post',
    params: { fileId, limitSecond, limitCount }
  })
}

// 获取分享文件（GET + 路径参数）
export const getShareFileUrl = (shareKey) => {
  return request({
    url: `/file/getShareFileUrl/${shareKey}`,
    method: 'get'
  })
}

// 管理端：获取文件总大小
export const getTotalFileSize = () => {
  return request({
    url: '/admin/file/totalFileSize',
    method: 'get'
  })
}
