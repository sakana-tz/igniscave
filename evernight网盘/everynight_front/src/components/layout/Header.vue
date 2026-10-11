<template>
  <header class="header">
    <div class="header-container">
      <div class="logo">
        <router-link to="/">
          <img src="/vite.svg" alt="Logo" class="logo-img" />
          <span class="logo-text">Evernight网盘</span>
        </router-link>
      </div>

      <nav class="nav-links" :class="{ 'nav-open': isMobileMenuOpen }">
        <router-link to="/" @click="closeMobileMenu">首页</router-link>
        <router-link to="/share" @click="closeMobileMenu">获取分享文件</router-link>
        <router-link to="/square" @click="closeMobileMenu">奥赫玛浴场</router-link>
        <router-link to="/files" @click="closeMobileMenu">文件管理</router-link>
        <router-link to="/users" @click="closeMobileMenu">个人中心</router-link>
        
        <template v-if="isLoggedIn">
          <router-link v-if="isAdminUser" to="/admin" @click="closeMobileMenu">管理员</router-link>
          <span class="nav-item" @click="handleLogout">登出</span>
        </template>
        <template v-else>
          <router-link to="/login" @click="closeMobileMenu">登录</router-link>
          <router-link to="/register" @click="closeMobileMenu">注册</router-link>
          <!-- <router-link to="/about" @click="closeMobileMenu">关于</router-link> -->

        </template>
      </nav>

      <button 
        class="mobile-menu-btn" 
        @click="toggleMobileMenu"
        :class="{ 'menu-open': isMobileMenuOpen }"
      >
        <span></span>
        <span></span>
        <span></span>
      </button>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import { isAuthenticated, removeToken, isAdmin } from '../../utils/auth'
import { logout } from '../../api/auth'

const router = useRouter()
const isMobileMenuOpen = ref(false)
const isLoggedIn = ref(isAuthenticated())
const isAdminUser = ref(isAdmin())

const updateLoginStatus = () => {
  isLoggedIn.value = isAuthenticated()
  isAdminUser.value = isAdmin()
}

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false
}

const handleLogout = async () => {
  try {
    await logout()
  } catch (error) {
    console.error('登出失败:', error.message)
  } finally {
    removeToken()
    updateLoginStatus()
    router.push('/login')
  }
}

const handleResize = () => {
  if (window.innerWidth > 768) {
    isMobileMenuOpen.value = false
  }
}

// 每次路由变化时检查登录状态
const checkLoginStatus = () => {
  updateLoginStatus()
}

// 在组件挂载时添加路由守卫
let routeGuard

onMounted(() => {
  window.addEventListener('resize', handleResize)
  
  // 添加路由后置守卫，每次路由变化后检查登录状态
  routeGuard = router.afterEach(() => {
    checkLoginStatus()
  })
  
  // 初始检查
  checkLoginStatus()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  // 移除路由守卫
  if (routeGuard) {
    routeGuard()
  }
})
</script>

<style scoped>
.header {
  background: linear-gradient(135deg, #667eea 0%, #ffb6c1 50%, #8B0000 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
}

.logo a {
  display: flex;
  align-items: center;
  text-decoration: none;
  gap: 10px;
}

.logo-img {
  width: 32px;
  height: 32px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-links a,
.nav-links .nav-item {
  text-decoration: none;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 24px;
  border-radius: 4px;
  transition: all 0.3s;
  display: block;
  min-width: 120px;
  text-align: center;
}

.nav-links a:hover,
.nav-links .nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
}

.nav-links a.router-link-exact-active {
  color: #fff;
  font-weight: bold;
  background: rgba(255, 255, 255, 0.3);
}

.mobile-menu-btn {
  display: none;
  flex-direction: column;
  justify-content: space-around;
  width: 30px;
  height: 24px;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  outline: none;
}

.mobile-menu-btn span {
  width: 100%;
  height: 3px;
  background: #333;
  transition: all 0.3s;
  border-radius: 2px;
}

.mobile-menu-btn.menu-open span:nth-child(1) {
  transform: rotate(45deg) translate(6px, 6px);
}

.mobile-menu-btn.menu-open span:nth-child(2) {
  opacity: 0;
}

.mobile-menu-btn.menu-open span:nth-child(3) {
  transform: rotate(-45deg) translate(6px, -6px);
}

@media (max-width: 768px) {
  .mobile-menu-btn {
    display: flex;
  }

  .nav-links {
    position: fixed;
    top: 60px;
    left: 0;
    right: 0;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.95), rgba(255, 182, 193, 0.95), rgba(139, 0, 0, 0.95));
    backdrop-filter: blur(10px);
    flex-direction: column;
    padding: 20px;
    gap: 0;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
    transform: translateY(-100%);
    opacity: 0;
    transition: all 0.3s;
    pointer-events: none;
    z-index: 1000;
  }

  .nav-links.nav-open {
    transform: translateY(0);
    opacity: 1;
    pointer-events: auto;
  }

  .nav-links a,
  .nav-links .nav-item {
    padding: 15px 12px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    color: #fff;
  }

  .nav-links a:hover,
  .nav-links .nav-item:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  .nav-links a.router-link-exact-active {
    background: rgba(255, 255, 255, 0.3);
  }

  .nav-links a:last-child,
  .nav-links .nav-item:last-child {
    border-bottom: none;
  }

  .mobile-menu-btn span {
    background: #fff;
  }
}
</style>
