<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowDown, ArrowUp, CircleClose, Delete, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createProcess,
  disableProcessVersion,
  fetchProcessDesignDetail,
  fetchProcessDetail,
  fetchProcessPage,
  publishProcess,
  removeProcess,
  saveProcessDesign,
  updateProcess
} from '../api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const designVisible = ref(false)
const dialogMode = ref('create')
const tableData = ref([])
const detailData = ref({})
const total = ref(0)
const formRef = ref()
const designLoading = ref(false)
const currentProcessId = ref(null)
const designVersionNo = ref()
const activeStepIndex = ref(0)
const publishLoading = ref(false)

const queryForm = reactive({
  processName: '',
  processCode: '',
  status: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const editForm = reactive({
  id: null,
  processCode: '',
  processName: '',
  description: '',
  status: 1
})

const designForm = reactive({
  steps: []
})

const rules = {
  processCode: [{ required: true, message: '请输入流程编码', trigger: 'blur' }],
  processName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增流程' : '编辑流程'))

const statusOptions = [
  { label: '停用', value: 0 },
  { label: '启用', value: 1 }
]

const stepTypeOptions = [
  { label: '动作', value: 'ACTION', tag: '动作步骤' },
  { label: '条件', value: 'CONDITION', tag: '条件判断' },
  { label: '循环', value: 'LOOP', tag: '循环步骤' }
]

const scriptLangOptions = [
  { label: 'Groovy', value: 'Groovy' },
  { label: 'Python', value: 'Python' },
  { label: 'Java', value: 'Java' },
  { label: 'JavaScript', value: 'JavaScript' },
  { label: 'SQL', value: 'SQL' }
]

const failureStrategyOptions = [
  { label: '失败即停止', value: 'STOP' },
  { label: '失败后继续', value: 'CONTINUE' },
  { label: '失败后重试', value: 'RETRY' }
]

const activeStep = computed(() => designForm.steps[activeStepIndex.value] || null)

function getStatusMeta(status) {
  return status === 1 ? { text: '启用', type: 'success' } : { text: '停用', type: 'danger' }
}

function getPublishStatusMeta(status) {
  const value = Number(status ?? 0)
  if (value === 1) {
    return { text: '已发布', type: 'success' }
  }
  if (value === 2) {
    return { text: '已停用', type: 'info' }
  }
  return { text: '草稿', type: 'warning' }
}

function getVersionStatusMeta(status) {
  return Number(status) === 1 ? { text: '发布中', type: 'success' } : { text: '已停用', type: 'info' }
}

function getStepTypeTag(stepType) {
  return stepTypeOptions.find((item) => item.value === stepType)?.tag || stepType || '未设置'
}

function getFailureStrategyLabel(strategy) {
  return failureStrategyOptions.find((item) => item.value === strategy)?.label || '失败即停止'
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  if (typeof value !== 'string') {
    return value
  }
  return value.replace('T', ' ').split('.')[0]
}

function getStepPreview(content) {
  if (!content) {
    return '暂无脚本内容'
  }
  return content.replace(/\s+/g, ' ').trim().slice(0, 72) || '暂无脚本内容'
}

function normalizeSteps(steps = []) {
  return steps
    .slice()
    .sort((a, b) => (a.stepNo || 0) - (b.stepNo || 0))
    .map((item, index) => ({
      id: item.id || null,
      stepNo: item.stepNo ?? index + 1,
      stepName: item.stepName || '',
      stepType: item.stepType || 'ACTION',
      scriptLang: item.scriptLang || 'Groovy',
      scriptContent: item.scriptContent || '',
      timeoutSeconds: item.timeoutSeconds || 60,
      failureStrategy: item.failureStrategy || 'STOP'
    }))
}

function resetStepNos() {
  designForm.steps.forEach((item, index) => {
    item.stepNo = index + 1
  })
}

function normalizeActiveStepIndex() {
  if (!designForm.steps.length) {
    activeStepIndex.value = 0
    return
  }
  if (activeStepIndex.value >= designForm.steps.length) {
    activeStepIndex.value = designForm.steps.length - 1
  }
}

async function loadProcesses() {
  loading.value = true
  try {
    const res = await fetchProcessPage({ ...queryForm, ...pager })
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '流程列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadProcesses()
}

function handleReset() {
  Object.assign(queryForm, { processName: '', processCode: '', status: '' })
  handleSearch()
}

function openCreate() {
  dialogMode.value = 'create'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: null,
    processCode: '',
    processName: '',
    description: '',
    status: 1
  })
}

function openEdit(row) {
  dialogMode.value = 'edit'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: row.id,
    processCode: row.processCode,
    processName: row.processName,
    description: row.description || '',
    status: row.status
  })
}

