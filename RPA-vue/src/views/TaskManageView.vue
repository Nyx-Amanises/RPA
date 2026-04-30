<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createTask,
  executeTask,
  fetchProcessPage,
  fetchRobotPage,
  fetchTaskDetail,
  fetchTaskPage,
  removeTask,
  subscribeExecutionLogs,
  updateTask
} from '../api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogMode = ref('create')
const tableData = ref([])
const detailData = ref({})
const total = ref(0)
const processOptions = ref([])
const robotOptions = ref([])
const optionsLoading = ref(false)
const optionsLoaded = ref(false)
const timeRange = ref([])
const formRef = ref()
let optionsRequest = null
let busyRefreshTimer = null
let silentRefreshRunning = false
const executionStreams = new Map()

const queryForm = reactive({
  keyword: '',
  status: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const editForm = reactive({
  id: null,
  taskCode: '',
  taskName: '',
  taxpayerIdNo: '',
  enterpriseName: '',
  processId: '',
  robotId: '',
  remark: ''
})

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taxpayerIdNo: [{ required: true, message: '请输入纳税人识别号', trigger: 'blur' }],
  enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  processId: [{ required: true, message: '请选择流程', trigger: 'change' }],
  robotId: [{ required: true, message: '请选择机器人', trigger: 'change' }]
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新建任务' : '编辑任务'))

const statusOptions = [
  { label: '待执行', value: 0 },
  { label: '执行中', value: 1 },
  { label: '执行成功', value: 2 },
  { label: '执行失败', value: 3 },
  { label: '排队中', value: 4 }
]

const selectedProcess = computed(() =>
  processOptions.value.find((item) => Number(item.id) === Number(editForm.processId))
)

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  if (typeof value !== 'string') {
    return value
  }
  return value.replace('T', ' ').split('.')[0]
}

function getTaskStatusMeta(status) {
  if (status === 4) {
    return { text: '排队中', type: 'warning' }
  }
  if (status === 1) {
    return { text: '执行中', type: 'warning' }
  }
  if (status === 2) {
    return { text: '已完成', type: 'success' }
  }
  if (status === 3) {
    return { text: '失败', type: 'danger' }
  }
  return { text: '待执行', type: 'info' }
}

function isTaskBusy(status) {
  return status === 1 || status === 4
}

function getProcessPublishMeta(process) {
  if (Number(process?.publishStatus) === 1 && Number(process?.publishedVersionNo || 0) > 0) {
    return { text: `v${process.publishedVersionNo}`, type: 'success' }
  }
  if (Number(process?.publishStatus) === 2) {
    return { text: '已停用', type: 'info' }
  }
  return { text: '未发布', type: 'warning' }
}

function canBindProcess(process) {
  return Number(process?.status) === 1 && Number(process?.publishStatus) === 1 && Number(process?.publishedVersionNo || 0) > 0
}

function getProcessDisabledReason(process) {
  if (Number(process?.status) !== 1) {
    return '流程已停用'
  }
  if (Number(process?.publishStatus) === 2) {
    return '版本已停用'
  }
  if (Number(process?.publishStatus) !== 1 || !process?.publishedVersionNo) {
    return '尚未发布'
  }
  return ''
}

function getProcessOptionLabel(process) {
  const versionText = process?.publishedVersionNo ? `v${process.publishedVersionNo}` : '未发布'
  return `${process?.processName || ''}（${process?.processCode || ''}） · ${versionText}`
}

function sortProcessOptions(list = []) {
  return [...list].sort((a, b) => Number(canBindProcess(b)) - Number(canBindProcess(a)) || Number(b.id) - Number(a.id))
}

function buildQueryParams() {
  return {
    keyword: queryForm.keyword,
    status: queryForm.status,
    startTime: timeRange.value?.[0] || '',
    endTime: timeRange.value?.[1] || '',
    pageNum: pager.pageNum,
    pageSize: pager.pageSize
  }
}

async function loadTaskPage(options = {}) {
  if (!options.silent) {
    loading.value = true
  }
  try {
    const res = await fetchTaskPage(buildQueryParams())
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
    syncBusyRefreshTimer()
  } catch (error) {
    if (!options.silent) {
      ElMessage.error(error.message || '任务列表加载失败')
    }
  } finally {
    if (!options.silent) {
      loading.value = false
    }
  }
}

async function loadOptions() {
  try {
    const [processRes, robotRes] = await Promise.all([
      fetchProcessPage({ pageNum: 1, pageSize: 100 }, { permissionScope: 'action' }),
      fetchRobotPage({ pageNum: 1, pageSize: 100 }, { permissionScope: 'action' })
    ])
    processOptions.value = sortProcessOptions(processRes.data?.list || [])
    robotOptions.value = robotRes.data?.list || []
  } catch (error) {
    ElMessage.error(error.message || '流程或机器人选项加载失败')
  }
}

async function ensureOptionsLoaded() {
  if (optionsLoaded.value) {
    return true
  }

  if (optionsRequest) {
    return optionsRequest
  }

  optionsLoading.value = true
  optionsRequest = Promise.all([
    fetchProcessPage({ pageNum: 1, pageSize: 100 }, { permissionScope: 'action' }),
    fetchRobotPage({ pageNum: 1, pageSize: 100 }, { permissionScope: 'action' })
  ])
    .then(([processRes, robotRes]) => {
      processOptions.value = sortProcessOptions(processRes.data?.list || [])
      robotOptions.value = robotRes.data?.list || []
      optionsLoaded.value = true
      return true
    })
    .catch((error) => {
      ElMessage.error(error.message || '娴佺▼鎴栨満鍣ㄤ汉閫夐」鍔犺浇澶辫触')
      return false
    })
    .finally(() => {
      optionsLoading.value = false
      optionsRequest = null
    })

  return optionsRequest
}

function handleSearch() {
  pager.pageNum = 1
  loadTaskPage()
}

function handleReset() {
  Object.assign(queryForm, { keyword: '', status: '' })
  timeRange.value = []
  handleSearch()
}

async function openCreate() {
  const ready = await ensureOptionsLoaded()
  if (!ready) {
    return
  }

  dialogMode.value = 'create'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: null,
    taskCode: '',
    taskName: '',
    taxpayerIdNo: '',
    enterpriseName: '',
    processId: '',
    robotId: '',
    remark: ''
  })
}

