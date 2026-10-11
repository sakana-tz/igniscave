import { getCaptcha } from '../api/auth'

export default {
  name: 'Home',
  setup() {
    const fetchCaptcha = async () => {
      try {
        const data = await getCaptcha()
        console.log('验证码数据:', data)
        console.log('验证码Key:', data.captchaKey)
        console.log('验证码图片:', data.captchaImage)
      } catch (error) {
        console.error('获取验证码失败:', error.message)
      }
    }

    return {
      fetchCaptcha
    }
  }
}
