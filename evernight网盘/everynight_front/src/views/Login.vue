<template>
  <AuthContainer title="用户登录">
    <div class="tab-buttons">
      <button 
        :class="{ active: loginType === 'password' }" 
        @click="loginType = 'password'"
      >
        密码登录
      </button>
      <button 
        :class="{ active: loginType === 'email' }" 
        @click="loginType = 'email'"
      >
        邮箱验证码登录
      </button>
    </div>

    <form @submit.prevent="handleLogin">
      <FormInput
        v-model="form.email"
        label="邮箱"
        type="email"
        placeholder="请输入邮箱"
        required
      />

      <FormInput
        v-if="loginType === 'password'"
        v-model="form.password"
        label="密码"
        type="password"
        placeholder="请输入密码"
        required
      />

      <ImageCaptchaInput
        v-if="loginType === 'password'"
        v-model="form.checkCode"
        label="验证码"
        :captcha-image="captchaImage"
        placeholder="请输入验证码"
        required
        @refresh="fetchCaptcha"
      />

      <EmailCaptchaInput
        v-if="loginType === 'email'"
        ref="emailCaptchaRef"
        v-model="form.emailCheckCode"
        label="邮箱验证码"
        placeholder="请输入邮箱验证码"
        :disabled="emailCodeLoading"
        required
        @send-code="sendEmailCaptcha"
      />

      <SubmitButton
        :loading="loading"
        button-text="登录"
        loading-text="登录中..."
      />
    </form>

    <template #links>
      <router-link to="/register">注册账号</router-link>
    </template>
  </AuthContainer>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCaptcha, getEmailCaptcha, login } from '../api/auth'
import AuthContainer from '../components/common/AuthContainer.vue'
import FormInput from '../components/common/FormInput.vue'
import ImageCaptchaInput from '../components/common/ImageCaptchaInput.vue'
import EmailCaptchaInput from '../components/common/EmailCaptchaInput.vue'
import SubmitButton from '../components/common/SubmitButton.vue'

const router = useRouter()
const loginType = ref('password')
const loading = ref(false)
const emailCodeLoading = ref(false)
const captchaKey = ref('')
const captchaImage = ref('')
const emailCaptchaRef = ref(null)

const form = reactive({
  email: '',
  password: '',
  checkCode: '',
  checkCodeKey: '',
  emailCheckCode: ''
})

const fetchCaptcha = async () => {
  try {
    const data = await getCaptcha()
    captchaKey.value = data.captchaKey
    captchaImage.value = data.captchaImage
    form.checkCodeKey = data.captchaKey
  } catch (error) {
    console.error('获取验证码失败:', error.message)
  }
}

const sendEmailCaptcha = async () => {
  if (!form.email) {
    alert('请先输入邮箱')
    return
  }

  emailCodeLoading.value = true
  try {
    await getEmailCaptcha(form.email)
  } catch (error) {
    alert(error.message)
  } finally {
    emailCodeLoading.value = false
  }
}

const handleLogin = async () => {
  loading.value = true
  try {
    const params = {
      email: form.email
    }

    if (loginType.value === 'password') {
      params.password = form.password
      params.checkCodeKey = form.checkCodeKey
      params.checkCode = form.checkCode
    } else {
      params.emailCheckCode = form.emailCheckCode
    }

    await login(params)
    alert('登录成功')
    router.push('/')
  } catch (error) {
    alert(error.message)
    if (loginType.value === 'password') {
      fetchCaptcha()
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCaptcha()
})
</script>

<style scoped>
.tab-buttons {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.tab-buttons button {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.8);
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.tab-buttons button.active {
  color: #000;
  border-bottom-color: #000;
  font-weight: 500;
}
</style>
