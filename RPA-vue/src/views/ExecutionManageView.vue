<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchExecutionDetail, fetchExecutionPage, subscribeExecutionLogs } from '../api/system'

const loading = ref(false)
const detailVisible = ref(false)
const tableData = ref([])
const detailData = ref({})
const liveStepLogs = ref([])
const streamStatus = ref('idle')
const streamMessage = ref('静态记录')
const currentStep = ref(null)
const total = ref(0)
const timeRange = ref([])
let streamController = null

const queryForm = reactive({
  taskId: '',
  executeStatus: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const statusOptions = [
  { label: '待执行', value: 0 },
  { label: '执行中', value: 1 },
  { label: '成功', value: 2 },
  { label: '失败', value: 3 },
  { label: '排队中', value: 4 }
]

const executionSteps = computed(() => {
  const historySteps = Array.isArray(detailData.value?.stepLogs)
    ? detailData.value.stepLogs
    : Array.isArray(detailData.value?.executionStepLogs)
      ? detailData.value.executionStepLogs
      : Array.isArray(detailData.value?.steps)
        ? detailData.value.steps
        : []
  const rawSteps = [...historySteps, ...liveStepLogs.value]
  const mergedSteps = []
  const stepIndexMap = new Map()

  rawSteps.forEach((item, index) => {
    const stepNo = item.stepNo ?? index + 1
    const key = `step-${stepNo}`
    const normalized = {
      id: item.id ?? key,
      stepNo,
      stepName: item.stepName || '',
      stepType: item.stepType || '',
      scriptLang: item.scriptLang || '',
      rawMessage: item.message || item.output || item.result || item.errorMessage || '',
      executeTime: item.executeTime || item.startTime || item.createTime || item.updateTime || item.eventTime || '',
      executeStatus: item.executeStatus ?? item.status ?? detailData.value?.executeStatus ?? 0,
      contextKeys: Array.isArray(item.contextKeys) ? item.contextKeys : [],
      timeoutSeconds: item.timeoutSeconds,
      failureStrategy: item.failureStrategy,
      durationMillis: item.durationMillis,
      stackTrace: item.stackTrace || '',
      screenshotUrl: item.screenshotUrl || ''
    }

    if (stepIndexMap.has(key)) {
      mergedSteps.splice(stepIndexMap.get(key), 1, { ...mergedSteps[stepIndexMap.get(key)], ...normalized })
    } else {
      stepIndexMap.set(key, mergedSteps.length)
      mergedSteps.push(normalized)
    }
  })

  return mergedSteps
})

const latestFailedStep = computed(() => {
  return [...executionSteps.value].reverse().find((step) => step.executeStatus === 3) || null
})

const activeScreenshotUrl = computed(() => detailData.value?.screenshotUrl || latestFailedStep.value?.screenshotUrl || '')

const activeStackTrace = computed(() => detailData.value?.stackTrace || latestFailedStep.value?.stackTrace || '')

const logContent = computed(() => {
  if (detailData.value?.logContent) {
    return detailData.value.logContent
  }
  if (executionSteps.value.length) {
    return JSON.stringify(executionSteps.value, null, 2)
  }
  return '暂无日志内容'
})

const streamStatusMeta = computed(() => {
  if (streamStatus.value === 'connecting') {
    return { text: '连接中', type: 'warning' }
  }
  if (streamStatus.value === 'connected') {
    return { text: '实时同步', type: 'success' }
  }
  if (streamStatus.value === 'closed') {
    return { text: '已结束', type: 'info' }
  }
  if (streamStatus.value === 'error') {
    return { text: '连接异常', type: 'danger' }
  }
  return { text: '静态记录', type: 'info' }
})

function formatDateTime(value) {
  if (!value) {
    return '--'
  }

  if (typeof value !== 'string') {
    return value
  }

  return value.replace('T', ' ').split('.')[0]
}

function getStatusMeta(status) {
  if (status === 4) {
    return { text: '排队中', type: 'warning' }
  }

  if (status === 1) {
    return { text: '执行中', type: 'warning' }
  }

  if (status === 2) {
    return { text: '成功', type: 'success' }
  }

  if (status === 3) {
    return { text: '失败', type: 'danger' }
  }

  return { text: '待执行', type: 'info' }
}

function parseStructuredMessage(message) {
  if (typeof message !== 'string') {
    return null
  }

  const text = message.trim()
  if (!text.startsWith('{') || !text.endsWith('}')) {
    return null
  }

  const body = text.slice(1, -1).trim()
  if (!body) {
    return null
  }

  const result = {}
  body.split(/,\s*/).forEach((pair) => {
    const separatorIndex = pair.indexOf('=')
    if (separatorIndex === -1) {
      return
    }

    const key = pair.slice(0, separatorIndex).trim()
    const value = pair.slice(separatorIndex + 1).trim()
    if (key) {
      result[key] = value
    }
  })

  return Object.keys(result).length ? result : null
}

function isGenericStepName(stepName) {
  if (!stepName) {
    return true
  }

  return /^\d+$/.test(String(stepName).trim())
}

function inferStepDisplayName(step) {
  if (!isGenericStepName(step.stepName)) {
    return step.stepName
  }

  const parsed = parseStructuredMessage(step.rawMessage)
  const businessMessage = parsed?.message || ''

  if (businessMessage.includes('采集')) {
    return '采集'
  }
  if (businessMessage.includes('解析')) {
    return '解析'
  }
  if (businessMessage.includes('加工')) {
    return '加工'
  }
  if (businessMessage.includes('落库') || businessMessage.includes('保存')) {
    return '落库'
  }

  const hintSource = `${step.rawMessage || ''} ${(step.contextKeys || []).join(' ')}`
  if (/collectionId|collected/i.test(hintSource)) {
    return '采集'
  }
  if (/analysisId|parsed/i.test(hintSource)) {
    return '解析'
  }
  if (/processingId|processed/i.test(hintSource)) {
    return '加工'
  }
  if (/saved|executionId/i.test(hintSource)) {
    return '落库'
  }

  return step.stepName || `步骤${step.stepNo}`
}

function buildQueryParams() {
  return {
    taskId: queryForm.taskId,
    executeStatus: queryForm.executeStatus,
    startTime: timeRange.value?.[0] || '',
    endTime: timeRange.value?.[1] || '',
    pageNum: pager.pageNum,
    pageSize: pager.pageSize
  }
}

function getStepTitle(step) {
  const displayName = inferStepDisplayName(step)
  return `步骤${step.stepNo}${displayName ? ` - ${displayName}` : ''}`
}

function getStepSubtitle(step) {
  if (step.stepType && step.scriptLang) {
    return `${step.stepType} (${step.scriptLang})`
  }

  return step.stepType || step.scriptLang || ''
}

function getStepMessage(step) {
  const parsed = parseStructuredMessage(step.rawMessage)
  if (!parsed) {
    return step.rawMessage || '暂无步骤摘要'
  }

  const lines = []
  if (parsed.message) {
    lines.push(parsed.message)
  }

  Object.entries(parsed).forEach(([key, value]) => {
    if (key !== 'message') {
      lines.push(`${key}: ${value}`)
    }
  })

  return lines.join('\n') || step.rawMessage || '暂无步骤摘要'
}

function formatDurationMillis(value) {
  if (value === null || typeof value === 'undefined' || value === '') {
    return ''
  }
  const duration = Number(value)
  if (!Number.isFinite(duration)) {
    return ''
  }
  return duration >= 1000 ? `${(duration / 1000).toFixed(1)}秒` : `${duration}毫秒`
}

function isLiveStatus(status) {
  return status === 1 || status === 4
}

function upsertLiveStep(step) {
  if (!step?.stepNo) {
    return
  }
  const index = liveStepLogs.value.findIndex((item) => item.stepNo === step.stepNo)
  if (index >= 0) {
    liveStepLogs.value.splice(index, 1, { ...liveStepLogs.value[index], ...step })
  } else {
    liveStepLogs.value.push(step)
  }
}

function handleStreamEvent({ eventType, data }) {
  const type = data?.eventType || eventType
  streamMessage.value = data?.message || streamMessage.value

  if (type === 'execution_queued' || type === 'execution_running') {
    detailData.value = { ...detailData.value, ...data }
    return
  }

  if (type === 'step_start') {
    currentStep.value = data
    upsertLiveStep({
      ...data,
      executeStatus: 1,
      status: 1,
      executeTime: data.eventTime
    })
    return
  }

  if (type === 'step_success' || type === 'step_failed') {
    upsertLiveStep({
      ...data,
      executeStatus: data.status ?? (type === 'step_failed' ? 3 : 2),
      executeTime: data.executeTime || data.eventTime
    })
    if (currentStep.value?.stepNo === data.stepNo) {
      currentStep.value = null
    }
    if (type === 'step_failed' && data.screenshotUrl) {
      detailData.value = { ...detailData.value, screenshotUrl: data.screenshotUrl }
    }
    return
  }

  if (type === 'execution_success' || type === 'execution_failed') {
    detailData.value = { ...detailData.value, ...data }
    currentStep.value = null
    streamStatus.value = 'closed'
    streamMessage.value = data?.message || '执行已结束'
    loadExecutionPage()
  }
}

function stopLogStream() {
  if (streamController) {
    streamController.abort()
    streamController = null
  }
}

function startLogStream(executionId) {
  stopLogStream()
  liveStepLogs.value = []
  currentStep.value = null

  if (!executionId || import.meta.env.VITE_USE_MOCK === 'true') {
    streamStatus.value = 'idle'
    streamMessage.value = '静态记录'
    return
  }

  streamStatus.value = 'connecting'
  streamMessage.value = '正在连接实时日志'
  const controller = new AbortController()
  streamController = controller

  subscribeExecutionLogs(executionId, {
    signal: controller.signal,
    onOpen: () => {
      streamStatus.value = 'connected'
      streamMessage.value = '实时日志已连接'
    },
    onEvent: handleStreamEvent
  })
    .then(() => {
      if (!controller.signal.aborted && streamStatus.value !== 'closed') {
        streamStatus.value = 'closed'
        streamMessage.value = '实时日志已结束'
      }
    })
    .catch((error) => {
      if (controller.signal.aborted) {
        return
      }
      streamStatus.value = 'error'
      streamMessage.value = error.message || '实时日志连接异常'
    })
}

async function loadExecutionPage() {
  loading.value = true
  try {
    const res = await fetchExecutionPage(buildQueryParams())
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '执行记录加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadExecutionPage()
}

function handleReset() {
  Object.assign(queryForm, { taskId: '', executeStatus: '' })
  timeRange.value = []
  handleSearch()
}

async function openDetail(row) {
  detailVisible.value = true
  detailData.value = {}
  liveStepLogs.value = []
  currentStep.value = null
  streamStatus.value = 'idle'
  streamMessage.value = '静态记录'

  try {
    const res = await fetchExecutionDetail(row.id)
    detailData.value = res.data || {}
    if (isLiveStatus(detailData.value.executeStatus ?? row.executeStatus)) {
      startLogStream(row.id)
    }
  } catch (error) {
    ElMessage.error(error.message || '执行记录详情加载失败')
    detailVisible.value = false
  }
}

function handleDetailClosed() {
  stopLogStream()
  liveStepLogs.value = []
  currentStep.value = null
}

watch(detailVisible, (visible) => {
  if (!visible) {
    stopLogStream()
  }
})

onMounted(loadExecutionPage)
onBeforeUnmount(stopLogStream)
</script>

<template>
  <div class="manage-page">
    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="任务ID">
          <el-input v-model="queryForm.taskId" placeholder="任务ID" clearable />
        </el-form-item>
        <el-form-item label="执行状态">
          <el-select v-model="queryForm.executeStatus" placeholder="请选择" clearable style="width: 220px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column type="index" label="序号" width="72" />
        <el-table-column prop="executionCode" label="执行ID" min-width="190" />
        <el-table-column prop="taskCode" label="任务编码" min-width="180" />
        <el-table-column prop="processCode" label="流程编码" min-width="140" />
        <el-table-column label="流程版本" min-width="100">
          <template #default="{ row }">
            {{ row.processVersionNo ? `v${row.processVersionNo}` : '--' }}
          </template>
        </el-table-column>
        <el-table-column prop="robotCode" label="机器人编码" min-width="140" />
        <el-table-column label="执行状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.executeStatus).type" effect="light">
              {{ getStatusMeta(row.executeStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="执行时长" min-width="100">
          <template #default="{ row }">
            {{ row.durationSeconds ? `${row.durationSeconds}秒` : '--' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <button v-permission="'execution:view'" class="text-action primary" @click="openDetail(row)">查看详情</button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pagination">
        <el-pagination
          v-model:current-page="pager.pageNum"
          v-model:page-size="pager.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @change="loadExecutionPage"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="执行记录详情" width="1120px" destroy-on-close @closed="handleDetailClosed">
      <div class="detail-table">
        <div class="detail-item"><span>执行ID</span><strong>{{ detailData.executionCode || '--' }}</strong></div>
        <div class="detail-item"><span>任务编码</span><strong>{{ detailData.taskCode || '--' }}</strong></div>
        <div class="detail-item"><span>流程编码</span><strong>{{ detailData.processCode || '--' }}</strong></div>
        <div class="detail-item"><span>流程版本</span><strong>{{ detailData.processVersionNo ? `v${detailData.processVersionNo}` : '--' }}</strong></div>
        <div class="detail-item"><span>机器人编码</span><strong>{{ detailData.robotCode || '--' }}</strong></div>
        <div class="detail-item">
          <span>执行状态</span>
          <strong>
            <el-tag :type="getStatusMeta(detailData.executeStatus).type" effect="light">
              {{ getStatusMeta(detailData.executeStatus).text }}
            </el-tag>
          </strong>
        </div>
        <div class="detail-item"><span>执行时长</span><strong>{{ detailData.durationSeconds ? `${detailData.durationSeconds}秒` : '--' }}</strong></div>
        <div class="detail-item"><span>开始时间</span><strong>{{ formatDateTime(detailData.startTime) }}</strong></div>
        <div class="detail-item"><span>结束时间</span><strong>{{ formatDateTime(detailData.endTime) }}</strong></div>
      </div>

      <div class="live-status-bar">
        <div class="live-status-main">
          <span class="live-pulse" :class="streamStatus"></span>
          <strong>实时执行日志</strong>
          <el-tag :type="streamStatusMeta.type" effect="light">{{ streamStatusMeta.text }}</el-tag>
        </div>
        <div class="live-status-copy">
          <span v-if="currentStep">当前步骤：步骤{{ currentStep.stepNo }} {{ currentStep.stepName || '' }}</span>
          <span v-else>{{ streamMessage }}</span>
        </div>
      </div>

      <div class="execution-body">
        <div class="execution-pane">
          <div class="pane-title">执行步骤</div>
          <div v-if="executionSteps.length" class="timeline-list">
            <div v-for="(step, index) in executionSteps" :key="step.id" class="timeline-item">
              <div class="timeline-rail">
                <div class="timeline-dot" :class="getStatusMeta(step.executeStatus).type"></div>
                <div v-if="index !== executionSteps.length - 1" class="timeline-line"></div>
              </div>
              <div class="timeline-card">
                <div class="timeline-header">
                  <div>
                    <div class="timeline-title">{{ getStepTitle(step) }}</div>
                    <div v-if="getStepSubtitle(step)" class="timeline-subtitle">{{ getStepSubtitle(step) }}</div>
                  </div>
                  <div class="timeline-time">{{ formatDateTime(step.executeTime) }}</div>
                </div>
                <div class="timeline-meta">
                  <span v-if="step.timeoutSeconds">超时 {{ step.timeoutSeconds }}秒</span>
                  <span v-if="step.failureStrategy">失败策略 {{ step.failureStrategy }}</span>
                  <span v-if="formatDurationMillis(step.durationMillis)">耗时 {{ formatDurationMillis(step.durationMillis) }}</span>
                </div>
                <div
                  class="timeline-message"
                  :class="{ 'timeline-message-danger': getStatusMeta(step.executeStatus).type === 'danger' }"
                >
                  {{ getStepMessage(step) }}
                </div>
                <div v-if="step.screenshotUrl" class="timeline-actions">
                  <el-link type="danger" :href="step.screenshotUrl" target="_blank">查看失败截图</el-link>
                </div>
                <el-collapse v-if="step.stackTrace" class="stack-collapse">
                  <el-collapse-item title="失败堆栈" name="stack">
                    <pre class="stack-box">{{ step.stackTrace }}</pre>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </div>
          <div v-else class="empty-box">暂无步骤记录</div>
        </div>

        <div class="execution-pane">
          <div class="pane-title">错误信息</div>
          <div class="error-box">{{ detailData.errorMessage || '-' }}</div>
          <template v-if="activeScreenshotUrl">
            <div class="pane-title">失败截图</div>
            <a :href="activeScreenshotUrl" target="_blank" class="screenshot-link">
              <img :src="activeScreenshotUrl" alt="失败截图" class="screenshot-preview" />
            </a>
          </template>
          <template v-if="activeStackTrace">
            <div class="pane-title">失败堆栈</div>
            <pre class="stack-box side-stack">{{ activeStackTrace }}</pre>
          </template>
          <div class="pane-title">执行日志</div>
          <pre class="log-box">{{ logContent }}</pre>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail-table {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 24px;
  box-shadow: inset 0 0 0 1px #edf2f7;
}

.detail-item {
  display: grid;
  grid-template-columns: 140px 1fr;
  min-height: 50px;
}

.detail-item span {
  display: flex;
  align-items: center;
  padding: 0 14px;
  font-weight: 600;
  color: #475569;
  background: #f7f9fc;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
}

.detail-item strong {
  display: flex;
  align-items: center;
  padding: 0 14px;
  color: #1f2937;
  font-weight: 500;
  border-bottom: 1px solid #edf2f7;
}

.live-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 22px;
  border: 1px solid #dbe7f7;
  border-radius: 10px;
  background: #f8fbff;
}

.live-status-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: #1f2937;
}

.live-status-copy {
  min-width: 0;
  color: #64748b;
  text-align: right;
  word-break: break-word;
}

.live-pulse {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #94a3b8;
}

.live-pulse.connecting,
.live-pulse.connected {
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.12);
}

.live-pulse.error {
  background: #ef4444;
  box-shadow: 0 0 0 6px rgba(239, 68, 68, 0.12);
}

.execution-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 1.05fr);
  gap: 24px;
}

.execution-pane {
  min-height: 280px;
}

.pane-title {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.timeline-list {
  display: grid;
  gap: 0;
}

.timeline-item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 14px;
}

.timeline-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-line {
  flex: 1;
  width: 2px;
  min-height: 40px;
  margin-top: 6px;
  background: #dbe7f7;
}

.timeline-card {
  padding: 18px 20px;
  margin-bottom: 22px;
  border: 1px solid #edf2f7;
  border-radius: 14px;
  background: #ffffff;
}

.timeline-header {
  display: flex;
  gap: 16px;
  justify-content: space-between;
  align-items: flex-start;
}

.timeline-dot {
  width: 14px;
  height: 14px;
  margin-top: 8px;
  border-radius: 50%;
  background: #94a3b8;
  box-shadow: 0 0 0 6px rgba(148, 163, 184, 0.12);
}

.timeline-dot.success {
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.12);
}

.timeline-dot.warning {
  background: #f59e0b;
  box-shadow: 0 0 0 6px rgba(245, 158, 11, 0.12);
}

.timeline-dot.danger {
  background: #ef4444;
  box-shadow: 0 0 0 6px rgba(239, 68, 68, 0.12);
}

.timeline-title {
  font-size: 18px;
  font-weight: 700;
  color: #334155;
}

.timeline-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 14px;
}

.timeline-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.timeline-meta span {
  padding: 4px 8px;
  border-radius: 6px;
  color: #64748b;
  background: #f1f5f9;
  font-size: 12px;
}

.timeline-message {
  margin-top: 12px;
  color: #475569;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.timeline-message-danger {
  color: #ef4444;
}

.timeline-time {
  color: #94a3b8;
  font-size: 14px;
  white-space: nowrap;
}

.timeline-actions {
  margin-top: 12px;
}

.stack-collapse {
  margin-top: 12px;
  border-top: 1px solid #edf2f7;
  border-bottom: none;
}

.stack-box {
  max-height: 260px;
  margin: 0;
  padding: 14px;
  overflow: auto;
  border-radius: 8px;
  background: #111827;
  color: #e5e7eb;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.side-stack {
  margin-bottom: 18px;
}

.screenshot-link {
  display: block;
  margin-bottom: 18px;
}

.screenshot-preview {
  width: 100%;
  max-height: 260px;
  object-fit: contain;
  border: 1px solid #fee2e2;
  border-radius: 8px;
  background: #fff7f7;
}

.empty-box {
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d7e2f0;
  border-radius: 14px;
  color: #94a3b8;
  background: #f8fbff;
}

.error-box {
  min-height: 64px;
  padding: 16px;
  margin-bottom: 18px;
  border-radius: 10px;
  background: #fff1f2;
  color: #ef4444;
}

.log-box {
  min-height: 180px;
  padding: 16px;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: inset 0 0 0 1px #e2e8f0;
}

@media (max-width: 1100px) {
  .execution-body {
    grid-template-columns: 1fr;
  }

  .timeline-header {
    flex-direction: column;
  }

  .timeline-time {
    white-space: normal;
  }

  .live-status-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .live-status-copy {
    text-align: left;
  }
}
</style>
