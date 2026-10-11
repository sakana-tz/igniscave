export class ApiResponse {
  constructor(response) {
    this.status = response.status || 'error'
    this.code = response.code || 500
    this.info = response.info || '未知错误'
    this.data = response.data || null
  }

  isSuccess() {
    return this.status === 'success' && this.code === 200
  }

  isFailure() {
    return !this.isSuccess()
  }

  getData() {
    return this.data
  }

  getMessage() {
    return this.info
  }

  getCode() {
    return this.code
  }
}

export const handleResponse = (response) => {
  const apiResponse = new ApiResponse(response)
  
  if (apiResponse.isFailure()) {
    throw new Error(apiResponse.getMessage())
  }
  
  return apiResponse.getData()
}

export const createSuccessResponse = (data, info = '请求成功', code = 200) => {
  return {
    status: 'success',
    code,
    info,
    data
  }
}

export const createErrorResponse = (info = '请求失败', code = 500, data = null) => {
  return {
    status: 'error',
    code,
    info,
    data
  }
}
