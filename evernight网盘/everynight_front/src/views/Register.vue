<template>
  <AuthContainer title="用户注册">
    <form @submit.prevent="handleRegister">
      <FormInput
        v-model="form.username"
        label="用户名"
        type="text"
        placeholder="请输入用户名（2-30个字符）"
        required
        minlength="2"
        maxlength="30"
      />

      <FormInput
        v-model="form.email"
        label="邮箱"
        type="email"
        placeholder="请输入邮箱"
        required
      />

      <FormInput
        v-model="form.password"
        label="密码"
        type="password"
        placeholder="请输入密码（6-42个字符）"
        required
        minlength="6"
        maxlength="42"
      />

      <FormInput
        v-model="form.confirmPassword"
        label="确认密码"
        type="password"
        placeholder="请再次输入密码"
        required
      />

      <EmailCaptchaInput
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
        button-text="注册"
        loading-text="注册中..."
      />
    </form>

    <template #links>
      <router-link to="/login">已有账号？去登录</router-link>
    </template>
  </AuthContainer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { getEmailCaptcha, register } from '../api/auth'
import AuthContainer from '../components/common/AuthContainer.vue'
import FormInput from '../components/common/FormInput.vue'
import EmailCaptchaInput from '../components/common/EmailCaptchaInput.vue'
import SubmitButton from '../components/common/SubmitButton.vue'

const router = useRouter()
const loading = ref(false)
const emailCodeLoading = ref(false)
const emailCaptchaRef = ref(null)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  emailCheckCode: ''
})

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

const handleRegister = async () => {
  if (form.password !== form.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    await register({
      username: form.username,
      email: form.email,
      password: form.password,
      emailCheckCode: form.emailCheckCode
    })
    alert('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    alert(error.message)
  } finally {
    loading.value = false
  }
}
</script>
