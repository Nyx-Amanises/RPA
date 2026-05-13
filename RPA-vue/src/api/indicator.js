import { request } from './http'

function withPageRequestOptions(config, requestOptions = {}) {
  return { ...config, permissionScope: 'page', ...requestOptions }
}

export function fetchIndicatorPage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/indicators/page', method: 'get', params }, requestOptions))
}

export function fetchIndicatorOptions(requestOptions = {}) {
  return request({ url: '/indicators/options', method: 'get', ...requestOptions })
}

export function fetchIndicatorDetail(id) {
  return request({ url: `/indicators/${id}`, method: 'get' })
}

export function createIndicator(payload) {
  return request({ url: '/indicators', method: 'post', data: payload })
}

export function updateIndicator(id, payload) {
  return request({ url: `/indicators/${id}`, method: 'put', data: payload })
}

export function removeIndicator(id) {
  return request({ url: `/indicators/${id}`, method: 'delete' })
}

export function calculateIndicator(id) {
  return request({ url: `/indicators/${id}/calculate`, method: 'post' })
}

export function fetchQuotaRulePage(params, requestOptions = {}) {
  return request(withPageRequestOptions({ url: '/indicator-quotas/page', method: 'get', params }, requestOptions))
}

export function fetchQuotaRuleDetail(id) {
  return request({ url: `/indicator-quotas/${id}`, method: 'get' })
}

export function createQuotaRule(payload) {
  return request({ url: '/indicator-quotas', method: 'post', data: payload })
}

export function updateQuotaRule(id, payload) {
  return request({ url: `/indicator-quotas/${id}`, method: 'put', data: payload })
}

export function removeQuotaRule(id) {
  return request({ url: `/indicator-quotas/${id}`, method: 'delete' })
}

export function calculateQuotaRule(id, payload = {}) {
  return request({ url: `/indicator-quotas/${id}/calculate`, method: 'post', data: payload })
}
