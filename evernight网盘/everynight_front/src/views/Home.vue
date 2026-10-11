<template>
  <div class="home">
    <h1>首页</h1>
    <div class="feature-grid">
      <router-link to="/share" class="feature-card">
        <div class="feature-icon share-icon">
          <img src="/icons/share-icon.png" alt="分享" class="feature-img">
        </div>
        <h2>获取分享文件</h2>
        <p>快速获取好友分享的文件，支持链接分享</p>
      </router-link>
      
      <router-link to="/square" class="feature-card">
        <div class="feature-icon square-icon">
          <img src="/icons/square-icon.png" alt="奥赫玛浴场" class="feature-img">
        </div>
        <h2>奥赫玛浴场</h2>
        <p>发现更多精彩</p>
      </router-link>
      
      <router-link to="/files" class="feature-card">
        <div class="feature-icon files-icon">
          <img src="/icons/files-icon.png" alt="文件" class="feature-img">
        </div>
        <h2>文件管理</h2>
        <p>管理您的所有文件，支持上传、下载、删除等操作</p>
      </router-link>
      
      <router-link to="/users" class="feature-card">
        <div class="feature-icon users-icon">
          <img src="/icons/users-icon.png" alt="用户" class="feature-img">
        </div>
        <h2>个人中心</h2>
        <p>查看和管理您的个人信息、设置和账户安全</p>
      </router-link>
      
      <template v-if="isLoggedIn && isAdminUser">
        <router-link to="/admin" class="feature-card">
          <div class="feature-icon admin-icon">
            <img src="/icons/admin-icon.png" alt="管理员" class="feature-img">
          </div>
          <h2>管理员中心</h2>
          <p>一般来说只有管理员能看到</p>
        </router-link>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { isAuthenticated, isAdmin } from '../utils/auth'

const isLoggedIn = ref(isAuthenticated())
const isAdminUser = ref(isAdmin())

onMounted(() => {
  isLoggedIn.value = isAuthenticated()
  isAdminUser.value = isAdmin()
})
</script>

<style scoped>
.home {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.home h1 {
  text-align: center;
  margin-bottom: 40px;
  color: #333;
  font-size: 32px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  margin-top: 30px;
}

.feature-card {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.8), rgba(255, 182, 193, 0.8));
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
  padding: 30px;
  text-decoration: none;
  color: #fff;
  transition: all 0.3s;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(255, 255, 255, 0.2);
  text-align: center;
  aspect-ratio: 1 / 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.3);
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.9), rgba(255, 182, 193, 0.9));
}

.feature-icon {
  width: 150px;
  height: 150px;
  margin: 0 auto 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.feature-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.feature-card h2 {
  font-size: 20px;
  margin-bottom: 12px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.feature-card p {
  font-size: 14px;
  line-height: 1.5;
  opacity: 0.9;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

@media (max-width: 768px) {
  .home {
    padding: 20px 15px;
  }
  
  .home h1 {
    font-size: 24px;
    margin-bottom: 30px;
  }
  
  .feature-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .feature-card {
    padding: 25px;
  }
  
  .feature-icon {
    width: 100px;
    height: 100px;
    font-size: 24px;
  }
  
  .feature-card h2 {
    font-size: 18px;
  }
}
</style>
