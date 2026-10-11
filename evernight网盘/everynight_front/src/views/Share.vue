<template>
  <div class="share">
    <h1>获取分享文件</h1>
    <div class="share-container">
      <div class="share-card">
        <h2>输入分享密钥</h2>
        <p>输入好友分享的密钥，获取文件</p>
        
        <div class="form-group">
          <input 
            type="text" 
            v-model="shareKey" 
            placeholder="请输入分享密钥"
            class="form-input"
          />
        </div>
        
        <button 
          @click="handleGetShareFileUrl" 
          class="submit-button"
          :disabled="loading"
        >
          {{ loading ? '获取中...' : '获取文件' }}
        </button>
        
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        
        <div v-if="shareFileInfo" class="success-message">
          <h3>文件信息</h3>
          <div class="file-info">
            <p><strong>文件名：</strong>{{ shareFileInfo.fileName }}</p>
            <p><strong>剩余时间：</strong>{{ formatRemainTime(shareFileInfo.remainSeconds) }}</p>
            <p><strong>剩余下载次数：</strong>{{ shareFileInfo.shareLimitCount - shareFileInfo.shareCount }}</p>
          </div>
          <a :href="shareFileInfo.fileUrl" target="_blank" class="download-link">
            点击下载文件
          </a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getShareFileUrl } from '../api/file'

const route = useRoute()
const shareKey = ref('')
const loading = ref(false)
const error = ref('')
const shareFileInfo = ref(null)

onMounted(() => {
  // 支持通过分享链接 ?key=xxx 自动填充并获取文件
  const keyFromQuery = route.query.key
  if (keyFromQuery) {
    shareKey.value = keyFromQuery
    handleGetShareFileUrl()
  }
})

const handleGetShareFileUrl = async () => {
  if (!shareKey.value.trim()) {
    error.value = '请输入分享密钥'
    return
  }
  
  loading.value = true
  error.value = ''
  shareFileInfo.value = null
  
  try {
    const data = await getShareFileUrl(shareKey.value)
    shareFileInfo.value = data
  } catch (err) {
    error.value = err.message || '获取文件失败'
    console.error('获取分享文件失败:', err)
  } finally {
    loading.value = false
  }
}

const formatRemainTime = (seconds) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}分${remainingSeconds}秒`
}
</script>

<style scoped>
.share {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.share h1 {
  text-align: center;
  margin-bottom: 40px;
  color: #333;
}

.share-container {
  display: flex;
  justify-content: center;
}

.share-card {
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

.share-card h2 {
  font-size: 24px;
  margin-bottom: 20px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.share-card p {
  font-size: 16px;
  opacity: 0.9;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 25px;
  text-align: left;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  outline: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.3s ease;
}

.form-input:focus {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.5);
}

.submit-button {
  width: 100%;
  padding: 12px;
  background-color: rgba(255, 255, 255, 0.9);
  color: #667eea;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 20px;
}

.submit-button:hover:not(:disabled) {
  background-color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.submit-button:disabled {
  background-color: rgba(255, 255, 255, 0.6);
  cursor: not-allowed;
  transform: none;
}

.error-message {
  background-color: rgba(255, 70, 70, 0.2);
  border: 1px solid rgba(255, 70, 70, 0.3);
  border-radius: 8px;
  padding: 12px;
  margin-top: 20px;
  color: #fff;
  font-size: 14px;
}

.success-message {
  background-color: rgba(70, 255, 120, 0.2);
  border: 1px solid rgba(70, 255, 120, 0.3);
  border-radius: 8px;
  padding: 20px;
  margin-top: 20px;
  color: #fff;
}

.success-message h3 {
  font-size: 18px;
  margin-bottom: 16px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.file-info {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.file-info p {
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.4;
}

.file-info p:last-child {
  margin-bottom: 0;
}

.download-link {
  display: inline-block;
  padding: 10px 20px;
  background-color: rgba(255, 255, 255, 0.9);
  color: #667eea;
  border-radius: 6px;
  text-decoration: none;
  font-weight: bold;
  transition: all 0.3s ease;
}

.download-link:hover {
  background-color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

@media (max-width: 768px) {
  .share {
    padding: 20px 15px;
  }
  
  .share h1 {
    font-size: 24px;
    margin-bottom: 30px;
  }
  
  .share-card {
    padding: 30px;
  }
  
  .share-card h2 {
    font-size: 20px;
  }
  
  .form-input {
    padding: 10px 14px;
    font-size: 14px;
  }
  
  .submit-button {
    padding: 10px;
    font-size: 14px;
  }
}
</style>