<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createRobot,
  fetchRobotDetail,
  fetchRobotOverview,
  fetchRobotPage,
  removeRobot,
  updateRobot
} from '../api/system'

const loading = ref(false)
const overviewLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogMode = ref('create')
const tableData = ref([])
const detailData = ref({})
const total = ref(0)
const formRef = ref()

const overview = reactive({
  totalCount: 0,
  onlineCount: 0,
  workingCount: 0,
  offlineCount: 0
})

const queryForm = reactive({
  robotName: '',
  robotCode: '',
  status: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const editForm = reactive({
  id: null,
  robotCode: '',
  robotName: '',
  robotType: '',
  description: '',
  status: 1
})

const rules = {
  robotCode: [{ required: true, message: '请输入机器人编码', trigger: 'blur' }],
  robotName: [{ required: true, message: '请输入机器人名称', trigger: 'blur' }],
  robotType: [{ required: true, message: '请输入机器人类型', trigger: 'blur' }]
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增机器人' : '编辑机器人'))

const statusOptions = [
  { label: '离线', value: 0 },
  { label: '在线', value: 1 },
  { label: '工作中', value: 2 }
]

function getStatusMeta(status) {
  if (status === 1) {
    return { text: '在线', type: 'success' }
  }
  if (status === 2) {
    return { text: '工作中', type: 'warning' }
  }
  return { text: '离线', type: 'danger' }
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

async function loadOverview() {
  overviewLoading.value = true
  try {
    const res = await fetchRobotOverview()
    Object.assign(overview, res.data || {})
  } catch (error) {
    ElMessage.error(error.message || '机器人统计加载失败')
  } finally {
    overviewLoading.value = false
  }
}

async function loadRobots() {
  loading.value = true
  try {
    const res = await fetchRobotPage({ ...queryForm, ...pager })
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '机器人列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadRobots()
}

function handleReset() {
  Object.assign(queryForm, { robotName: '', robotCode: '', status: '' })
  handleSearch()
}

function openCreate() {
  dialogMode.value = 'create'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: null,
    robotCode: '',
    robotName: '',
    robotType: '',
    description: '',
    status: 1
  })
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  dialogVisible.value = true
  try {
    const res = await fetchRobotDetail(row.id)
    Object.assign(editForm, {
      id: res.data?.id ?? row.id,
      robotCode: res.data?.robotCode ?? row.robotCode,
      robotName: res.data?.robotName ?? row.robotName,
      robotType: res.data?.robotType ?? row.robotType,
      description: res.data?.description ?? row.description ?? '',
      status: res.data?.status ?? row.status
    })
  } catch (error) {
    ElMessage.error(error.message || '机器人详情加载失败')
    dialogVisible.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detailData.value = {}
  try {
    const res = await fetchRobotDetail(row.id)
    detailData.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '机器人详情加载失败')
    detailVisible.value = false
  }
}

async function submitForm() {
  try {
    await formRef.value.validate()
    const payload = {
      robotCode: editForm.robotCode,
      robotName: editForm.robotName,
      robotType: editForm.robotType,
      description: editForm.description,
      status: editForm.status
    }
    if (dialogMode.value === 'create') {
      await createRobot(payload)
      ElMessage.success('新增机器人成功')
    } else {
      await updateRobot(editForm.id, payload)
      ElMessage.success('修改机器人成功')
    }
    dialogVisible.value = false
    await Promise.all([loadOverview(), loadRobots()])
  } catch (error) {
    ElMessage.error(error.message || (dialogMode.value === 'create' ? '新增机器人失败' : '修改机器人失败'))
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除机器人 ${row.robotName} 吗？`, '提示', { type: 'warning' })
    await removeRobot(row.id)
    ElMessage.success('机器人已删除')
    await Promise.all([loadOverview(), loadRobots()])
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除机器人失败')
  }
}

onMounted(async () => {
  await Promise.all([loadOverview(), loadRobots()])
})
</script>

<template>
  <div class="manage-page">
    <div class="robot-overview" v-loading="overviewLoading">
      <el-card shadow="never" class="overview-card">
        <div class="overview-value">{{ overview.totalCount }}</div>
        <div class="overview-label">总机器人数</div>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <div class="overview-value success">{{ overview.onlineCount }}</div>
        <div class="overview-label">在线</div>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <div class="overview-value warning">{{ overview.workingCount }}</div>
        <div class="overview-label">工作中</div>
      </el-card>
      <el-card shadow="never" class="overview-card">
        <div class="overview-value danger">{{ overview.offlineCount }}</div>
        <div class="overview-label">离线</div>
      </el-card>
    </div>

    <div class="toolbar-row">
      <el-button v-permission="'robot:create'" type="primary" @click="openCreate">新增机器人</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="机器人名称">
          <el-input v-model="queryForm.robotName" placeholder="请输入机器人名称" clearable />
        </el-form-item>
        <el-form-item label="机器人编码">
          <el-input v-model="queryForm.robotCode" placeholder="请输入机器人编码" clearable />
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
        <el-table-column prop="robotCode" label="机器人编码" min-width="160" />
        <el-table-column prop="robotName" label="机器人名称" min-width="160" />
        <el-table-column prop="robotType" label="类型" min-width="140" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusMeta(row.status).type" effect="light">
              {{ getStatusMeta(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前任务ID" min-width="140">
          <template #default="{ row }">
            {{ row.currentTaskId || row.currentTaskCode || '空闲' }}
          </template>
        </el-table-column>
        <el-table-column label="最后心跳" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.lastHeartbeatTime) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="200">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <button v-permission="'robot:view'" class="text-action primary" @click="openDetail(row)">查看</button>
            <button v-permission="'robot:update'" class="text-action primary" @click="openEdit(row)">编辑</button>
            <button v-permission="'robot:delete'" class="text-action danger" @click="handleDelete(row)">删除</button>
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
          @change="loadRobots"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="机器人编码" prop="robotCode">
          <el-input v-model="editForm.robotCode" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="机器人名称" prop="robotName">
          <el-input v-model="editForm.robotName" />
        </el-form-item>
        <el-form-item label="类型" prop="robotType">
          <el-input v-model="editForm.robotType" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">在线</el-radio>
            <el-radio :value="0">离线</el-radio>
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

    <el-dialog v-model="detailVisible" title="机器人详情" width="760px" destroy-on-close>
      <div class="detail-grid">
        <div class="detail-row">
          <span class="detail-label">机器人编码</span>
          <span>{{ detailData.robotCode || '--' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">机器人名称</span>
          <span>{{ detailData.robotName || '--' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <span>{{ detailData.robotType || '--' }}</span>
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
          <span class="detail-label">最后心跳</span>
          <span>{{ formatDateTime(detailData.lastHeartbeatTime) }}</span>
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
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
