<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchBusinessDataDetail, fetchBusinessDataPage, removeBusinessDataRecord } from '../api/system'

const loading = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const tableData = ref([])
const detailData = ref({})
const timeRange = ref([])

const queryForm = reactive({
  keyword: '',
  taskId: '',
  taxAreaId: '',
  dataStatus: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const statusOptions = [
  { label: '采集中', value: 0 },
  { label: '成功', value: 1 },
  { label: '失败', value: 2 }
]

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  if (typeof value !== 'string') {
    return String(value)
  }
  return value.replace('T', ' ').split('.')[0]
}

function getStatusMeta(status) {
  if (status === 0) {
    return { text: '采集中', type: 'warning' }
  }
  if (status === 1) {
    return { text: '成功', type: 'success' }
  }
  if (status === 2) {
    return { text: '失败', type: 'danger' }
  }
  return { text: '--', type: 'info' }
}

function buildQueryParams() {
  return {
    keyword: queryForm.keyword,
    taskId: queryForm.taskId,
    taxAreaId: queryForm.taxAreaId,
    dataStatus: queryForm.dataStatus,
    startTime: timeRange.value?.[0] || '',
    endTime: timeRange.value?.[1] || '',
    pageNum: pager.pageNum,
    pageSize: pager.pageSize
  }
}

async function loadBusinessDataPage() {
  loading.value = true
  try {
    const res = await fetchBusinessDataPage(buildQueryParams())
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '业务数据列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadBusinessDataPage()
}

function handleReset() {
  Object.assign(queryForm, { keyword: '', taskId: '', taxAreaId: '', dataStatus: '' })
  timeRange.value = []
  handleSearch()
}

async function openDetail(row) {
  detailVisible.value = true
  detailData.value = {}
  try {
    const res = await fetchBusinessDataDetail(row.id)
    detailData.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '业务数据详情加载失败')
    detailVisible.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除业务数据 ${row.id} 吗？`, '提示', { type: 'warning' })
    await removeBusinessDataRecord(row.id)
    ElMessage.success('业务数据已删除')
    loadBusinessDataPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除业务数据失败')
  }
}

function prettyJson(data) {
  if (!data) {
    return ''
  }
  return JSON.stringify(data, null, 2)
}

onMounted(loadBusinessDataPage)
</script>

<template>
  <div class="manage-page">
    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="关键字">
          <el-input v-model="queryForm.keyword" placeholder="纳税人识别号/企业名称" clearable style="width: 250px" />
        </el-form-item>
        <el-form-item label="任务 ID">
          <el-input v-model="queryForm.taskId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="税区 ID">
          <el-input v-model="queryForm.taxAreaId" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="数据状态">
          <el-select v-model="queryForm.dataStatus" placeholder="请选择" clearable style="width: 220px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            range-separator="至"
            value-format="YYYY-MM-DD HH:mm:ss"
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
        <el-table-column type="selection" width="52" />
        <el-table-column type="index" label="序号" width="72" />
        <el-table-column prop="taskId" label="任务 ID" min-width="180" />
        <el-table-column prop="taxpayerIdNo" label="纳税人识别号" min-width="220" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="260" />
        <el-table-column prop="taxAreaId" label="税区 ID" min-width="120" />
        <el-table-column label="数据状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.dataStatus).type" effect="light">
              {{ getStatusMeta(row.dataStatus).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <button v-permission="'data:business:view'" class="text-action primary" @click="openDetail(row)">查看</button>
            <button v-permission="'data:business:delete'" class="text-action danger" @click="handleDelete(row)">删除</button>
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
          @change="loadBusinessDataPage"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="业务数据详情" width="1440px" destroy-on-close>
      <div class="detail-table">
        <div class="detail-item"><span>任务 ID</span><strong>{{ detailData.taskId || '--' }}</strong></div>
        <div class="detail-item"><span>税区 ID</span><strong>{{ detailData.taxAreaId || '--' }}</strong></div>
        <div class="detail-item"><span>纳税人识别号</span><strong>{{ detailData.taxpayerIdNo || '--' }}</strong></div>
        <div class="detail-item"><span>企业名称</span><strong>{{ detailData.enterpriseName || '--' }}</strong></div>
        <div class="detail-item">
          <span>数据状态</span>
          <strong>
            <el-tag :type="getStatusMeta(detailData.dataStatus).type" effect="light">
              {{ getStatusMeta(detailData.dataStatus).text }}
            </el-tag>
          </strong>
        </div>
        <div class="detail-item"><span>创建时间</span><strong>{{ formatDateTime(detailData.createTime) }}</strong></div>
      </div>

      <div class="business-data-section">
        <div class="business-data-title">业务数据</div>
        <pre class="business-data-box">{{ prettyJson(detailData.businessData) }}</pre>
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
  margin-bottom: 22px;
  box-shadow: inset 0 0 0 1px #edf2f7;
}

.detail-item {
  display: grid;
  grid-template-columns: 150px 1fr;
  min-height: 50px;
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

.business-data-section {
  padding-top: 8px;
}

.business-data-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #334155;
  text-align: center;
}

.business-data-box {
  min-height: 180px;
  margin: 0;
  padding: 18px;
  border-radius: 10px;
  background: #f8fafc;
  color: #475569;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-x: auto;
  box-shadow: inset 0 0 0 1px #e2e8f0;
}

@media (max-width: 1080px) {
  .detail-table {
    grid-template-columns: 1fr;
  }
}
</style>
