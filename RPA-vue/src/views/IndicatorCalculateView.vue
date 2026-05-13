<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CirclePlus, Delete, Edit, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchTaskPage } from '../api/system'
import {
  calculateIndicator,
  createIndicator,
  fetchIndicatorDetail,
  fetchIndicatorPage,
  removeIndicator,
  updateIndicator
} from '../api/indicator'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const tableData = ref([])
const taskOptions = ref([])
const total = ref(0)

const queryForm = reactive({
  keyword: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const editForm = reactive({
  id: null,
  indicatorName: '',
  indicatorCode: '',
  resultVarName: '',
  indicatorLogic: '',
  formulaExpression: '',
  taskId: '',
  status: 1,
  remark: ''
})

const rules = {
  indicatorName: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
  indicatorCode: [{ required: true, message: '请输入指标编码', trigger: 'blur' }],
  indicatorLogic: [{ required: true, message: '请输入指标逻辑说明', trigger: 'blur' }],
  formulaExpression: [{ required: true, message: '请输入指标计算公式', trigger: 'blur' }],
  taskId: [{ required: true, message: '请选择数据来源任务ID', trigger: 'change' }]
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增指标' : '编辑指标'))

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').split('.')[0]
}

function buildQueryParams() {
  return {
    keyword: queryForm.keyword,
    pageNum: pager.pageNum,
    pageSize: pager.pageSize
  }
}

async function loadPage() {
  loading.value = true
  try {
    const res = await fetchIndicatorPage(buildQueryParams())
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '指标列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadTaskOptions() {
  try {
    const res = await fetchTaskPage({ pageNum: 1, pageSize: 100 }, { permissionScope: 'action' })
    taskOptions.value = res.data?.list || []
  } catch (error) {
    ElMessage.error(error.message || '任务列表加载失败')
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadPage()
}

function handleReset() {
  queryForm.keyword = ''
  handleSearch()
}

function resetForm() {
  Object.assign(editForm, {
    id: null,
    indicatorName: '',
    indicatorCode: '',
    resultVarName: '',
    indicatorLogic: '',
    formulaExpression: '',
    taskId: '',
    status: 1,
    remark: ''
  })
}

async function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
  if (!taskOptions.value.length) {
    await loadTaskOptions()
  }
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  dialogVisible.value = true
  if (!taskOptions.value.length) {
    await loadTaskOptions()
  }
  try {
    const res = await fetchIndicatorDetail(row.id)
    Object.assign(editForm, {
      id: res.data?.id,
      indicatorName: res.data?.indicatorName || '',
      indicatorCode: res.data?.indicatorCode || '',
      resultVarName: res.data?.resultVarName || '',
      indicatorLogic: res.data?.indicatorLogic || '',
      formulaExpression: res.data?.formulaExpression || '',
      taskId: res.data?.taskId || '',
      status: res.data?.status ?? 1,
      remark: res.data?.remark || ''
    })
  } catch (error) {
    ElMessage.error(error.message || '指标详情加载失败')
    dialogVisible.value = false
  }
}

async function submitForm() {
  try {
    await formRef.value.validate()
    const payload = {
      indicatorName: editForm.indicatorName,
      indicatorCode: editForm.indicatorCode,
      resultVarName: editForm.resultVarName,
      indicatorLogic: editForm.indicatorLogic,
      formulaExpression: editForm.formulaExpression,
      taskId: editForm.taskId,
      status: editForm.status,
      remark: editForm.remark
    }
    if (dialogMode.value === 'create') {
      await createIndicator(payload)
      ElMessage.success('指标已新增')
    } else {
      await updateIndicator(editForm.id, payload)
      ElMessage.success('指标已保存')
    }
    dialogVisible.value = false
    loadPage()
  } catch (error) {
    ElMessage.error(error.message || '指标保存失败')
  }
}

async function handleCalculate(row) {
  try {
    const res = await calculateIndicator(row.id)
    ElMessage.success(`计算完成：${res.data?.displayValue || ''}`.trim())
    loadPage()
  } catch (error) {
    ElMessage.error(error.message || '指标计算失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除指标 ${row.indicatorName} 吗？`, '提示', { type: 'warning' })
    await removeIndicator(row.id)
    ElMessage.success('指标已删除')
    loadPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除指标失败')
  }
}

onMounted(async () => {
  await Promise.all([loadPage(), loadTaskOptions()])
})
</script>

<template>
  <div class="manage-page indicator-page">
    <div class="toolbar-row">
      <el-button type="primary" :icon="CirclePlus" @click="openCreate">新增指标</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="指标名称/编码">
          <el-input v-model="queryForm.keyword" placeholder="请输入指标名称或编码" clearable style="width: 280px" />
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
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="indicatorName" label="指标名称" min-width="160" />
        <el-table-column prop="indicatorCode" label="指标编码" min-width="180" />
        <el-table-column prop="taskId" label="任务ID" width="110" />
        <el-table-column prop="indicatorLogic" label="指标逻辑" min-width="260" show-overflow-tooltip />
        <el-table-column label="最近结果" min-width="150">
          <template #default="{ row }">
            <el-tag v-if="row.latestResult" type="success" effect="light">{{ row.latestResult }}</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="最近计算时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.latestCalculateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="RefreshRight" @click="handleCalculate(row)">计算</el-button>
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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
          @change="loadPage"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="120px">
        <el-form-item label="指标名称" prop="indicatorName">
          <el-input v-model="editForm.indicatorName" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="指标编码" prop="indicatorCode">
          <el-input v-model="editForm.indicatorCode" placeholder="请输入指标编码" />
        </el-form-item>
        <el-form-item label="结果变量名">
          <el-input v-model="editForm.resultVarName" placeholder="如 taxRate，可为空" />
        </el-form-item>
        <el-form-item label="指标逻辑" prop="indicatorLogic">
          <el-input v-model="editForm.indicatorLogic" type="textarea" :rows="3" placeholder="请输入展示用指标逻辑说明" />
        </el-form-item>
        <el-form-item label="数据来源" prop="taskId">
          <el-select v-model="editForm.taskId" placeholder="请选择任务ID" style="width: 100%">
            <el-option
              v-for="item in taskOptions"
              :key="item.id"
              :label="`${item.id} - ${item.taskName || item.taskCode}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计算公式" prop="formulaExpression">
          <el-input
            v-model="editForm.formulaExpression"
            type="textarea"
            :rows="5"
            placeholder="SALE_JSHJ_SUM = sum(invoices where sign == '销项' and state == '正常' and monthDiffToAppDate between 1 and 12).jshj"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.indicator-page :deep(.el-textarea__inner) {
  font-family: Consolas, "Microsoft YaHei", monospace;
}
</style>
