import { defineStore } from 'pinia'
import { fetchCurrentUser, login } from '../api/auth'
import { clearAuthSession, getAuthToken, getAuthUser, setAuthToken, setAuthUser } from '../utils/auth'
import {
  getAllPermissions,
  getButtonPermissions,
  getMenuPermissions,
  hasAnyPermission,
  hasPermission
} from '../utils/permission'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getAuthToken(),
    user: getAuthUser(),
    initialized: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) => state.user?.realName || state.user?.username || '未登录',
    permissions: (state) => getAllPermissions(state.user),
    menuPermissions: (state) => getMenuPermissions(state.user),
    buttonPermissions: (state) => getButtonPermissions(state.user),
    hasPermission: (state) => (permission) => hasPermission(permission, state.user),
    hasAnyPermission: (state) => (permissions) => hasAnyPermission(permissions, state.user)
  },
  actions: {
    async loginByPassword(payload) {
      const res = await login(payload)
      const rawToken = res.data?.token || ''
      const token = rawToken ? `${res.data?.tokenHead || 'Bearer '}${rawToken}`.trim() : ''
      const userInfo = res.data?.userInfo || null

      if (token) {
        this.token = token
        setAuthToken(token)
      }

      if (userInfo) {
        this.user = userInfo
        setAuthUser(userInfo)
      }

      this.initialized = true
      return res
    },
    async loadCurrentUser(force = false) {
      if (!this.token) {
        this.initialized = true
        return null
      }

      if (this.user && !force) {
        this.initialized = true
        return this.user
      }

      const res = await fetchCurrentUser()
      this.user = res.data || null
      if (this.user) {
        setAuthUser(this.user)
      }
      this.initialized = true
      return this.user
    },
    logout() {
      this.token = ''
      this.user = null
      this.initialized = true
      clearAuthSession()
    }
  }
})
