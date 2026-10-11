import request from '../utils/request'

// 管理员模块所有接口均为 POST，标量参数后端用 @RequestParam 接收，
// 前端统一走 axios `params`（URL query）。

// 兼容后端 Jackson default typing：将 ["java.util.ArrayList", [...]]、
// ["java.lang.Long", 123]、["com.xxx.Dto", {...}] 等类型包装递归解包为实际值。
// 仅当数组长度为 2、第一个元素是含 "." 的 Java 类全名字符串时才视为包装，
// 避免误伤普通业务二元数组。
const unwrapTyped = (value) => {
  if (Array.isArray(value)) {
    if (
      value.length === 2 &&
      typeof value[0] === 'string' &&
      value[0].includes('.') &&
      /^[a-zA-Z][\w.]*$/.test(value[0])
    ) {
      return unwrapTyped(value[1])
    }
    return value.map(unwrapTyped)
  }
  if (value !== null && typeof value === 'object') {
    const result = {}
    Object.keys(value).forEach(key => {
      result[key] = unwrapTyped(value[key])
    })
    return result
  }
  return value
}

// 统一对 request 返回的 data 做解包，避免每个调用点重复处理
const unwrap = (promise) => promise.then(data => unwrapTyped(data))

// ===== 系统仪表盘 =====

// 获取系统各模块核心统计：userCount / totalStorageSize / postCount / commentCount
// 注意：overview 不含文件总数，文件总数需另调 getFileTotalCount
export const getOverview = () => {
  return unwrap(request({
    url: '/admin/dashboard/getOverview',
    method: 'post'
  }))
}

// ===== 文件管理 =====

// 查询文件总数（仅统计未删除文件）。无参数，data 为 Long
export const getFileTotalCount = () => {
  return unwrap(request({
    url: '/admin/file/fileTotalCount',
    method: 'post'
  }))
}

// ===== 用户管理 =====

// 分页模糊查询用户列表（UserQuery）。标量参数走 query
export const getUserList = (query) => {
  return unwrap(request({
    url: '/admin/user/loadDataList',
    method: 'post',
    params: query
  }))
}

// 新增用户。User 对象走 JSON body
export const addUser = (user) => {
  return unwrap(request({
    url: '/admin/user/add',
    method: 'post',
    data: user
  }))
}

// 根据 UserId 查询用户。标量参数走 query
export const getUserByUserId = (userId) => {
  return unwrap(request({
    url: '/admin/user/getUserByUserId',
    method: 'post',
    params: { userId }
  }))
}

// 根据 UserId 修改用户。userId 走 query，User 对象走 JSON body（仅传需修改字段）
export const updateUserByUserId = (userId, user) => {
  return unwrap(request({
    url: '/admin/user/updateUserByUserId',
    method: 'post',
    params: { userId },
    data: user
  }))
}

// 根据 UserId 删除用户。标量参数走 query
export const deleteUserByUserId = (userId) => {
  return unwrap(request({
    url: '/admin/user/deleteUserByUserId',
    method: 'post',
    params: { userId }
  }))
}

// ===== 论坛管理 — 帖子 =====

// 分页查询帖子列表（ForumPostQuery，含发帖用户名和评论数）。标量参数走 query
export const getPostList = (query) => {
  return unwrap(request({
    url: '/admin/forum/post/loadDataList',
    method: 'post',
    params: query
  }))
}

// 根据 PostId 查询帖子详情（含 OSS 上的 MD 正文）。标量参数走 query
export const getPostByPostId = (postId) => {
  return unwrap(request({
    url: '/admin/forum/post/getPostByPostId',
    method: 'post',
    params: { postId }
  }))
}

// 根据 PostId 删除帖子（同时清理 OSS MD 与评论）。标量参数走 query
export const deletePostByPostId = (postId) => {
  return unwrap(request({
    url: '/admin/forum/post/deletePostByPostId',
    method: 'post',
    params: { postId }
  }))
}

// ===== 论坛管理 — 评论 =====

// 分页查询评论列表（ForumCommentQuery，含评论用户名）。标量参数走 query
export const getCommentList = (query) => {
  return unwrap(request({
    url: '/admin/forum/comment/loadDataList',
    method: 'post',
    params: query
  }))
}

// 根据 CommentId 删除评论（不限用户归属）。标量参数走 query
export const deleteCommentByCommentId = (commentId) => {
  return unwrap(request({
    url: '/admin/forum/comment/deleteCommentByCommentId',
    method: 'post',
    params: { commentId }
  }))
}
