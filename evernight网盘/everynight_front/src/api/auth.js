import request from '../utils/request'

export const getCaptcha = () => {
  return request({
    url: '/account/getCaptcha',
    method: 'get'
  })
}

export const getEmailCaptcha = (email) => {
  return request({
    url: '/account/getEmailCaptcha',
    method: 'get',
    params: { email }
  })
}

export const login = (params) => {
  return request({
    url: '/account/login',
    method: 'post',
    params
  })
}

export const autoLogin = () => {
  return request({
    url: '/account/autoLogin',
    method: 'get'
  })
}

export const logout = () => {
  return request({
    url: '/account/logout',
    method: 'get'
  })
}

export const register = (params) => {
  return request({
    url: '/account/register',
    method: 'post',
    params
  })
}

export const updateAccount = (params) => {
  return request({
    url: '/account/updateAccount',
    method: 'post',
    params
  })
}

// 头像上传：获取预签名URL → PUT 直传 OSS → 确认上传，返回头像访问URL
export const uploadAvatar = async (file) => {
  const uploadUrl = await request({
    url: '/account/getAvatarUploadUrl',
    method: 'post'
  })

  const resp = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': 'image/png' },
    body: file
  })
  if (!resp.ok) {
    throw new Error(`头像上传失败 (HTTP ${resp.status})`)
  }

  const avatarUrl = await request({
    url: '/account/confirmAvatarUpload',
    method: 'post'
  })
  return avatarUrl
}

export const getAvatar = () => {
  return request({
    url: '/account/getAvatar',
    method: 'post'
  })
}

// 获取当前登录用户的基本信息（userId、username、email、userStatus、userIp、expire 毫秒级绝对过期时间戳）
export const getUserInfo = () => {
  return request({
    url: '/account/getUserInfo',
    method: 'post'
  })
}
