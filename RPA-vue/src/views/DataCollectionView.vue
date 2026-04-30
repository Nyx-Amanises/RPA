<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCollectionRecord,
  fetchCollectionDetail,
  fetchCollectionPage,
  removeCollectionRecord
} from '../api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const tableData = ref([])
const detailData = ref({})
const timeRange = ref([])
const formRef = ref()

const summary = reactive({
  total: 0,
  success: 0,
  processing: 0,
  failed: 0
})

const queryForm = reactive({
  taskId: '',
  keyword: '',
  status: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const editForm = reactive({
  taskId: 1,
  taxpayerIdNo: '',
  enterpriseName: '',
  sourceName: '',
  status: 2,
  rawDataText: '',
  errorMessage: ''
})

const rules = {
  taskId: [{ required: true, message: '请输入任务ID', trigger: 'blur' }],
  taxpayerIdNo: [{ required: true, message: '请输入纳税人识别号', trigger: 'blur' }],
  enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  sourceName: [{ required: true, message: '请输入数据来源', trigger: 'blur' }],
  rawDataText: [{ required: true, message: '请输入原始数据', trigger: 'blur' }]
}

const statusOptions = [
  { label: '待处理', value: 0 },
  { label: '采集中', value: 1 },
  { label: '成功', value: 2 },
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
    return { text: '采集中', type: 'warning' }
  }
  if (status === 2) {
    return { text: '成功', type: 'success' }
  }
  if (status === 3) {
    return { text: '失败', type: 'danger' }
  }
  return { text: '待处理', type: 'info' }
}

function buildQueryParams() {
  return {
    taskId: queryForm.taskId,
    keyword: queryForm.keyword,
    status: queryForm.status,
    startTime: timeRange.value?.[0] || '',
    endTime: timeRange.value?.[1] || '',
    pageNum: pager.pageNum,
    pageSize: pager.pageSize
  }
}

async function loadCollectionPage() {
  loading.value = true
  try {
    const res = await fetchCollectionPage(buildQueryParams())
    Object.assign(summary, res.data?.summary || {})
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '采集数据列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadCollectionPage()
}

function handleReset() {
  Object.assign(queryForm, { taskId: '', keyword: '', status: '' })
  timeRange.value = []
  handleSearch()
}

function openCreate() {
  dialogVisible.value = true
  Object.assign(editForm, {
    taskId: 1,
    taxpayerIdNo: '',
    enterpriseName: '',
    sourceName: '',
    status: 2,
    rawDataText: '',
    errorMessage: ''
  })
}

async function openDetail(row) {
  detailVisible.value = true
  detailData.value = {}
  try {
    const res = await fetchCollectionDetail(row.id)
    detailData.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '采集详情加载失败')
    detailVisible.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除采集记录 ${row.id} 吗？`, '提示', { type: 'warning' })
    await removeCollectionRecord(row.id)
    ElMessage.success('采集记录已删除')
    loadCollectionPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除采集记录失败')
  }
}

async function submitForm() {
  try {
    await formRef.value.validate()
    let rawData
    try {
      rawData = JSON.parse(editForm.rawDataText)
    } catch {
      ElMessage.error('原始数据必须是合法的 JSON')
      return
    }

    await createCollectionRecord({
      taskId: editForm.taskId,
      taxpayerIdNo: editForm.taxpayerIdNo,
      enterpriseName: editForm.enterpriseName,
      sourceName: editForm.sourceName,
      status: editForm.status,
      rawData,
      errorMessage: editForm.errorMessage
    })
    ElMessage.success('采集记录新增成功')
    dialogVisible.value = false
    loadCollectionPage()
  } catch (error) {
    ElMessage.error(error.message || '采集记录新增失败')
  }
}

function prettyRawData(data) {
  if (!data) {
    return ''
  }
  return JSON.stringify(data, null, 2)
}

onMounted(loadCollectionPage)
</script>

<template>
  <div class="manage-page">
    <div class="collection-overview">
      <el-card shadow="never" class="overview-card">
        <div class="overview-value">{{ summary.total }}</div>
        <div class="overview-label">总采集数</div>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <div class="overview-value success">{{ summary.success }}</div>
        <div class="overview-label">成功</div>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <div class="overview-value warning">{{ summary.processing }}</div>
        <div class="overview-label">采集中</div>
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
        <el-form-item label="关键字">
          <el-input v-model="queryForm.keyword" placeholder="纳税人识别号/企业名称" clearable style="width: 250px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="请选择" clearable style="width: 220px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="采集时间">
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
          <el-button v-permission="'data:collection:create'" type="success" @click="openCreate">新增</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column type="index" label="序号" width="72" />
        <el-table-column prop="taskId" label="任务ID" min-width="140" />
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type" effect="light">
              {{ getStatusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taxpayerIdNo" label="纳税人识别号" min-width="220" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="220" />
        <el-table-column prop="sourceName" label="数据来源" min-width="170" />
        <el-table-column label="采集时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.collectionTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <button v-permission="'data:collection:view'" class="text-action primary" @click="openDetail(row)">查看</button>
            <button v-permission="'data:collection:delete'" class="text-action danger" @click="handleDelete(row)">删除</button>
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
          @change="loadCollectionPage"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增采集记录" width="780px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="任务ID" prop="taskId">
          <el-input-number v-model="editForm.taskId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="纳税人识别号" prop="taxpayerIdNo">
          <el-input v-model="editForm.taxpayerIdNo" placeholder="请输入纳税人识别号" />
        </el-form-item>
        <el-form-item label="企业名称" prop="enterpriseName">
          <el-input v-model="editForm.enterpriseName" placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="数据来源" prop="sourceName">
          <el-input v-model="editForm.sourceName" placeholder="请输入数据来源" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="原始数据" prop="rawDataText">
          <el-input
            v-model="editForm.rawDataText"
            type="textarea"
            :rows="6"
            placeholder="请输入原始数据（JSON格式）"
          />
        </el-form-item>
        <el-form-item label="错误信息">
          <el-input v-model="editForm.errorMessage" type="textarea" :rows="3" placeholder="请输入错误信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="采集数据详情" width="1200px" destroy-on-close>
      <div class="detail-table">
        <div class="detail-item"><span>任务ID</span><strong>{{ detailData.taskId || '--' }}</strong></div>
        <div class="detail-item"><span>纳税人识别号</span><strong>{{ detailData.taxpayerIdNo || '--' }}</strong></div>
        <div class="detail-item"><span>企业名称</span><strong>{{ detailData.enterpriseName || '--' }}</strong></div>
        <div class="detail-item">
          <span>状态</span>
          <strong><el-tag :type="getStatusMeta(detailData.status).type" effect="light">{{ getStatusMeta(detailData.status).text }}</el-tag></strong>
        </div>
        <div class="detail-item"><span>数据来源</span><strong>{{ detailData.sourceName || '--' }}</strong></div>
        <div class="detail-item"><span>采集时间</span><strong>{{ formatDateTime(detailData.collectionTime) }}</strong></div>
        <div class="detail-item full"><span>错误信息</span><strong>{{ detailData.errorMessage || '-' }}</strong></div>
      </div>

      <div class="raw-data-section">
        <div class="raw-data-title">原始数据</div>
        <pre class="raw-data-box">{{ prettyRawData(detailData.rawData) }}</pre>
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
.collection-overview {
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

.raw-data-section {
  padding-top: 8px;
}

.raw-data-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #334155;
}

.raw-data-box {
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
