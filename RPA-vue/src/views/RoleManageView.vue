<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRole, fetchRolePage, removeRole, updateRole } from '../api/system'
import PermissionDialog from '../components/PermissionDialog.vue'

const loading = ref(false)
const dialogVisible = ref(false)
const permissionVisible = ref(false)
const currentRole = ref(null)
const dialogMode = ref('create')
const tableData = ref([])
const total = ref(0)
const formRef = ref()

const queryForm = reactive({
  roleName: '',
  roleCode: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const editForm = reactive({
  id: null,
  roleCode: '',
  roleName: '',
  description: '',
  status: 1
})

const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增角色' : '编辑角色'))

function formatDateTime(value) {
  if (!value) {
    return '--'
  }

  if (typeof value !== 'string') {
    return value
  }

  return value.replace('T', ' ').split('.')[0]
}

async function loadRoles() {
  loading.value = true
  try {
    const res = await fetchRolePage({ ...queryForm, ...pager })
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '角色列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadRoles()
}

function handleReset() {
  Object.assign(queryForm, { roleName: '', roleCode: '' })
  handleSearch()
}

function openCreate() {
  dialogMode.value = 'create'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: null,
    roleCode: '',
    roleName: '',
    description: '',
    status: 1
  })
}

function openEdit(row) {
  dialogMode.value = 'edit'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
    description: row.description,
    status: row.status
  })
}

async function submitForm() {
  try {
    await formRef.value.validate()
    if (dialogMode.value === 'create') {
      await createRole(editForm)
      ElMessage.success('新增角色成功')
    } else {
      await updateRole(editForm.id, editForm)
      ElMessage.success('修改角色成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch (error) {
    ElMessage.error(error.message || (dialogMode.value === 'create' ? '新增角色失败' : '修改角色失败'))
  }
}

function openPermission(row) {
  currentRole.value = row
  permissionVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除角色 ${row.roleName} 吗？`, '提示', { type: 'warning' })
    await removeRole(row.id)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除角色失败')
  }
}

onMounted(loadRoles)
</script>

<template>
  <div class="manage-page">
    <div class="toolbar-row">
      <el-button v-permission="'system:role:create'" type="primary" @click="openCreate">新增角色</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="角色名称">
          <el-input v-model="queryForm.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="queryForm.roleCode" placeholder="请输入角色编码" clearable />
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
        <el-table-column prop="roleCode" label="角色编码" min-width="150" />
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column label="权限" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.permissions || '无权限' }}
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" min-width="100" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="220">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <button v-permission="'system:role:update'" class="text-action primary" @click="openEdit(row)">编辑</button>
            <button v-permission="'system:role:assign-resource'" class="text-action primary" @click="openPermission(row)">分配权限</button>
            <button v-permission="'system:role:delete'" class="text-action danger" @click="handleDelete(row)">删除</button>
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
          @change="loadRoles"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="editForm.roleCode" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="editForm.roleName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
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

    <PermissionDialog v-model="permissionVisible" :role="currentRole" @saved="loadRoles" />
  </div>
</template>
