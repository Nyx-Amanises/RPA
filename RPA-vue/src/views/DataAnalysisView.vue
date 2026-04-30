<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAnalysisDetail, fetchAnalysisPage, removeAnalysisRecord } from '../api/system'

const loading = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const tableData = ref([])
const detailData = ref({})
const timeRange = ref([])

const summary = reactive({
  total: 0,
  success: 0,
  processing: 0,
  failed: 0
})

const queryForm = reactive({
  taskId: '',
  status: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const statusOptions = [
  { label: '待处理', value: 0 },
  { label: '解析中', value: 1 },
  { label: '已解析', value: 2 },
  { label: '失败', value: 3 }
]

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
  if (status === 1) {
    return { text: '解析中', type: 'warning' }
  }
  if (status === 2) {
    return { text: '已解析', type: 'success' }
  }
  if (status === 3) {
    return { text: '失败', type: 'danger' }
  }
  return { text: '待处理', type: 'info' }
}

function buildQueryParams() {
  return {
    taskId: queryForm.taskId,
    status: queryForm.status,
    startTime: timeRange.value?.[0] || '',
    endTime: timeRange.value?.[1] || '',
    pageNum: pager.pageNum,
    pageSize: pager.pageSize
  }
}

async function loadAnalysisPage() {
  loading.value = true
  try {
    const res = await fetchAnalysisPage(buildQueryParams())
    Object.assign(summary, res.data?.summary || {})
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '解析数据列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadAnalysisPage()
}

function handleReset() {
  Object.assign(queryForm, { taskId: '', status: '' })
  timeRange.value = []
  handleSearch()
}

async function openDetail(row) {
  detailVisible.value = true
  detailData.value = {}
  try {
    const res = await fetchAnalysisDetail(row.id)
    detailData.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '解析详情加载失败')
    detailVisible.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除解析记录 ${row.id} 吗？`, '提示', { type: 'warning' })
    await removeAnalysisRecord(row.id)
    ElMessage.success('解析记录已删除')
    loadAnalysisPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除解析记录失败')
  }
}

function prettyParsedData(data) {
  if (!data) {
    return ''
  }
  return JSON.stringify(data, null, 2)
}

onMounted(loadAnalysisPage)
</script>

<template>
  <div class="manage-page">
    <div class="analysis-overview">
      <el-card shadow="never" class="overview-card">
        <div class="overview-value">{{ summary.total }}</div>
        <div class="overview-label">总解析数</div>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <div class="overview-value success">{{ summary.success }}</div>
        <div class="overview-label">成功</div>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <div class="overview-value warning">{{ summary.processing }}</div>
        <div class="overview-label">解析中</div>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <div class="overview-value danger">{{ summary.failed }}</div>
        <div class="overview-label">失败</div>
      </el-card>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="任务ID">
          <el-input v-model="queryForm.taskId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable style="width: 220px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="到"
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
        <el-table-column prop="taskId" label="任务ID" min-width="160" />
        <el-table-column prop="collectionId" label="采集ID" min-width="180" />
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type" effect="light">
              {{ getStatusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="extractedFieldCount" label="提取字段数" min-width="120" />
        <el-table-column prop="ruleName" label="解析规则" min-width="180" />
        <el-table-column label="解析时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.analysisTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <button v-permission="'data:analysis:view'" class="text-action primary" @click="openDetail(row)">查看</button>
            <button v-permission="'data:analysis:delete'" class="text-action danger" @click="handleDelete(row)">删除</button>
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
          @change="loadAnalysisPage"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="解析数据详情" width="1200px" destroy-on-close>
      <div class="detail-table">
        <div class="detail-item"><span>任务ID</span><strong>{{ detailData.taskId || '--' }}</strong></div>
        <div class="detail-item"><span>采集ID</span><strong>{{ detailData.collectionId || '--' }}</strong></div>
        <div class="detail-item"><span>纳税人识别号</span><strong>{{ detailData.taxpayerIdNo || '--' }}</strong></div>
        <div class="detail-item"><span>企业名称</span><strong>{{ detailData.enterpriseName || '--' }}</strong></div>
        <div class="detail-item">
          <span>状态</span>
          <strong><el-tag :type="getStatusMeta(detailData.status).type" effect="light">{{ getStatusMeta(detailData.status).text }}</el-tag></strong>
        </div>
        <div class="detail-item"><span>解析时间</span><strong>{{ formatDateTime(detailData.analysisTime) }}</strong></div>
        <div class="detail-item full"><span>错误信息</span><strong>{{ detailData.errorMessage || '-' }}</strong></div>
      </div>

      <div class="parsed-data-section">
        <div class="parsed-data-title">解析数据</div>
        <pre class="parsed-data-box">{{ prettyParsedData(detailData.parsedData) }}</pre>
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
.analysis-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.overview-card {
  border-radius: 16px;
}

.overview-value {
  padding-top: 10px;
  text-align: center;
  font-size: 56px;
  font-weight: 700;
  color: #1f2937;
}

.overview-value.success {
  color: #67c23a;
}

.overview-value.warning {
  color: #e6a23c;
}

.overview-value.danger {
  color: #f56c6c;
}

.overview-label {
  padding-bottom: 16px;
  text-align: center;
  color: #475569;
  font-size: 16px;
}

.detail-table {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 22px;
  box-shadow: inset 0 0 0 1px #edf2f7;
}

.detail-item {
  display: grid;
  grid-template-columns: 150px 1fr;
  min-height: 50px;
}

.detail-item.full {
  grid-column: 1 / -1;
}

.detail-item span {
  display: flex;
  align-items: center;
  padding: 0 14px;
  background: #f7f9fc;
  color: #475569;
  font-weight: 600;
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

.parsed-data-section {
  padding-top: 8px;
}

.parsed-data-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #334155;
}

.parsed-data-box {
  min-height: 180px;
  padding: 18px;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: inset 0 0 0 1px #e2e8f0;
}
</style>
