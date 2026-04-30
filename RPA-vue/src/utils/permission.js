import { getAuthUser } from './auth'

const ENTRY_PERMISSION_SUFFIXES = [':page', ':tree', ':overview']

function normalizeList(value) {
  if (!value) {
    return []
  }
  return Array.isArray(value) ? value.filter(Boolean) : [value].filter(Boolean)
}

function normalizeUser(user) {
  return user || getAuthUser() || {}
}

export function getAllPermissions(user) {
  const authUser = normalizeUser(user)
  return [
    ...normalizeList(authUser.permissions),
    ...normalizeList(authUser.menuPermissions),
    ...normalizeList(authUser.buttonPermissions)
  ].filter((item, index, list) => list.indexOf(item) === index)
}

export function getMenuPermissions(user) {
  const authUser = normalizeUser(user)
  const explicitMenus = normalizeList(authUser.menuPermissions)
  if (explicitMenus.length) {
    return explicitMenus
  }
  return getAllPermissions(authUser).filter((permission) =>
    ENTRY_PERMISSION_SUFFIXES.some((suffix) => permission.endsWith(suffix))
  )
}

export function getButtonPermissions(user) {
  const authUser = normalizeUser(user)
  const explicitButtons = normalizeList(authUser.buttonPermissions)
  if (explicitButtons.length) {
    return explicitButtons
  }
  return getAllPermissions(authUser).filter(
    (permission) => !ENTRY_PERMISSION_SUFFIXES.some((suffix) => permission.endsWith(suffix))
  )
}

export function hasPermission(permission, user) {
  if (!permission) {
    return true
  }
  return getAllPermissions(user).includes(permission)
}

export function hasAnyPermission(permissions, user) {
  const permissionList = normalizeList(permissions)
  if (!permissionList.length) {
    return true
  }
  const ownedPermissions = getAllPermissions(user)
  return permissionList.some((permission) => ownedPermissions.includes(permission))
}

export function canAccessPath(path, user) {
  if (!path) {
    return true
  }
  const authUser = normalizeUser(user)
  return normalizeList(authUser.paths).includes(path)
}

export function getDefaultAuthorizedPath(user) {
  const authUser = normalizeUser(user)
  const menuPermissions = getMenuPermissions(authUser)
  const candidates = [
    { path: '/dashboard', permission: 'dashboard:view' },
    { path: '/rpa/tasks', permission: 'task:page' },
    { path: '/rpa/executions', permission: 'execution:page' },
    { path: '/rpa/robots', permission: 'robot:page' },
    { path: '/rpa/processes', permission: 'process:page' },
    { path: '/rpa/data-collection', permission: 'data:collection:page' },
    { path: '/rpa/data-analysis', permission: 'data:analysis:page' },
    { path: '/rpa/data-processing', permission: 'data:processing:page' },
    { path: '/rpa/business-data', permission: 'data:business:page' },
    { path: '/system/users', permission: 'system:user:page' },
    { path: '/system/roles', permission: 'system:role:page' },
    { path: '/system/resources', permission: 'system:resource:tree' }
  ]
  const matched = candidates.find((item) => menuPermissions.includes(item.permission) || hasPermission(item.permission, authUser))
  return matched?.path || '/system/profile'
}

function updateElementPermission(el, binding) {
  if (!hasAnyPermission(binding.value)) {
    el.remove()
  }
}

export const permissionDirective = {
  mounted: updateElementPermission,
  updated: updateElementPermission
}