async function openEdit(row) {
  const ready = await ensureOptionsLoaded()
  if (!ready) {
    return
  }

  dialogMode.value = 'edit'
  dialogVisible.value = true
  try {
    const res = await fetchTaskDetail(row.id)
    Object.assign(editForm, {
      id: res.data?.id ?? row.id,
      taskCode: res.data?.taskCode ?? row.taskCode,
      taskName: res.data?.taskName ?? row.taskName,
      taxpayerIdNo: res.data?.taxpayerIdNo ?? row.taxpayerIdNo,
      enterpriseName: res.data?.enterpriseName ?? row.enterpriseName,
      processId: res.data?.processId ?? '',
      robotId: res.data?.robotId ?? '',
      remark: res.data?.remark ?? ''
    })
  } catch (error) {
    ElMessage.error(error.message || '任务详情加载失败')
    dialogVisible.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailData.value = {}
  try {
    const res = await fetchTaskDetail(row.id)
    detailData.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '任务详情加载失败')
    detailVisible.value = false
  }
}

async function submitForm() {
  try {
    await formRef.value.validate()
    if (!canBindProcess(selectedProcess.value)) {
      ElMessage.warning('请选择已启用且已发布可执行版本的流程')
      return
    }
    const payload = {
      taskName: editForm.taskName,
      taxpayerIdNo: editForm.taxpayerIdNo,
      enterpriseName: editForm.enterpriseName,
      processId: editForm.processId,
      robotId: editForm.robotId,
      remark: editForm.remark
    }
    if (dialogMode.value === 'create') {
      await createTask(payload)
      ElMessage.success('任务创建成功')
    } else {
      await updateTask(editForm.id, payload)
      ElMessage.success('任务修改成功')
    }
    dialogVisible.value = false
    loadTaskPage()
  } catch (error) {
    ElMessage.error(error.message || (dialogMode.value === 'create' ? '任务创建失败' : '任务修改失败'))
  }
}

