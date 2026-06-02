import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  config => {
    console.log(`🚀 [API请求] ${config.method.toUpperCase()} ${config.url}`)
    return config
  },
  error => {
    console.error('❌ [请求错误]', error)
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    console.log(`✅ [API响应] ${response.config.url}`, res)
    
    if (res.success === false) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
  },
  error => {
    console.error('❌ [响应错误]', error)
    
    let message = '网络错误'
    if (error.response) {
      switch (error.response.status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '未授权，请登录'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `错误 ${error.response.status}`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request

export const routeApi = {
  planRoute: (data) => request.post('/route/plan', data),
  
  getPOIByCategory: (category) => request.get(`/poi/category/${category}`),
  
  searchNearby: (params) => request.get('/poi/nearby', { params }),
  
  getHotPOIs: (limit = 10) => request.get(`/poi/hot?limit=${limit}`)
}

export const userApi = {
  getProfile: (userId) => request.get(`/user/${userId}/profile`),
  
  updateProfile: (userId, data) => request.put(`/user/${userId}/profile`, data),
  
  submitFeedback: (data) => request.post('/feedback', data)
}
