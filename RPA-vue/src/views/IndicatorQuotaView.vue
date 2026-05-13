<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { CirclePlus, Delete, Edit, Plus, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  calculateQuotaRule,
  createQuotaRule,
  fetchIndicatorOptions,
  fetchQuotaRuleDetail,
  fetchQuotaRulePage,
  removeQuotaRule,
  updateQuotaRule
} from '../api/indicator'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const tableData = ref([])
const indicatorOptions = ref([])
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
  quotaName: '',
  indicatorIds: [],
  resultVarName: '',
  outputTemplate: '',
  status: 1,
  remark: '',
  branches: []
})

const rules = {
  quotaName: [{ required: true, message: '请输入额度名称', trigger: 'blur' }],
  indicatorIds: [{ required: true, message: '请选择指标编码', trigger: 'change' }],
  resultVarName: [{ required: true, message: '请输入结果变量名称', trigger: 'blur' }],
  outputTemplate: [{ required: true, message: '请输入输出数据模板', trigger: 'blur' }]
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增指标额度计算' : '编辑指标额度计算'))

function buildQueryParams() {
  return {
    keyword: queryForm.keyword,
    pageNum: pager.pageNum,
    pageSize: pager.pageSize
  }
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').split('.')[0]
}

async function loadPage() {
  loading.value = true
  try {
    const res = await fetchQuotaRulePage(buildQueryParams())
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '指标额度计算列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadIndicatorOptions() {
  try {
    const res = await fetchIndicatorOptions({ permissionScope: 'action' })
    indicatorOptions.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '指标选项加载失败')
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

function createBranch(index = 0) {
  return {
    branchName: '',
    conditionExpression: '',
    calculationExpression: '',
    branchOrder: index + 1,
    defaultBranch: false,
    status: 1,
    remark: ''
  }
}

function resetForm() {
  Object.assign(editForm, {
    id: null,
    quotaName: '',
    indicatorIds: [],
    resultVarName: '',
    outputTemplate: '{"companyId":"${companyId}","creditLimit":"${creditLimit}","status":"approved"}',
    status: 1,
    remark: '',
    branches: [createBranch(0)]
  })
}

function addBranch() {
  editForm.branches.push(createBranch(editForm.branches.length))
}

function removeBranch(index) {
  if (editForm.branches.length <= 1) {
    ElMessage.warning('至少保留一个分支')
    return
  }
  editForm.branches.splice(index, 1)
  syncBranchOrder()
}

function syncBranchOrder() {
  editForm.branches.forEach((branch, index) => {
    branch.branchOrder = index + 1
  })
}

function normalizeBranches() {
  syncBranchOrder()
  return editForm.branches.map((branch) => ({
    branchName: branch.branchName,
    conditionExpression: branch.defaultBranch ? 'else' : branch.conditionExpression,
    calculationExpression: branch.calculationExpression,
    branchOrder: branch.branchOrder,
    defaultBranch: branch.defaultBranch,
    status: branch.status,
    remark: branch.remark
  }))
}

function validateBranches() {
  if (!editForm.branches.length) {
    ElMessage.warning('至少配置一个分支')
    return false
  }
  const defaultCount = editForm.branches.filter((branch) => branch.defaultBranch).length
  if (defaultCount > 1) {
    ElMessage.warning('默认分支最多只能有一个')
    return false
  }
  for (const branch of editForm.branches) {
    if (!branch.defaultBranch && !branch.conditionExpression?.trim()) {
      ElMessage.warning('非默认分支需要填写判断逻辑')
      return false
    }
    if (!branch.calculationExpression?.trim()) {
      ElMessage.warning('每个分支都需要填写额度计算公式')
      return false
    }
  }
  return true
}

async function openCreate() {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
  if (!indicatorOptions.value.length) {
    await loadIndicatorOptions()
  }
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  resetForm()
  dialogVisible.value = true
  if (!indicatorOptions.value.length) {
    await loadIndicatorOptions()
  }
  try {
    const res = await fetchQuotaRuleDetail(row.id)
    Object.assign(editForm, {
      id: res.data?.id,
      quotaName: res.data?.quotaName || '',
      indicatorIds: res.data?.indicatorIds || [],
      resultVarName: res.data?.resultVarName || '',
      outputTemplate: res.data?.outputTemplate || '',
      status: res.data?.status ?? 1,
      remark: res.data?.remark || '',
      branches: (res.data?.branches || []).map((branch, index) => ({
        branchName: branch.branchName || '',
        conditionExpression: branch.conditionExpression || '',
        calculationExpression: branch.calculationExpression || '',
        branchOrder: branch.branchOrder || index + 1,
        defaultBranch: Boolean(branch.defaultBranch),
        status: branch.status ?? 1,
        remark: branch.remark || ''
      }))
    })
    if (!editForm.branches.length) {
      editForm.branches = [createBranch(0)]
    }
  } catch (error) {
    ElMessage.error(error.message || '额度计算详情加载失败')
    dialogVisible.value = false
  }
}

async function submitForm() {
  try {
    await formRef.value.validate()
    if (!validateBranches()) {
      return
    }
    const payload = {
      quotaName: editForm.quotaName,
      indicatorIds: editForm.indicatorIds,
      resultVarName: editForm.resultVarName,
      outputTemplate: editForm.outputTemplate,
      status: editForm.status,
      remark: editForm.remark,
      branches: normalizeBranches()
    }
    if (dialogMode.value === 'create') {
      await createQuotaRule(payload)
      ElMessage.success('指标额度计算已新增')
    } else {
      await updateQuotaRule(editForm.id, payload)
      ElMessage.success('指标额度计算已保存')
    }
    dialogVisible.value = false
    loadPage()
  } catch (error) {
    ElMessage.error(error.message || '指标额度计算保存失败')
  }
}

async function handleCalculate(row) {
  try {
    const res = await calculateQuotaRule(row.id, {})
    ElMessage.success(`额度计算完成：${res.data?.displayValue || ''}`.trim())
    loadPage()
  } catch (error) {
    ElMessage.error(error.message || '额度计算失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除额度计算 ${row.quotaName} 吗？`, '提示', { type: 'warning' })
    await removeQuotaRule(row.id)
    ElMessage.success('额度计算已删除')
    loadPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除额度计算失败')
  }
}

function optionLabel(item) {
  return `${item.indicatorCode} - ${item.indicatorName}`
}

onMounted(async () => {
  await Promise.all([loadPage(), loadIndicatorOptions()])
})
</script>

<template>
  <div class="manage-page quota-page">
    <div class="toolbar-row">
      <el-button type="primary" :icon="CirclePlus" @click="openCreate">新增额度计算</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="额度名称">
          <el-input v-model="queryForm.keyword" placeholder="请输入额度名称" clearable style="width: 280px" />
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
        <el-table-column prop="quotaName" label="额度名称" min-width="170" />
        <el-table-column label="指标编码" min-width="250">
          <template #default="{ row }">
            <el-tag v-for="code in row.indicatorCodes" :key="code" class="code-tag" effect="light">{{ code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultVarName" label="结果变量名称" min-width="150" />
        <el-table-column label="额度计算结果" min-width="160">
          <template #default="{ row }">
            <el-tag v-if="row.latestResult" type="success" effect="light">{{ row.latestResult }}</el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column prop="outputTemplate" label="输出数据模板" min-width="260" show-overflow-tooltip />
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="960px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="120px">
        <el-form-item label="额度名称" prop="quotaName">
          <el-input v-model="editForm.quotaName" placeholder="请输入额度名称" />
        </el-form-item>
        <el-form-item label="指标编码" prop="indicatorIds">
          <el-select v-model="editForm.indicatorIds" multiple placeholder="请选择指标编码" style="width: 100%">
            <el-option
              v-for="item in indicatorOptions"
              :key="item.id"
              :label="optionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="判断逻辑">
          <div class="branch-list">
            <div v-for="(branch, index) in editForm.branches" :key="index" class="branch-row">
              <div class="branch-head">
                <el-input v-model="branch.branchName" placeholder="分支名称" class="branch-name" />
                <el-checkbox v-model="branch.defaultBranch">默认分支</el-checkbox>
                <el-button :icon="Delete" type="danger" plain @click="removeBranch(index)" />
              </div>
              <el-input
                v-model="branch.conditionExpression"
                :disabled="branch.defaultBranch"
                placeholder="如 taxRate < 5% && profitRate > 0.1"
              />
              <el-input
                v-model="branch.calculationExpression"
                type="textarea"
                :rows="3"
                placeholder="如 baseAmount * (1 + profitRate * 0.5 - taxRate * 0.3)"
              />
            </div>
            <el-button type="primary" :icon="Plus" class="add-branch" @click="addBranch">添加分支</el-button>
          </div>
        </el-form-item>

        <el-form-item label="结果变量名称" prop="resultVarName">
          <el-input v-model="editForm.resultVarName" placeholder="如 creditLimit" />
        </el-form-item>
        <el-form-item label="输出数据模板" prop="outputTemplate">
          <el-input v-model="editForm.outputTemplate" type="textarea" :rows="5" placeholder='{"companyId":"${companyId}","creditLimit":"${creditLimit}"}' />
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
.code-tag {
  margin: 2px 6px 2px 0;
}

.branch-list {
  display: grid;
  gap: 12px;
  width: 100%;
}

.branch-row {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e5edf7;
  border-radius: 8px;
  background: #fbfcfe;
}

.branch-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.branch-name {
  flex: 1;
}

.add-branch {
  width: 180px;
}

.quota-page :deep(.el-textarea__inner) {
  font-family: Consolas, "Microsoft YaHei", monospace;
}
</style>