async function handleExecute(row) {
  try {
    const res = await executeTask(row.id)
    const status = res.data?.status
    if (status === 4) {
      ElMessage.success(`任务已进入执行队列：${res.data?.executionCode || ''}`.trim())
    } else if (status === 2) {
      ElMessage.success('任务执行成功')
    } else if (status === 3) {
      ElMessage.error(res.data?.errorMessage || '任务执行失败')
    } else {
      ElMessage.success('任务已触发执行')
    }
    updateTaskRowStatus(row.id, status ?? 4)
    loadTaskPage()
    startExecutionStream(res.data?.executionId, row.id)
  } catch (error) {
    ElMessage.error(error.message || '任务执行失败')
  }
}

function updateTaskRowStatus(taskId, status) {
  if (typeof status === 'undefined' || status === null) {
    return
  }

  tableData.value = tableData.value.map((item) =>
    Number(item.id) === Number(taskId) ? { ...item, status } : item
  )

  if (Number(detailData.value?.id) === Number(taskId)) {
    detailData.value = { ...detailData.value, status }
  }
}

function updateTaskDetailIfOpen(taskId) {
  if (Number(detailData.value?.id) !== Number(taskId)) {
    return
  }

  fetchTaskDetail(taskId)
    .then((res) => {
      detailData.value = res.data || detailData.value
    })
    .catch(() => {})
}

function startExecutionStream(executionId, taskId) {
  if (!executionId || import.meta.env.VITE_USE_MOCK === 'true' || executionStreams.has(executionId)) {
    syncBusyRefreshTimer()
    return
  }

  const controller = new AbortController()
  executionStreams.set(executionId, controller)

  subscribeExecutionLogs(executionId, {
    signal: controller.signal,
    onEvent: ({ eventType, data }) => {
      const type = data?.eventType || eventType

      if (type === 'execution_queued' || type === 'execution_running') {
        updateTaskRowStatus(taskId, data.executeStatus)
        return
      }

      if (type === 'execution_success' || type === 'execution_failed') {
        updateTaskRowStatus(taskId, data.executeStatus)
        updateTaskDetailIfOpen(taskId)
        loadTaskPage({ silent: true })
        stopExecutionStream(executionId)

        if (type === 'execution_success') {
          ElMessage.success('任务执行成功，状态已刷新')
        } else {
          ElMessage.error(data.errorMessage || '任务执行失败，状态已刷新')
        }
      }
    }
  })
    .catch(() => {
      if (!controller.signal.aborted) {
        executionStreams.delete(executionId)
        syncBusyRefreshTimer()
      }
    })
    .finally(() => {
      if (executionStreams.get(executionId) === controller) {
        executionStreams.delete(executionId)
        syncBusyRefreshTimer()
      }
    })
}

function stopExecutionStream(executionId) {
  const controller = executionStreams.get(executionId)
  if (controller) {
    controller.abort()
    executionStreams.delete(executionId)
  }
}

function hasBusyTask() {
  return tableData.value.some((item) => isTaskBusy(item.status))
}

function syncBusyRefreshTimer() {
  if (hasBusyTask() && !busyRefreshTimer) {
    busyRefreshTimer = window.setInterval(async () => {
      if (silentRefreshRunning) {
        return
      }
      silentRefreshRunning = true
      try {
        await loadTaskPage({ silent: true })
        if (detailVisible.value && detailData.value?.id && isTaskBusy(detailData.value.status)) {
          updateTaskDetailIfOpen(detailData.value.id)
        }
      } finally {
        silentRefreshRunning = false
      }
    }, 3000)
  }

  if (!hasBusyTask() && busyRefreshTimer) {
    window.clearInterval(busyRefreshTimer)
    busyRefreshTimer = null
  }
}

