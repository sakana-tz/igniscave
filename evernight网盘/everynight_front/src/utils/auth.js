const TOKEN_KEY = 'userTokenJwt'
const USER_INFO_KEY = 'userInfo'
// 头像 base64 缓存 key
export const AVATAR_STORAGE_KEY = 'user_avatar_base64'
// 用户会话信息缓存 key（含 expire/userIp 等完整字段）
export const USER_SESSION_CACHE_KEY = 'user_session_info'

// 解析 JWT token
export const parseToken = (token) => {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      window.atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (error) {
    console.error('解析 token 失败:', error)
    return null
  }
}

export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY)
}

export const setToken = (token) => {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
    // 解析并存储用户信息
    const userInfo = parseToken(token)
    if (userInfo) {
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
    }
  }
}

// 清除所有用户相关本地缓存：token、JWT 解析出的用户信息、头像 base64、会话信息缓存
export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)
  localStorage.removeItem(AVATAR_STORAGE_KEY)
  localStorage.removeItem(USER_SESSION_CACHE_KEY)
}

export const isAuthenticated = () => {
  return !!getToken()
}

export const getUserInfo = () => {
  const userInfoStr = localStorage.getItem(USER_INFO_KEY)
  return userInfoStr ? JSON.parse(userInfoStr) : null
}

export const getUserStatus = () => {
  const userInfo = getUserInfo()
  return userInfo ? userInfo.userStatus : null
}

export const getUserId = () => {
  const userInfo = getUserInfo()
  return userInfo ? userInfo.userId : null
}

export const getUsername = () => {
  const userInfo = getUserInfo()
  return userInfo ? userInfo.username : null
}

export const getEmail = () => {
  const userInfo = getUserInfo()
  return userInfo ? userInfo.email : null
}

export const isAdmin = () => {
  return getUserStatus() === 0
}

export const isNormalUser = () => {
  return getUserStatus() === 2
}

export const isBannedUser = () => {
  return getUserStatus() === 1
}
