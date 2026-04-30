import { request } from './http'
import { getAuthToken } from '../utils/auth'
import {
  createMockCollectionRecord,
  getMockDashboardRecentTasks,
  getMockDashboardSummary,
  deleteMockBusinessDataRecord,
  deleteMockProcessingRecord,
  deleteMockAnalysisRecord,
  deleteMockCollectionRecord,
  deleteMockExecution,
  deleteMockRole,
  deleteMockProcess,
  deleteMockRobot,
  deleteMockTask,
  deleteMockUser,
  executeMockTask,
  getMockCollectionDetail,
  getMockCollectionPage,
  getMockAnalysisDetail,
  getMockAnalysisPage,
  getMockBusinessDataDetail,
  getMockBusinessDataPage,
  getMockProcessingDetail,
  getMockProcessingPage,
  getMockExecutionDetail,
  getMockExecutions,
  getMockProfile,
  getMockProcessDesignDetail,
  getMockProcessDetail,
  getMockProcesses,
  getMockResourceTree,
  getMockRobotDetail,
  getMockRobotOverview,
  getMockRobots,
  getMockRoleResources,
  getMockRoles,
  getMockTaskDetail,
  getMockTasks,
  disableMockProcessVersion,
  publishMockProcess,
  getMockUsers,
  resetMockUserPassword,
  saveMockResource,
  saveMockProcess,
  saveMockProcessDesign,
  saveMockRobot,
  saveMockRole,
  saveMockRoleResources,
  saveMockTask,
  saveMockUser,
  toggleMockUserStatus,
  deleteMockResource,
  updateMockPassword,
  updateMockProfile,
  uploadMockAvatar
} from '../mock/system'

function withPageRequestOptions(config, requestOptions = {}) {
  return { ...config, permissionScope: 'page', ...requestOptions }
}

export function fetchDashboardSummary(requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/dashboard/summary', method: 'get' }, requestOptions), getMockDashboardSummary)
}

export function fetchDashboardRecentTasks(params = {}, requestOptions = {}) {
  return request(
    withPageRequestOptions({ url: '/dashboard/recent-tasks', method: 'get', params }, requestOptions),
    () => getMockDashboardRecentTasks(params)
  )
}

export function fetchProfile(requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/auth/current-user', method: 'get' }, requestOptions), getMockProfile)
}

export function saveProfile(payload) {
  return request({ url: '/profile', method: 'put', data: payload }, () => updateMockProfile(payload))
}

export function savePassword(payload) {
  return request({ url: '/profile/password', method: 'put', data: payload }, updateMockPassword)
}

export function uploadProfileAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request(
    {
      url: '/profile/avatar',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    },
    () => uploadMockAvatar(file)
  )
}

export function fetchUserPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/users/page', method: 'get', params }, requestOptions), () =>
    getMockUsers(params)
  )
}

export function createUser(payload) {
  return request({ url: '/users', method: 'post', data: payload }, () => saveMockUser(payload))
}

export function updateUser(id, payload) {
  return request({ url: `/users/${id}`, method: 'put', data: payload }, () => saveMockUser({ ...payload, id }))
}

export function resetUserPassword(id) {
  return request({ url: `/users/${id}/reset-password`, method: 'put' }, () => resetMockUserPassword(id))
}

export function changeUserStatus(id, status) {
  return request({ url: `/users/${id}/status`, method: 'put', data: { status } }, () => toggleMockUserStatus(id, status))
}

export function removeUser(id) {
  return request({ url: `/users/${id}`, method: 'delete' }, () => deleteMockUser(id))
}

export function fetchRolePage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/roles/page', method: 'get', params }, requestOptions), () =>
    getMockRoles(params)
  )
}

export function fetchRobotOverview(requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/robots/overview', method: 'get' }, requestOptions), getMockRobotOverview)
}

export function fetchProcessPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/processes/page', method: 'get', params }, requestOptions), () =>
    getMockProcesses(params)
  )
}

