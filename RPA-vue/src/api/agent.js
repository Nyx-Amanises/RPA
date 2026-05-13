import { request } from './http'

export async function fetchAiModelConfig() {
  const response = await request({ url: '/agent-assist/model-config', method: 'get' })
  return response.data || {}
}

export async function saveAiModelConfig(payload) {
  const response = await request({ url: '/agent-assist/model-config', method: 'put', data: payload })
  return response.data || {}
}

export async function runAgentAssist(payload) {
  const response = await request({ url: '/agent-assist', method: 'post', data: payload, timeout: 60000 })
  return response.data || {}
}

export async function runAgentQuotaCalculate(payload) {
  const response = await request({ url: '/agent-assist/quota-calculate', method: 'post', data: payload, timeout: 60000 })
  return response.data || {}
}