function cleanupExecutionWatchers() {
  executionStreams.forEach((controller) => controller.abort())
  executionStreams.clear()
  if (busyRefreshTimer) {
    window.clearInterval(busyRefreshTimer)
    busyRefreshTimer = null
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除任务 ${row.taskName} 吗？`, '提示', { type: 'warning' })
    await removeTask(row.id)
    ElMessage.success('任务已删除')
    loadTaskPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除任务失败')
  }
}

async function handleCopy(value) {
  if (!value) {
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

onMounted(async () => {
  await loadTaskPage()
})

onBeforeUnmount(cleanupExecutionWatchers)
</script>

<template>
  <div class="manage-page">
    <div class="toolbar-row">
      <el-button v-permission="'task:create'" type="primary" @click="openCreate">新建任务</el-button>
      <span class="toolbar-tip">创建任务时必须绑定：流程 + 机器人</span>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="任务编码/名称">
          <el-input v-model="queryForm.keyword" placeholder="任务编码或名称" clearable style="width: 290px" />
        </el-form-item>
        <el-form-item label="任务状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable style="width: 220px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            start-placeholder="开始"
            end-placeholder="结束"
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
        <el-table-column label="任务编码" min-width="210">
          <template #default="{ row }">
            <div class="copy-cell">
              <span>{{ row.taskCode }}</span>
              <el-button link type="primary" @click="handleCopy(row.taskCode)">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="taskName" label="任务名称" min-width="180" />
        <el-table-column label="纳税人识别号" min-width="220">
          <template #default="{ row }">
            <div class="copy-cell">
              <span>{{ row.taxpayerIdNo }}</span>
              <el-button link type="primary" @click="handleCopy(row.taxpayerIdNo)">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="enterpriseName" label="企业名称" min-width="210" />
        <el-table-column label="当前流程版本" min-width="120">
          <template #default="{ row }">
            {{ row.processPublishedVersionNo ? `v${row.processPublishedVersionNo}` : '--' }}
          </template>
        </el-table-column>
        <el-table-column label="任务状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusMeta(row.status).type" effect="light">
              {{ getTaskStatusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <button v-permission="'task:view'" class="text-action primary" @click="openDetail(row)">查看详情</button>
            <button v-permission="'task:update'" class="text-action primary" :disabled="isTaskBusy(row.status)" @click="openEdit(row)">编辑</button>
            <button v-permission="'task:execute'" class="text-action success" :disabled="isTaskBusy(row.status)" @click="handleExecute(row)">执行</button>
            <button v-permission="'task:delete'" class="text-action danger" :disabled="isTaskBusy(row.status)" @click="handleDelete(row)">删除</button>
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
          @change="loadTaskPage"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
      <el-form v-loading="optionsLoading" ref="formRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="任务编码">
          <el-input v-model="editForm.taskCode" disabled placeholder="新增时自动生成" />
        </el-form-item>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="editForm.taskName" />
        </el-form-item>
        <el-form-item label="绑定流程" prop="processId">
          <el-select v-model="editForm.processId" placeholder="请选择流程" style="width: 100%">
            <el-option
              v-for="item in processOptions"
              :key="item.id"
              :label="getProcessOptionLabel(item)"
              :value="item.id"
              :disabled="!canBindProcess(item)"
            >
              <div class="process-option">
                <div class="process-option-main">
                  <span>{{ item.processName }}</span>
                  <span class="process-code">（{{ item.processCode }}）</span>
                </div>
                <div class="process-option-meta">
                  <el-tag :type="getProcessPublishMeta(item).type" effect="light" size="small">
                    {{ getProcessPublishMeta(item).text }}
                  </el-tag>
                  <span v-if="!canBindProcess(item)" class="process-disabled-reason">
                    {{ getProcessDisabledReason(item) }}
                  </span>
                </div>
              </div>
            </el-option>
          </el-select>
          <div v-if="selectedProcess" class="form-hint">
            当前发布版本：{{ selectedProcess.publishedVersionNo ? `v${selectedProcess.publishedVersionNo}` : '未发布' }}，实际执行时会写入执行记录。
          </div>
        </el-form-item>
        <el-form-item label="绑定机器人" prop="robotId">
          <el-select v-model="editForm.robotId" placeholder="请选择机器人" style="width: 100%">
            <el-option
              v-for="item in robotOptions"
              :key="item.id"
              :label="`${item.robotName}（${item.robotCode}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="纳税人识别号" prop="taxpayerIdNo">
          <el-input v-model="editForm.taxpayerIdNo" />
        </el-form-item>
        <el-form-item label="企业名称" prop="enterpriseName">
          <el-input v-model="editForm.enterpriseName" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="任务详情" width="1000px" destroy-on-close>
      <div class="detail-block">
        <div class="detail-block-title">基本信息</div>
        <div class="detail-table">
          <div class="detail-item"><span>任务编码</span><strong>{{ detailData.taskCode || '--' }}</strong></div>
          <div class="detail-item"><span>任务名称</span><strong>{{ detailData.taskName || '--' }}</strong></div>
          <div class="detail-item"><span>纳税人识别号</span><strong>{{ detailData.taxpayerIdNo || '--' }}</strong></div>
          <div class="detail-item"><span>企业名称</span><strong>{{ detailData.enterpriseName || '--' }}</strong></div>
          <div class="detail-item"><span>流程编码</span><strong>{{ detailData.processCode || '--' }}</strong></div>
          <div class="detail-item"><span>当前流程版本</span><strong>{{ detailData.processPublishedVersionNo ? `v${detailData.processPublishedVersionNo}` : '--' }}</strong></div>
          <div class="detail-item"><span>机器人编码</span><strong>{{ detailData.robotCode || '--' }}</strong></div>
          <div class="detail-item">
            <span>任务状态</span>
            <strong><el-tag :type="getTaskStatusMeta(detailData.status).type" effect="light">{{ getTaskStatusMeta(detailData.status).text }}</el-tag></strong>
          </div>
          <div class="detail-item"><span>创建时间</span><strong>{{ formatDateTime(detailData.createTime) }}</strong></div>
          <div class="detail-item"><span>开始时间</span><strong>{{ formatDateTime(detailData.startTime) }}</strong></div>
          <div class="detail-item"><span>结束时间</span><strong>{{ formatDateTime(detailData.endTime) }}</strong></div>
        </div>
      </div>

      <div class="detail-block">
        <div class="detail-block-title">执行记录</div>
        <div class="detail-table short">
          <div class="detail-item"><span>开始时间</span><strong>{{ formatDateTime(detailData.startTime) }}</strong></div>
          <div class="detail-item"><span>结束时间</span><strong>{{ formatDateTime(detailData.endTime) }}</strong></div>
          <div class="detail-item"><span>执行时长</span><strong>{{ detailData.durationSeconds ? `${detailData.durationSeconds}秒` : '--' }}</strong></div>
          <div class="detail-item"><span>错误信息</span><strong>{{ detailData.errorMessage || '-' }}</strong></div>
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
.toolbar-tip {
  color: #6b7280;
  font-size: 14px;
}

.copy-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.process-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.process-option-main {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-code,
.process-disabled-reason,
.form-hint {
  color: #64748b;
  font-size: 13px;
}

.process-option-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.form-hint {
  margin-top: 6px;
  line-height: 1.5;
}

.detail-block {
  margin-bottom: 20px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px #edf2f7;
}

.detail-block-title {
  padding: 18px 20px;
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
  border-bottom: 1px solid #edf2f7;
}

.detail-table {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-table.short {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-item {
  display: grid;
  grid-template-columns: 160px 1fr;
  min-height: 54px;
}

.detail-item span {
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: #f7f9fc;
  color: #475569;
  font-weight: 600;
  border-right: 1px solid #edf2f7;
  border-bottom: 1px solid #edf2f7;
}

.detail-item strong {
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: #1f2937;
  font-weight: 500;
  border-bottom: 1px solid #edf2f7;
}
</style>