export function fetchTaskPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/tasks/page', method: 'get', params }, requestOptions), () =>
    getMockTasks(params)
  )
}

export function fetchTaskDetail(id) {
  return request({ url: `/tasks/${id}`, method: 'get' }, () => getMockTaskDetail(id))
}

export function createTask(payload) {
  return request({ url: '/tasks', method: 'post', data: payload }, () => saveMockTask(payload))
}

export function updateTask(id, payload) {
  return request({ url: `/tasks/${id}`, method: 'put', data: payload }, () => saveMockTask({ ...payload, id }))
}

export function executeTask(id) {
  return request({ url: `/tasks/${id}/execute`, method: 'post', timeout: 60000 }, () => executeMockTask(id))
}

export function removeTask(id) {
  return request({ url: `/tasks/${id}`, method: 'delete' }, () => deleteMockTask(id))
}

export function fetchExecutionPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/executions/page', method: 'get', params }, requestOptions), () =>
    getMockExecutions(params)
  )
}

export function fetchExecutionDetail(id) {
  return request({ url: `/executions/${id}`, method: 'get' }, () => getMockExecutionDetail(id))
}

export async function subscribeExecutionLogs(id, { signal, onEvent, onOpen } = {}) {
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    return
  }

  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const token = getAuthToken() || import.meta.env.VITE_DEV_TOKEN || ''
  const headers = { Accept: 'text/event-stream' }
  if (token) {
    headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
  }

  const response = await fetch(`${baseURL}/executions/${id}/logs/stream`, { headers, signal })
  if (!response.ok) {
    throw new Error(`实时日志连接失败（${response.status}）`)
  }

  onOpen?.()
  const reader = response.body?.getReader()
  if (!reader) {
    return
  }

  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split(/\r?\n\r?\n/)
    buffer = blocks.pop() || ''
    blocks.forEach((block) => dispatchSseBlock(block, onEvent))
  }

  if (buffer.trim()) {
    dispatchSseBlock(buffer, onEvent)
  }
}

function dispatchSseBlock(block, onEvent) {
  const lines = block.split(/\r?\n/)
  const eventType = lines.find((line) => line.startsWith('event:'))?.slice(6).trim() || 'message'
  const dataText = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())
    .join('\n')

  if (!dataText) {
    return
  }

  try {
    onEvent?.({ eventType, data: JSON.parse(dataText) })
  } catch {
    onEvent?.({ eventType, data: { message: dataText } })
  }
}

export function removeExecution(id) {
  return request({ url: `/executions/${id}`, method: 'delete' }, () => deleteMockExecution(id))
}

export function fetchCollectionPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/data-collection/page', method: 'get', params }, requestOptions), () =>
    getMockCollectionPage(params)
  )
}

export function fetchCollectionDetail(id) {
  return request({ url: `/data-collection/${id}`, method: 'get' }, () => getMockCollectionDetail(id))
}

export function createCollectionRecord(payload) {
  return request({ url: '/data-collection', method: 'post', data: payload }, () => createMockCollectionRecord(payload))
}

export function removeCollectionRecord(id) {
  return request({ url: `/data-collection/${id}`, method: 'delete' }, () => deleteMockCollectionRecord(id))
}

export function fetchAnalysisPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/data-analysis/page', method: 'get', params }, requestOptions), () =>
    getMockAnalysisPage(params)
  )
}

export function fetchAnalysisDetail(id) {
  return request({ url: `/data-analysis/${id}`, method: 'get' }, () => getMockAnalysisDetail(id))
}

export function removeAnalysisRecord(id) {
  return request({ url: `/data-analysis/${id}`, method: 'delete' }, () => deleteMockAnalysisRecord(id))
}

export function fetchProcessingPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/data-processing/page', method: 'get', params }, requestOptions), () =>
    getMockProcessingPage(params)
  )
}

export function fetchProcessingDetail(id) {
  return request({ url: `/data-processing/${id}`, method: 'get' }, () => getMockProcessingDetail(id))
}