async function openDetail(row) {
  detailVisible.value = true
  detailData.value = {}
  try {
    const res = await fetchProcessDetail(row.id)
    detailData.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '流程详情加载失败')
    detailVisible.value = false
  }
}

async function openDesign(row) {
  designVisible.value = true
  designLoading.value = true
  currentProcessId.value = row.id
  designVersionNo.value = row.publishedVersionNo
  activeStepIndex.value = 0
  designForm.steps = []
  try {
    const res = await fetchProcessDesignDetail(row.id)
    detailData.value = res.data || {}
    designVersionNo.value = res.data?.publishedVersionNo
    designForm.steps = normalizeSteps(res.data?.steps || [])
    resetStepNos()
    normalizeActiveStepIndex()
  } catch (error) {
    ElMessage.error(error.message || '流程设计加载失败')
    designVisible.value = false
  } finally {
    designLoading.value = false
  }
}

function addStep() {
  designForm.steps.push({
    id: null,
    stepNo: designForm.steps.length + 1,
    stepName: '',
    stepType: 'ACTION',
    scriptLang: 'Groovy',
    scriptContent: '',
    timeoutSeconds: 60,
    failureStrategy: 'STOP'
  })
  activeStepIndex.value = designForm.steps.length - 1
}

function removeStep(index) {
  designForm.steps.splice(index, 1)
  resetStepNos()
  normalizeActiveStepIndex()
}

function selectStep(index) {
  activeStepIndex.value = index
}

function moveStep(index, direction) {
  const nextIndex = index + direction
  if (nextIndex < 0 || nextIndex >= designForm.steps.length) {
    return
  }
  const [target] = designForm.steps.splice(index, 1)
  designForm.steps.splice(nextIndex, 0, target)
  activeStepIndex.value = nextIndex
  resetStepNos()
}

async function submitForm() {
  try {
    await formRef.value.validate()
    const payload = {
      processCode: editForm.processCode,
      processName: editForm.processName,
      description: editForm.description,
      status: editForm.status
    }
    if (dialogMode.value === 'create') {
      await createProcess(payload)
      ElMessage.success('新增流程成功')
    } else {
      await updateProcess(editForm.id, payload)
      ElMessage.success('修改流程成功')
    }
    dialogVisible.value = false
    loadProcesses()
  } catch (error) {
    ElMessage.error(error.message || (dialogMode.value === 'create' ? '新增流程失败' : '修改流程失败'))
  }
}

function validateDesignSteps() {
  if (!designForm.steps.length) {
    ElMessage.warning('请至少添加一个步骤')
    return false
  }

  const invalidStep = designForm.steps.find(
    (item) =>
      !item.stepName?.trim() ||
      !item.stepType ||
      !item.scriptLang ||
      !item.scriptContent?.trim() ||
      !item.timeoutSeconds ||
      !item.failureStrategy
  )
  if (invalidStep) {
    ElMessage.warning('请完善每一个步骤的名称、类型、脚本语言、超时时间、失败策略和脚本内容')
    return false
  }
  return true
}

function buildDesignPayload() {
  return {
    versionNo: designVersionNo.value,
    steps: designForm.steps.map((item, index) => ({
      stepNo: index + 1,
      stepName: item.stepName.trim(),
      stepType: item.stepType,
      scriptLang: item.scriptLang,
      scriptContent: item.scriptContent,
      timeoutSeconds: item.timeoutSeconds,
      failureStrategy: item.failureStrategy
    }))
  }
}

async function submitDesign(closeAfterSave = true) {
  if (!validateDesignSteps()) {
    return false
  }

  try {
    const payload = buildDesignPayload()
    await saveProcessDesign(currentProcessId.value, payload)
    ElMessage.success('流程设计已保存')
    if (closeAfterSave) {
      designVisible.value = false
    }
    await loadProcesses()
    return true
  } catch (error) {
    ElMessage.error(error.message || '保存流程设计失败')
    return false
  }
}

