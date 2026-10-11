import request from '../utils/request'
import { BASE_API_URL } from '../config'

// ===== 帖子管理 =====

// 创建帖子，返回新建 postId。标量参数后端 @RequestParam 接收，走 query
export const createPost = (title, content) => {
  return request({
    url: '/forum/createPost',
    method: 'post',
    params: { title, content }
  })
}

// 发布帖子（草稿 0 → 已发布 1，仅作者可操作）
export const publishPost = (postId) => {
  return request({
    url: '/forum/publishPost',
    method: 'post',
    params: { postId }
  })
}

// 更新帖子状态（双向切换：0=草稿，1=发布），仅作者可操作
export const updatePostStatus = (postId, postStatus) => {
  return request({
    url: '/forum/updatePostStatus',
    method: 'post',
    params: { postId, postStatus }
  })
}

// 分页获取已发布帖子列表（keyword 可选：为空查全部，有值按标题模糊搜索；按更新时间倒序，含评论数）
export const getPostList = (keyword, pageNo = 1, pageSize = 20) => {
  return request({
    url: '/forum/getPostList',
    method: 'post',
    params: { keyword: keyword || undefined, pageNo, pageSize }
  })
}

// 获取我的帖子列表（含草稿和已发布，需登录，按更新时间倒序）
export const getMyPostList = (keyword, pageNo = 1, pageSize = 20) => {
  return request({
    url: '/forum/getMyPostList',
    method: 'post',
    params: { keyword: keyword || undefined, pageNo, pageSize }
  })
}

// 获取帖子详情（含 OSS 上的 MD 正文 + 评论数）
export const getPostDetail = (postId) => {
  return request({
    url: '/forum/getPostDetail',
    method: 'post',
    params: { postId }
  })
}

// 更新帖子标题和 MD 内容（仅作者）
export const updatePost = (postId, title, content) => {
  return request({
    url: '/forum/updatePost',
    method: 'post',
    params: { postId, title, content }
  })
}

// 删除帖子（仅作者，OSS MD 文件与评论由后端级联清理）
export const deletePost = (postId) => {
  return request({
    url: '/forum/deletePost',
    method: 'post',
    params: { postId }
  })
}

// 浏览量 +1（进入详情页时调用）
export const incrementViewCount = (postId) => {
  return request({
    url: '/forum/incrementViewCount',
    method: 'post',
    params: { postId }
  })
}

// 获取热门帖子列表（浏览量 Top10，Redis 缓存 1 小时）
export const getHotPostList = () => {
  return request({
    url: '/forum/getHotPostList',
    method: 'post'
  })
}

// ===== 封面与图片管理 =====

// 获取封面上传预签名 URL（前端直传 OSS，固定 PNG 格式，1 小时有效）
export const getCoverUploadUrl = (postId) => {
  return request({
    url: '/forum/getCoverUploadUrl',
    method: 'post',
    params: { postId }
  })
}

// 获取封面访问 URL（预签名，1 小时有效；封面不存在返回 null）
export const getCoverUrl = (postId) => {
  return request({
    url: '/forum/getCoverUrl',
    method: 'post',
    params: { postId }
  })
}

// 上传帖子内图片，返回 { uploadUrl, proxyUrl }；proxyUrl 永久有效，可嵌入 Markdown
export const uploadPostImage = (postId, fileName, contentType) => {
  return request({
    url: '/forum/uploadPostImage',
    method: 'post',
    params: { postId, fileName, contentType }
  })
}

// 构造帖子图片代理访问 URL（浏览器直接访问，302 重定向到 OSS 预签名）
// encryptedKey 由 uploadPostImage 返回的 proxyUrl 中解析得到，通常直接使用 proxyUrl 即可
export const getForumImageUrl = (encryptedKey) => {
  return `${BASE_API_URL}/forum/getForumImage/${encryptedKey}`
}

// ===== 评论管理 =====

// 发表评论（限 500 字）
export const addComment = (postId, content) => {
  return request({
    url: '/forum/addComment',
    method: 'post',
    params: { postId, content }
  })
}

// 获取评论列表（按发布时间升序）
export const getCommentList = (postId) => {
  return request({
    url: '/forum/getCommentList',
    method: 'post',
    params: { postId }
  })
}

// 删除评论（仅评论作者）
export const deleteComment = (commentId) => {
  return request({
    url: '/forum/deleteComment',
    method: 'post',
    params: { commentId }
  })
}