export function removeProcessingRecord(id) {
  return request({ url: `/data-processing/${id}`, method: 'delete' }, () => deleteMockProcessingRecord(id))
}

export function fetchBusinessDataPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/business-data/page', method: 'get', params }, requestOptions), () =>
    getMockBusinessDataPage(params)
  )
}

export function fetchBusinessDataDetail(id) {
  return request({ url: `/business-data/${id}`, method: 'get' }, () => getMockBusinessDataDetail(id))
}

export function removeBusinessDataRecord(id) {
  return request({ url: `/business-data/${id}`, method: 'delete' }, () => deleteMockBusinessDataRecord(id))
}

export function fetchProcessDetail(id) {
  return request({ url: `/processes/${id}`, method: 'get' }, () => getMockProcessDetail(id))
}

export function fetchProcessDesignDetail(id) {
  return request({ url: `/processes/${id}/design`, method: 'get' }, () => getMockProcessDesignDetail(id))
}

export function createProcess(payload) {
  return request({ url: '/processes', method: 'post', data: payload }, () => saveMockProcess(payload))
}

export function updateProcess(id, payload) {
  return request({ url: `/processes/${id}`, method: 'put', data: payload }, () => saveMockProcess({ ...payload, id }))
}

export function saveProcessDesign(id, payload) {
  return request({ url: `/processes/${id}/design`, method: 'put', data: payload }, () => saveMockProcessDesign(id, payload))
}

export function publishProcess(id) {
  return request({ url: `/processes/${id}/publish`, method: 'post' }, () => publishMockProcess(id))
}

export function disableProcessVersion(id) {
  return request({ url: `/processes/${id}/disable-version`, method: 'post' }, () => disableMockProcessVersion(id))
}

export function removeProcess(id) {
  return request({ url: `/processes/${id}`, method: 'delete' }, () => deleteMockProcess(id))
}

export function fetchRobotPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/robots/page', method: 'get', params }, requestOptions), () =>
    getMockRobots(params)
  )
}

export function fetchRobotDetail(id) {
  return request({ url: `/robots/${id}`, method: 'get' }, () => getMockRobotDetail(id))
}

export function createRobot(payload) {
  return request({ url: '/robots', method: 'post', data: payload }, () => saveMockRobot(payload))
}

export function updateRobot(id, payload) {
  return request({ url: `/robots/${id}`, method: 'put', data: payload }, () => saveMockRobot({ ...payload, id }))
}

export function removeRobot(id) {
  return request({ url: `/robots/${id}`, method: 'delete' }, () => deleteMockRobot(id))
}

export function createRole(payload) {
  return request({ url: '/roles', method: 'post', data: payload }, () => saveMockRole(payload))
}

export function updateRole(id, payload) {
  return request({ url: `/roles/${id}`, method: 'put', data: payload }, () => saveMockRole({ ...payload, id }))
}

export function removeRole(id) {
  return request({ url: `/roles/${id}`, method: 'delete' }, () => deleteMockRole(id))
}

export function fetchResourceTree(params = {}, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/resources/tree', method: 'get', params }, requestOptions), () =>
    getMockResourceTree(params)
  )
}

export function createResource(payload) {
  return request({ url: '/resources', method: 'post', data: payload }, () => saveMockResource(payload))
}

export function updateResource(id, payload) {
  return request({ url: `/resources/${id}`, method: 'put', data: payload }, () => saveMockResource({ ...payload, id }))
}

export function removeResource(id) {
  return request({ url: `/resources/${id}`, method: 'delete' }, () => deleteMockResource(id))
}

export function fetchRoleResourceIds(id) {
  return request({ url: `/roles/${id}/resources`, method: 'get' }, () => getMockRoleResources(id))
}

export function saveRoleResources(id, resourceIds) {
  return request({ url: `/roles/${id}/resources`, method: 'put', data: { resourceIds } }, () => saveMockRoleResources(id, resourceIds))
}
