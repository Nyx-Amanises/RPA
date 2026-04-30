import axios from 'axios'
import { ElMessage } from 'element-plus'
import {
  clearPageForbidden,
  DEFAULT_PAGE_FORBIDDEN_MESSAGE,
  pageAccessState,
  setPageForbidden
} from '../stores/pageAccess'
import { clearAuthSession, getAuthToken } from '../utils/auth'

const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const SUCCESS_CODE = 100200
const REQUEST_TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT || 30000)
const PAGE_PERMISSION_SCOPE = 'page'
const ACTION_PERMISSION_SCOPE = 'action'
const LOGIN_EXPIRED_MESSAGE = '\u767b\u5f55\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55'
const PERMISSION_DENIED_MESSAGE = '\u6743\u9650\u4e0d\u8db3'
const REQUEST_FAILED_MESSAGE = '\u8bf7\u6c42\u5931\u8d25'
const BUSINESS_FAILED_MESSAGE = '\u4e1a\u52a1\u5904\u7406\u5931\u8d25'

let isHandlingUnauthorized = false

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: REQUEST_TIMEOUT
})

function createNormalizedError(message, extras = {}) {
  const error = new Error(message)
  Object.assign(error, extras)
  return error
}

function getPermissionScope(config = {}) {
  return config.permissionScope === PAGE_PERMISSION_SCOPE ? PAGE_PERMISSION_SCOPE : ACTION_PERMISSION_SCOPE
}

function getPagePath(config = {}) {
  return config.pagePath || window.location.pathname
}

function getErrorMessage(error, fallbackMessage) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.msg ||
    error?.businessMessage ||
    error?.message ||
    fallbackMessage
  )
}

function buildPageForbiddenResponse(message) {
  return {
    code: 403,
    message: message || DEFAULT_PAGE_FORBIDDEN_MESSAGE,
    data: null,
    __pageForbidden: true
  }
}

function normalizeError(error) {
  const permissionScope = error?.permissionScope || getPermissionScope(error?.config)
  const pagePath = error?.pagePath || getPagePath(error?.config)
  const baseMeta = {
    statusCode: error?.response?.status || error?.statusCode || 0,
    businessCode: error?.businessCode,
    permissionScope,
    pagePath,
    __pageForbidden: Boolean(error?.__pageForbidden),
    __handled: Boolean(error?.__handled)
  }

  if (error?.code === 'ECONNABORTED' || String(error?.message || '').includes('timeout')) {
    return createNormalizedError(`\u8bf7\u6c42\u8d85\u65f6\uff0c\u63a5\u53e3\u5728 ${REQUEST_TIMEOUT}ms \u5185\u672a\u8fd4\u56de`, baseMeta)
  }

  return createNormalizedError(getErrorMessage(error, REQUEST_FAILED_MESSAGE), baseMeta)
}

service.interceptors.request.use((config) => {
  const token = getAuthToken() || import.meta.env.VITE_DEV_TOKEN || ''
  const permissionScope = getPermissionScope(config)

  config.headers = config.headers || {}
  config.permissionScope = permissionScope
  config.pagePath = getPagePath(config)
  config.pageAccessVersion = config.pageAccessVersion ?? pageAccessState.version

  if (token) {
    config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  }

  return config
})

service.interceptors.response.use(
  (response) => response,
  (error) => {
    if (useMock) {
      return Promise.reject(error)
    }

    const statusCode = error?.response?.status

    if (statusCode === 401 && window.location.pathname !== '/login') {
      error.__handled = true
      error.message = LOGIN_EXPIRED_MESSAGE

      if (!isHandlingUnauthorized) {
        isHandlingUnauthorized = true
        ElMessage.error(LOGIN_EXPIRED_MESSAGE)
        clearAuthSession()
        clearPageForbidden()
        window.location.replace('/login')
      }
    }

    if (statusCode === 403) {
      const permissionScope = getPermissionScope(error?.config)
      const pagePath = getPagePath(error?.config)
      const message = getErrorMessage(
        error,
        permissionScope === PAGE_PERMISSION_SCOPE ? DEFAULT_PAGE_FORBIDDEN_MESSAGE : PERMISSION_DENIED_MESSAGE
      )

      error.permissionScope = permissionScope
      error.pagePath = pagePath
      error.message = message

      if (permissionScope === PAGE_PERMISSION_SCOPE) {
        error.__pageForbidden = true

        if (
          typeof error?.config?.pageAccessVersion === 'undefined' ||
          error.config.pageAccessVersion === pageAccessState.version
        ) {
          setPageForbidden(pagePath, message)
        }
      }
    }

    return Promise.reject(error)
  }
)

export async function request(config, fallback) {
  if (useMock && typeof fallback === 'function') {
    return fallback()
  }

  const requestConfig = {
    ...config,
    permissionScope: getPermissionScope(config),
    pagePath: getPagePath(config),
    pageAccessVersion: config?.pageAccessVersion ?? pageAccessState.version
  }

  try {
    const { data } = await service(requestConfig)

    if (typeof data?.code !== 'undefined' && data.code !== SUCCESS_CODE) {
      const businessMessage = data.message || BUSINESS_FAILED_MESSAGE
      throw createNormalizedError(businessMessage, {
        businessMessage,
        businessCode: data.code,
        response: { data },
        config: requestConfig
      })
    }

    return data
  } catch (error) {
    const normalizedError = normalizeError(error)

    if (normalizedError.__pageForbidden) {
      return buildPageForbiddenResponse(normalizedError.message)
    }

    throw normalizedError
  }
}

export default service
