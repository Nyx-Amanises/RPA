import { watch } from 'vue'
import { useAuthStore } from '../stores/auth'
import { hasAnyPermission } from '../utils/permission'

function applyPermission(el, binding, authStore) {
  const allowed = hasAnyPermission(binding.value, authStore.user)
  el.style.display = allowed ? el.__permissionOriginalDisplay || '' : 'none'
}

export const permissionDirective = {
  mounted(el, binding) {
    const authStore = useAuthStore()
    el.__permissionOriginalDisplay = el.style.display || ''
    el.__permissionStop = watch(
      () => authStore.user,
      () => applyPermission(el, binding, authStore),
      { immediate: true, deep: true }
    )
  },
  updated(el, binding) {
    applyPermission(el, binding, useAuthStore())
  },
  beforeUnmount(el) {
    if (typeof el.__permissionStop === 'function') {
      el.__permissionStop()
    }
    delete el.__permissionStop
    delete el.__permissionOriginalDisplay
  }
}