async function refreshCurrentDesign() {
  if (!currentProcessId.value) {
    return
  }
  const res = await fetchProcessDesignDetail(currentProcessId.value)
  detailData.value = res.data || {}
  designVersionNo.value = res.data?.publishedVersionNo
  designForm.steps = normalizeSteps(res.data?.steps || [])
  resetStepNos()
  normalizeActiveStepIndex()
}

async function handlePublish(row) {
  publishLoading.value = true
  try {
    await publishProcess(row.id)
    ElMessage.success('流程版本已发布')
    await loadProcesses()
    if (designVisible.value && currentProcessId.value === row.id) {
      await refreshCurrentDesign()
    }
  } catch (error) {
    ElMessage.error(error.message || '发布流程版本失败')
  } finally {
    publishLoading.value = false
  }
}

async function publishCurrentDesign() {
  const saved = await submitDesign(false)
  if (saved) {
    await handlePublish({ id: currentProcessId.value })
  }
}

async function disableCurrentVersion() {
  await handleDisableVersion({
    id: currentProcessId.value,
    processName: detailData.value.processName
  })
}

async function handleDisableVersion(row) {
  try {
    await ElMessageBox.confirm(`确认停用流程 ${row.processName || detailData.value.processName || ''} 的当前发布版本吗？`, '提示', { type: 'warning' })
    publishLoading.value = true
    await disableProcessVersion(row.id)
    ElMessage.success('流程版本已停用')
    await loadProcesses()
    if (designVisible.value && currentProcessId.value === row.id) {
      await refreshCurrentDesign()
    }
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '停用流程版本失败')
  } finally {
    publishLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除流程 ${row.processName} 吗？`, '提示', { type: 'warning' })
    await removeProcess(row.id)
    ElMessage.success('流程已删除')
    loadProcesses()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除流程失败')
  }
}

onMounted(loadProcesses)
</script>

<template>
  <div class="manage-page">
    <div class="toolbar-row">
      <el-button v-permission="'process:create'" type="primary" @click="openCreate">新增流程</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="流程名称">
          <el-input v-model="queryForm.processName" placeholder="请输入流程名称" clearable />
        </el-form-item>
        <el-form-item label="流程编码">
          <el-input v-model="queryForm.processCode" placeholder="请输入流程编码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择状态" clearable style="width: 220px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
        <el-table-column prop="processCode" label="流程编码" min-width="150" />
        <el-table-column prop="processName" label="流程名称" min-width="160" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="stepCount" label="步骤数" min-width="90" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type" effect="light">
              {{ getStatusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="版本状态" min-width="130">
          <template #default="{ row }">
            <el-tag :type="getPublishStatusMeta(row.publishStatus).type" effect="light">
              {{ getPublishStatusMeta(row.publishStatus).text }}
            </el-tag>
            <span class="version-no" v-if="row.publishedVersionNo">v{{ row.publishedVersionNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="340" fixed="right">
          <template #default="{ row }">
            <button v-permission="'process:view'" class="text-action primary" @click="openDetail(row)">查看</button>
            <button v-permission="'process:update'" class="text-action primary" @click="openEdit(row)">编辑</button>
            <button v-permission="'process:design'" class="text-action warning" @click="openDesign(row)">设计流程</button>
            <button v-permission="'process:design'" class="text-action success" @click="handlePublish(row)">发布</button>
            <button v-if="row.publishStatus === 1" v-permission="'process:design'" class="text-action" @click="handleDisableVersion(row)">停用版本</button>
            <button v-permission="'process:delete'" class="text-action danger" @click="handleDelete(row)">删除</button>
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
          @change="loadProcesses"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="90px">
        <el-form-item label="流程编码" prop="processCode">
          <el-input v-model="editForm.processCode" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="流程名称" prop="processName">
          <el-input v-model="editForm.processName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="流程详情" width="760px" destroy-on-close>
      <div class="detail-grid">
        <div class="detail-row">
          <span class="detail-label">流程编码</span>
          <span>{{ detailData.processCode || '--' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">流程名称</span>
          <span>{{ detailData.processName || '--' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <span>
            <el-tag :type="getStatusMeta(detailData.status).type" effect="light">
              {{ getStatusMeta(detailData.status).text }}
            </el-tag>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">步骤数</span>
          <span>{{ detailData.stepCount ?? 0 }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">版本状态</span>
          <span>
            <el-tag :type="getPublishStatusMeta(detailData.publishStatus).type" effect="light">
              {{ getPublishStatusMeta(detailData.publishStatus).text }}
            </el-tag>
          </span>
        </div>
        <div class="detail-row">
          <span class="detail-label">发布版本</span>
          <span>{{ detailData.publishedVersionNo ? `v${detailData.publishedVersionNo}` : '--' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span>{{ formatDateTime(detailData.createTime) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">更新时间</span>
          <span>{{ formatDateTime(detailData.updateTime) }}</span>
        </div>
        <div class="detail-row detail-row-full">
          <span class="detail-label">描述</span>
          <span>{{ detailData.description || '--' }}</span>
        </div>
      </div>

      <div class="step-list" v-if="detailData.steps?.length">
        <div class="step-list-title">步骤列表</div>
        <el-table :data="detailData.steps" stripe>
          <el-table-column prop="stepNo" label="步骤号" width="90" />
          <el-table-column prop="stepName" label="步骤名称" min-width="160" />
          <el-table-column prop="stepType" label="步骤类型" min-width="120" />
          <el-table-column prop="scriptLang" label="脚本语言" min-width="120" />
          <el-table-column label="更新时间" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updateTime) }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="designVisible"
      title="流程设计"
      width="96%"
      top="2vh"
      destroy-on-close
      class="process-design-dialog"
    >
      <div class="process-design" v-loading="designLoading">
        <div class="process-design-toolbar">
          <div class="design-version-info">
            <el-tag :type="getPublishStatusMeta(detailData.publishStatus).type" effect="light">
              {{ getPublishStatusMeta(detailData.publishStatus).text }}
            </el-tag>
            <span class="version-no" v-if="designVersionNo">当前发布版本 v{{ designVersionNo }}</span>
          </div>
          <div class="design-toolbar-actions">
            <el-button v-permission="'process:design'" type="primary" @click="addStep">添加步骤</el-button>
            <el-button v-permission="'process:design'" @click="submitDesign()">保存草稿</el-button>
            <el-button v-permission="'process:design'" type="success" :loading="publishLoading" @click="publishCurrentDesign">
              <el-icon><Upload /></el-icon>
              发布版本
            </el-button>
            <el-button
              v-if="detailData.publishStatus === 1"
              v-permission="'process:design'"
              :loading="publishLoading"
              @click="disableCurrentVersion"
            >
              <el-icon><CircleClose /></el-icon>
              停用版本
            </el-button>
          </div>
        </div>

        <div class="version-strip" v-if="detailData.versions?.length">
          <span class="version-strip-title">版本记录</span>
          <div v-for="version in detailData.versions" :key="version.id" class="version-chip">
            <span>v{{ version.versionNo }}</span>
            <el-tag :type="getVersionStatusMeta(version.versionStatus).type" effect="light" size="small">
              {{ getVersionStatusMeta(version.versionStatus).text }}
            </el-tag>
            <span>{{ formatDateTime(version.publishTime) }}</span>
          </div>
        </div>

        <div class="process-design-stage">
          <div
            v-for="(step, index) in designForm.steps"
            :key="step.id || `${step.stepNo}-${index}`"
            class="process-step-card"
            :class="{ active: index === activeStepIndex }"
            @click="selectStep(index)"
          >
            <div class="process-step-main">
              <div class="process-step-head">
                <div class="process-step-left">
                  <div class="process-step-badge">{{ step.stepNo }}</div>
                  <div class="process-step-title">{{ step.stepName || `步骤${step.stepNo}` }}</div>
                </div>
                <div class="step-card-actions">
                  <button class="icon-action" :disabled="index === 0" title="上移" @click.stop="moveStep(index, -1)">
                    <el-icon><ArrowUp /></el-icon>
                  </button>
                  <button
                    class="icon-action"
                    :disabled="index === designForm.steps.length - 1"
                    title="下移"
                    @click.stop="moveStep(index, 1)"
                  >
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <button class="icon-delete" title="删除" @click.stop="removeStep(index)">
                    <el-icon><Delete /></el-icon>
                  </button>
                </div>
              </div>

              <div class="process-step-meta">
                {{ step.scriptLang }} {{ getStepTypeTag(step.stepType) }} · {{ step.timeoutSeconds || 60 }}s ·
                {{ getFailureStrategyLabel(step.failureStrategy) }}
              </div>
              <div class="process-step-preview">{{ getStepPreview(step.scriptContent) }}</div>
            </div>

            <div class="process-step-arrow">
              <el-icon><ArrowDown /></el-icon>
            </div>
          </div>

          <el-empty v-if="!designForm.steps.length" description="暂无步骤，请先添加步骤" />
        </div>

        <div class="process-design-config">
          <div class="config-title-wrap">
            <div class="config-divider"></div>
            <div class="config-title">步骤配置</div>
            <div class="config-divider"></div>
          </div>

          <template v-if="activeStep">
            <el-form label-width="88px" class="config-form">
              <el-form-item label="步骤名称">
                <el-input v-model="activeStep.stepName" placeholder="请输入步骤名称" />
              </el-form-item>
              <el-form-item label="步骤类型">
                <el-select v-model="activeStep.stepType" style="width: 220px">
                  <el-option
                    v-for="item in stepTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="脚本语言">
                <el-select v-model="activeStep.scriptLang" style="width: 220px">
                  <el-option
                    v-for="item in scriptLangOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="超时时间">
                <el-input-number v-model="activeStep.timeoutSeconds" :min="1" :max="3600" :step="10" controls-position="right" />
                <span class="form-unit">秒</span>
              </el-form-item>
              <el-form-item label="失败策略">
                <el-select v-model="activeStep.failureStrategy" style="width: 220px">
                  <el-option
                    v-for="item in failureStrategyOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item :label="`${activeStep.scriptLang || '脚本'}代码`">
                <el-input
                  v-model="activeStep.scriptContent"
                  class="script-editor"
                  type="textarea"
                  :rows="16"
                  resize="vertical"
                  placeholder="请输入脚本内容"
                />
              </el-form-item>
            </el-form>
          </template>
          <el-empty v-else description="请选择一个步骤进行配置" />
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="designVisible = false">取消</el-button>
          <el-button v-permission="'process:design'" type="primary" @click="submitDesign()">保存草稿</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.step-list {
  margin-top: 20px;
}

.step-list-title {
  margin-bottom: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.process-design {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-height: 70vh;
}

.process-design-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e8edf5;
}

.design-version-info,
.design-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.version-no {
  margin-left: 8px;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.version-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 12px 14px;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  background: #fbfdff;
}

.version-strip-title {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.version-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 9px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
}

.process-design-stage {
  padding: 22px;
  border-radius: 8px;
  background: #f7f9fc;
}

.process-step-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 148px;
  padding: 18px 20px;
  margin-bottom: 22px;
  border: 2px solid #d9e1ee;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.04);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.process-step-card:last-child {
  margin-bottom: 0;
}

.process-step-card.active {
  border-color: #3b82f6;
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.14);
}

.process-step-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.process-step-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.process-step-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #3b82f6;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
}

.process-step-title {
  font-size: 18px;
  font-weight: 700;
  color: #374151;
}

.process-step-meta {
  display: flex;
  flex-wrap: wrap;
  align-self: flex-start;
  margin: 14px 0 12px 40px;
  padding: 4px 10px;
  border-radius: 8px;
  background: #fff1f2;
  color: #fb7185;
  font-size: 13px;
  font-weight: 600;
}

.process-step-preview {
  margin-left: 40px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.7;
}

.process-step-arrow {
  display: flex;
  justify-content: center;
  margin-top: 10px;
  color: #3b82f6;
  font-size: 20px;
}

.step-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-action,
.icon-delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 18px;
  cursor: pointer;
}

.icon-action:disabled {
  color: #cbd5e1;
  cursor: not-allowed;
}

.icon-delete {
  color: #ef4444;
}

.process-design-config {
  padding: 24px 20px 8px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px #eef2f7;
}

.config-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.config-divider {
  flex: 1;
  height: 1px;
  background: #dbe3ef;
}

.config-title {
  padding: 0 18px;
  color: #374151;
  font-size: 16px;
  font-weight: 700;
}

.config-form {
  max-width: 100%;
}

.form-unit {
  margin-left: 10px;
  color: #64748b;
  font-size: 13px;
}

:deep(.script-editor .el-textarea__inner) {
  min-height: 360px !important;
  padding: 14px 16px;
  border-color: #cbd5e1;
  background: #0f172a;
  color: #dbeafe;
  font-family: "Cascadia Code", Consolas, "Courier New", monospace;
  font-size: 14px;
  line-height: 1.65;
  tab-size: 2;
}

:deep(.process-design-dialog .el-dialog__body) {
  padding-top: 12px;
}
</style>
