<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  changeUserStatus,
  createUser,
  fetchRolePage,
  fetchUserPage,
  removeUser,
  resetUserPassword,
  updateUser
} from '../api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const tableData = ref([])
const total = ref(0)
const roleOptions = ref([])
const formRef = ref()

const queryForm = reactive({
  username: '',
  realName: '',
  roleId: ''
})

const pager = reactive({
  pageNum: 1,
  pageSize: 10
})

const editForm = reactive({
  id: null,
  username: '',
  realName: '',
  email: '',
  mobile: '',
  roleIds: [],
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  mobile: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增用户' : '编辑用户'))

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
  try {
    const res = await fetchRolePage({ pageNum: 1, pageSize: 100 }, { permissionScope: 'action' })
    roleOptions.value = res.data?.list || []
  } catch (error) {
    ElMessage.error(error.message || '角色列表加载失败')
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await fetchUserPage({ ...queryForm, ...pager })
    tableData.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '用户列表加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.pageNum = 1
  loadUsers()
}

function handleReset() {
  Object.assign(queryForm, { username: '', realName: '', roleId: '' })
  handleSearch()
}

function openCreate() {
  dialogMode.value = 'create'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: null,
    username: '',
    realName: '',
    email: '',
    mobile: '',
    roleIds: [],
    status: 1
  })
}

function openEdit(row) {
  const roleIds =
    row.roleIds?.length
      ? row.roleIds
      : roleOptions.value
          .filter((item) => (row.roleNames || []).includes(item.roleName))
          .map((item) => item.id)

  dialogMode.value = 'edit'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    email: row.email,
    mobile: row.mobile,
    roleIds,
    status: row.status
  })
}

async function submitForm() {
  try {
    await formRef.value.validate()
    if (dialogMode.value === 'create') {
      await createUser(editForm)
      ElMessage.success('新增用户成功')
    } else {
      await updateUser(editForm.id, editForm)
      ElMessage.success('修改用户成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error(error.message || (dialogMode.value === 'create' ? '新增用户失败' : '修改用户失败'))
  }
}

async function handleResetPassword(row) {
  try {
    await ElMessageBox.confirm(`确认重置用户 ${row.username} 的密码吗？`, '提示', { type: 'warning' })
    await resetUserPassword(row.id)
    ElMessage.success('密码已重置')
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '重置密码失败')
  }
}

async function handleToggleStatus(row) {
  try {
    const nextStatus = row.status === 1 ? 0 : 1
    await changeUserStatus(row.id, nextStatus)
    ElMessage.success(nextStatus === 1 ? '用户已启用' : '用户已禁用')
    loadUsers()
  } catch (error) {
    ElMessage.error(error.message || '状态修改失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, '提示', { type: 'warning' })
    await removeUser(row.id)
    ElMessage.success('用户已删除')
    loadUsers()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除用户失败')
  }
}

onMounted(async () => {
  await loadRoles()
  await loadUsers()
})
</script>

<template>
  <div class="manage-page">
    <div class="toolbar-row">
      <el-button v-permission="'system:user:create'" type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="用户名">
          <el-input v-model="queryForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="queryForm.realName" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="queryForm.roleId" placeholder="请选择角色" clearable style="width: 220px">
            <el-option v-for="item in roleOptions" :key="item.id" :label="item.roleName" :value="item.id" />
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
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column prop="mobile" label="手机号" min-width="140" />
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag type="danger" effect="plain">{{ (row.roleNames || []).join(' / ') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="220">
          <template #default="{ row }">
            <button v-permission="'system:user:update'" class="text-action primary" @click="openEdit(row)">编辑</button>
            <button v-permission="'system:user:reset-password'" class="text-action warning" @click="handleResetPassword(row)">重置密码</button>
            <button v-permission="'system:user:status'" class="text-action danger" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </button>
            <button v-permission="'system:user:delete'" class="text-action danger" @click="handleDelete(row)">删除</button>
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
          @change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editForm.realName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="editForm.mobile" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="editForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.id" :label="item.roleName" :value="item.id" />
          </el-select>
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
  </div>
</template>
