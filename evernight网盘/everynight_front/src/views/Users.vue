<template>
  <div class="users">
    <h1>个人中心</h1>
    <div class="users-container">
      <div class="users-card">
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img v-if="avatarUrl" :src="avatarUrl" alt="头像" class="avatar" />
            <div v-else class="avatar-placeholder">
              <span>暂无头像</span>
            </div>
          </div>
          <div class="avatar-actions">
            <input 
              type="file" 
              ref="fileInput" 
              accept="image/*" 
              style="display: none" 
              @change="handleFileChange"
            />
            <button @click="triggerFileUpload" class="upload-btn">上传头像</button>
          </div>
        </div>

        <!-- <h2>个人信息</h2> -->
        <div class="info-section">
          <!-- <div class="info-item">
            <span class="info-label">用户ID</span>
            <span class="info-value">{{ userInfo.userId ?? '—' }}</span>
          </div> -->
          <div class="info-item">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ userInfo.username ?? '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">邮箱</span>
            <span class="info-value">{{ userInfo.email ?? '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">用户状态</span>
            <span class="info-value" :class="statusClass">{{ statusText }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">登录IP</span>
            <span class="info-value">{{ userInfo.userIp ?? '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Session过期</span>
            <span class="info-value">{{ expireText }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">剩余登录有效时间</span>
            <span class="info-value">{{ remainText }}</span>
          </div>
        </div>

        <h2>修改个人信息</h2>

        <form @submit.prevent="handleUpdateAccount" class="account-form">
          <div class="form-group">
            <label>用户名</label>
            <input 
              v-model="form.username" 
              type="text" 
              placeholder="请输入用户名"
              maxlength="30"
            />
          </div>

          <div class="form-group">
            <label>密码</label>
            <input 
              v-model="form.password" 
              type="password" 
              placeholder="请输入新密码（留空则不修改）"
              maxlength="42"
            />
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '保存中...' : '保存修改' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { updateAccount, uploadAvatar, getAvatar, getUserInfo, logout } from '../api/auth'
import { removeToken, AVATAR_STORAGE_KEY, USER_SESSION_CACHE_KEY } from '../utils/auth'

const router = useRouter()
const loading = ref(false)
const avatarUrl = ref('')
const fileInput = ref(null)
const userInfo = ref({})
// 每秒刷新一次，让"剩余有效时间"基于当前时刻实时计算
const now = ref(Date.now())
let timer = null
// 防止 session 过期时重复触发登出
let hasTriggeredLogout = false

const form = reactive({
  username: '',
  password: ''
})

// 头像与会话信息缓存 key 统一从 auth.js 导入，避免多处硬编码不一致

// 用户状态映射：0=管理员，1=禁用，2=普通用户
const statusText = computed(() => {
  switch (userInfo.value.userStatus) {
    case 0: return '管理员'
    case 1: return '已禁用'
    case 2: return '普通用户'
    default: return '—'
  }
})

const statusClass = computed(() => {
  switch (userInfo.value.userStatus) {
    case 0: return 'status-admin'
    case 1: return 'status-banned'
    case 2: return 'status-normal'
    default: return ''
  }
})

// expire 已归一化为毫秒级绝对过期时间戳，格式化为本地日期字符串
const expireText = computed(() => {
  const ts = userInfo.value.expire
  if (!ts) return '—'
  const d = new Date(Number(ts))
  if (isNaN(d.getTime())) return '—'
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
})

// 剩余时间：以 expire 减去当前时间实时计算，不再用后端的 remainSeconds 快照
const remainText = computed(() => {
  const ts = userInfo.value.expire
  if (!ts) return '—'
  const total = Math.floor((Number(ts) - now.value) / 1000)
  if (isNaN(total)) return '—'
  if (total <= 0) return '已过期'
  const day = Math.floor(total / 86400)
  const hour = Math.floor((total % 86400) / 3600)
  const min = Math.floor((total % 3600) / 60)
  const sec = Math.floor(total % 60)
  const parts = []
  if (day > 0) parts.push(`${day}天`)
  if (hour > 0 || day > 0) parts.push(`${hour}时`)
  if (min > 0 || hour > 0 || day > 0) parts.push(`${min}分`)
  parts.push(`${sec}秒`)
  return parts.join('')
})

// 是否已过期（基于 expire 与当前时间比较）
const isExpired = computed(() => {
  const ts = userInfo.value.expire
  if (!ts) return false
  return Number(ts) <= now.value
})

// 将后端 expire 归一化为毫秒级绝对过期时间戳：
// - 当前后端返回毫秒级绝对时间戳（如 1790074909983，约 1.7e12）
// - 防御性兼容秒级绝对时间戳（约 1.7e9）与历史版本的"剩余秒数"（如 86400）
const normalizeExpireMs = (raw) => {
  const n = Number(raw)
  if (!n || isNaN(n) || n <= 0) return 0
  if (n >= 1e11) return n // 已是毫秒级绝对时间戳
  if (n >= 1e9) return n * 1000 // 秒级绝对时间戳（2001 年之后）
  return Date.now() + n * 1000 // 兼容旧版剩余秒数：拉取时刻 + 时长
}

// 读取缓存的用户会话信息：缓存存在且未过期才返回，否则返回 null
const loadCachedUserInfo = () => {
  try {
    const cached = localStorage.getItem(USER_SESSION_CACHE_KEY)
    if (!cached) return null
    const data = JSON.parse(cached)
    // 校验缓存有效性：必须有 expire 且尚未过期
    if (!data || !data.expire || Number(data.expire) <= Date.now()) {
      localStorage.removeItem(USER_SESSION_CACHE_KEY)
      return null
    }
    return data
  } catch (error) {
    console.error('解析用户会话缓存失败:', error)
    localStorage.removeItem(USER_SESSION_CACHE_KEY)
    return null
  }
}

const fetchUserInfo = async () => {
  // 1. 优先读本地缓存，命中且有效则不访问服务器（缓存中的 expire 已是毫秒级绝对时间戳）
  const cached = loadCachedUserInfo()
  if (cached) {
    userInfo.value = cached
    return
  }
  // 2. 缓存缺失或已过期，访问服务器
  try {
    const data = await getUserInfo()
    if (data) {
      // 统一归一化为毫秒级绝对过期时间戳后再展示/缓存
      data.expire = normalizeExpireMs(data.expire)
      userInfo.value = data
      localStorage.setItem(USER_SESSION_CACHE_KEY, JSON.stringify(data))
    }
  } catch (error) {
    console.error('获取用户信息失败:', error.message)
  }
}

// session 过期自动退出登录
const handleSessionExpired = async () => {
  if (hasTriggeredLogout) return
  hasTriggeredLogout = true
  try {
    await logout()
  } catch (error) {
    console.error('登出失败:', error.message)
  } finally {
    // removeToken 已统一清除 token / userInfo / 头像 / 会话信息全部缓存
    removeToken()
    router.push('/login')
  }
}

// 监听 isExpired，从 false 变为 true 时触发自动登出
watch(isExpired, (expired) => {
  if (expired) {
    handleSessionExpired()
  }
})

const fetchAvatar = async () => {
  try {
    const cachedAvatar = localStorage.getItem(AVATAR_STORAGE_KEY)
    if (cachedAvatar) {
      avatarUrl.value = cachedAvatar
      return
    }

    const url = await getAvatar()
    if (url) {
      const response = await fetch(url)
      const blob = await response.blob()
      const reader = new FileReader()
      reader.onloadend = () => {
        const base64data = reader.result
        avatarUrl.value = base64data
        localStorage.setItem(AVATAR_STORAGE_KEY, base64data)
      }
      reader.readAsDataURL(blob)
    }
  } catch (error) {
    console.error('获取头像失败:', error.message)
  }
}

const triggerFileUpload = () => {
  fileInput.value?.click()
}

const handleFileChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    alert('图片大小不能超过5MB')
    return
  }

  loading.value = true
  try {
    const url = await uploadAvatar(file)
    
    const response = await fetch(url)
    const blob = await response.blob()
    const reader = new FileReader()
    reader.onloadend = () => {
      const base64data = reader.result
      avatarUrl.value = base64data
      localStorage.setItem(AVATAR_STORAGE_KEY, base64data)
    }
    reader.readAsDataURL(blob)
    
    alert('头像上传成功')
  } catch (error) {
    alert(error.message || '头像上传失败')
  } finally {
    loading.value = false
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

const handleUpdateAccount = async () => {
  if (!form.username && !form.password) {
    alert('请输入要修改的用户名或密码')
    return
  }

  if (form.username && (form.username.length < 2 || form.username.length > 30)) {
    alert('用户名长度应为2-30个字符')
    return
  }

  if (form.password && (form.password.length < 6 || form.password.length > 42)) {
    alert('密码长度应为6-42个字符')
    return
  }

  loading.value = true
  try {
    const params = {}
    if (form.username) params.username = form.username
    if (form.password) params.password = form.password

    await updateAccount(params)
    alert('账户信息修改成功，请重新登录')
    // removeToken 已统一清除 token / userInfo / 头像 / 会话信息全部缓存
    removeToken()
    window.location.href = '/login'
  } catch (error) {
    alert(error.message || '账户信息修改失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
  fetchAvatar()
  // 每秒更新当前时间，驱动 remainText 倒计时
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.users {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.users h1 {
  text-align: center;
  margin-bottom: 40px;
  color: #333;
}

.users-container {
  display: flex;
  justify-content: center;
}

.users-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.8), rgba(255, 182, 193, 0.8));
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  padding: 40px;
  color: #fff;
  text-align: center;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.2);
  max-width: 500px;
  width: 100%;
}

.avatar-section {
  margin-bottom: 30px;
}

.avatar-wrapper {
  width: 120px;
  height: 120px;
  margin: 0 auto 20px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.2);
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.avatar-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.upload-btn {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.upload-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.users-card h2 {
  font-size: 24px;
  margin-bottom: 10px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.users-card p {
  font-size: 16px;
  opacity: 0.9;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.account-form {
  text-align: left;
}

.info-section {
  text-align: left;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 10px;
  padding: 18px 20px;
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.2);
  font-size: 14px;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: rgba(255, 255, 255, 0.75);
  flex-shrink: 0;
}

.info-value {
  color: #fff;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.status-admin {
  color: #ffd700;
}

.status-banned {
  color: #ff6b6b;
}

.status-normal {
  color: #81c784;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.form-group input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.form-group input:focus {
  border-color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.15);
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .users {
    padding: 20px 15px;
  }
  
  .users h1 {
    font-size: 24px;
    margin-bottom: 30px;
  }
  
  .users-card {
    padding: 30px;
  }
  
  .users-card h2 {
    font-size: 20px;
  }
  
  .avatar-wrapper {
    width: 100px;
    height: 100px;
  }
}
</style>