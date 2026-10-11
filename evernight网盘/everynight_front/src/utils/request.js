import axios from 'axios'
import { handleResponse } from './response'
import { getToken, removeToken, setToken } from './auth'
import { BASE_API_URL } from '../config'

const service = axios.create({
  baseURL: BASE_API_URL,
  timeout: 15000,
  withCredentials: true
})

service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    
    if (res.status === 'success' && res.code === 200) {
      if (response.config.url.includes('/account/login') && res.data) {
        setToken(res.data)
      }
      return handleResponse(res)
    } else {
      const message = res.info || '请求失败'
      console.error('API Error:', message)
      return Promise.reject(new Error(message))
    }
  },
  error => {
    let message = '网络错误'
    
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请登录'
          removeToken()
          window.location.href = '/login'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = error.response.data?.info || '请求失败'
      }
    } else if (error.request) {
      message = '网络连接失败'
    } else {
      message = error.message
    }
    
    console.error('Request Error:', message)
    return Promise.reject(new Error(message))
  }
)

export default service
