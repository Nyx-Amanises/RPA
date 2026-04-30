<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createResource, fetchResourceTree, removeResource, updateResource } from '../api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref()
const treeData = ref([])

const queryForm = reactive({
  resourceName: '',
  resourceType: ''
})

const editForm = reactive({
  id: null,
  parentId: null,
  resourceName: '',
  resourceCode: '',
  resourceType: 2,
  path: '',
  component: '',
  permissionKey: '',
  icon: '',
  sortNo: 1,
  status: 1
})

const resourceTypeOptions = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '按钮', value: 3 }
]

const rules = {
  resourceName: [{ required: true, message: '请输入资源名称', trigger: 'blur' }],
  resourceCode: [{ required: true, message: '请输入资源编码', trigger: 'blur' }],
  resourceType: [{ required: true, message: '请选择资源类型', trigger: 'change' }]
}

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增资源' : '编辑资源'))
const parentOptions = computed(() => flattenResources(treeData.value))

async function loadResources() {
  loading.value = true
  try {
    const res = await fetchResourceTree({
      resourceName: queryForm.resourceName || '',
      resourceType: queryForm.resourceType === '' ? '' : queryForm.resourceType
    })
    treeData.value = normalizeResourceTree(res.data || [])
  } catch (error) {
    ElMessage.error(error.message || '资源树加载失败')
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  await loadResources()
}

async function handleReset() {
  Object.assign(queryForm, { resourceName: '', resourceType: '' })
  await loadResources()
}

function openCreate(row = null) {
  dialogMode.value = 'create'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: null,
    parentId: row?.id ?? null,
    resourceName: '',
    resourceCode: '',
    resourceType: row ? 2 : 1,
    path: '',
    component: '',
    permissionKey: '',
    icon: '',
    sortNo: 1,
    status: 1
  })
}

function openEdit(row) {
  dialogMode.value = 'edit'
  dialogVisible.value = true
  Object.assign(editForm, {
    id: row.id,
    parentId: row.parentId ?? null,
    resourceName: row.resourceName ?? '',
    resourceCode: row.resourceCode ?? '',
    resourceType: row.resourceType ?? 2,
    path: row.path ?? '',
    component: row.component ?? '',
    permissionKey: row.permissionKey ?? row.permissionCode ?? '',
    icon: row.icon ?? '',
    sortNo: row.sortNo ?? 1,
    status: row.status ?? 1
  })
}

async function submitForm() {
  const payload = {
    parentId: editForm.parentId || null,
    resourceName: editForm.resourceName,
    resourceCode: editForm.resourceCode,
    resourceType: editForm.resourceType,
    path: editForm.path,
    component: editForm.component,
    permissionKey: editForm.permissionKey,
    icon: editForm.icon,
    sortNo: editForm.sortNo,
    status: editForm.status
  }

  try {
    await formRef.value.validate()
    if (dialogMode.value === 'create') {
      await createResource(payload)
      ElMessage.success('资源新增成功')
    } else {
      await updateResource(editForm.id, payload)
      ElMessage.success('资源修改成功')
    }
    dialogVisible.value = false
    await loadResources()
  } catch (error) {
    ElMessage.error(error.message || (dialogMode.value === 'create' ? '新增资源失败' : '修改资源失败'))
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除资源 ${row.resourceName} 吗？`, '提示', { type: 'warning' })
    await removeResource(row.id)
    ElMessage.success('资源已删除')
    await loadResources()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除资源失败')
  }
}

function getTypeLabel(type) {
  return resourceTypeOptions.find((item) => item.value === type)?.label || '--'
}

function normalizeResourceTree(nodes) {
  const source = Array.isArray(nodes) ? nodes : []
  const nodeMap = new Map()

  const collect = (items) => {
    items.forEach((item) => {
      if (!item || typeof item !== 'object') {
        return
      }

      const current = {
        ...item,
        children: []
      }
      const previous = nodeMap.get(item.id)
      nodeMap.set(item.id, previous ? { ...previous, ...current } : current)

      if (Array.isArray(item.children) && item.children.length) {
        collect(item.children)
      }
    })
  }

  collect(source)

  const roots = []
  nodeMap.forEach((node) => {
    const parentId = node.parentId
    if (parentId !== null && typeof parentId !== 'undefined' && nodeMap.has(parentId)) {
      nodeMap.get(parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })

  const sortTree = (items) => {
    items.sort((a, b) => {
      const sortDiff = Number(a.sortNo || 0) - Number(b.sortNo || 0)
      if (sortDiff !== 0) {
        return sortDiff
      }
      return String(a.resourceCode || '').localeCompare(String(b.resourceCode || ''))
    })

    items.forEach((item) => {
      if (item.children?.length) {
        sortTree(item.children)
      }
    })

    return items
  }

  return sortTree(roots)
}

function flattenResources(nodes, level = 0) {
  const list = []
  nodes.forEach((item) => {
    list.push({
      ...item,
      level
    })
    if (item.children?.length) {
      list.push(...flattenResources(item.children, level + 1))
    }
  })
  return list
}

onMounted(loadResources)
</script>

<template>
  <div class="manage-page">
    <div class="toolbar-row">
      <el-button v-permission="'system:resource:create'" type="primary" @click="openCreate()">新增资源</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="queryForm" inline class="inline-form">
        <el-form-item label="资源名称">
          <el-input v-model="queryForm.resourceName" placeholder="请输入" clearable />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="queryForm.resourceType" placeholder="请选择" clearable style="width: 220px">
            <el-option v-for="item in resourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="resourceName" label="资源名称" min-width="220" />
        <el-table-column prop="resourceCode" label="资源编码" min-width="180" />
        <el-table-column label="资源类型" min-width="100">
          <template #default="{ row }">
            <el-tag type="primary" effect="light">{{ getTypeLabel(row.resourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径/URL" min-width="180" />
        <el-table-column prop="icon" label="图标" min-width="120" />
        <el-table-column prop="sortNo" label="排序" min-width="80" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="190" fixed="right">
          <template #default="{ row }">
            <button v-permission="'system:resource:create'" class="text-action primary" @click="openCreate(row)">新增子级</button>
            <button v-permission="'system:resource:update'" class="text-action primary" @click="openEdit(row)">编辑</button>
            <button v-permission="'system:resource:delete'" class="text-action danger" @click="handleDelete(row)">删除</button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="editForm" :rules="rules" label-width="98px">
        <el-form-item label="上级资源">
          <el-select v-model="editForm.parentId" clearable placeholder="顶级资源" style="width: 100%">
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="`${'—'.repeat(item.level)} ${item.resourceName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="资源名称" prop="resourceName">
          <el-input v-model="editForm.resourceName" />
        </el-form-item>
        <el-form-item label="资源编码" prop="resourceCode">
          <el-input v-model="editForm.resourceCode" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="资源类型" prop="resourceType">
          <el-select v-model="editForm.resourceType" style="width: 100%">
            <el-option v-for="item in resourceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="editForm.path" />
        </el-form-item>
        <el-form-item label="组件">
          <el-input v-model="editForm.component" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="editForm.permissionKey" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="editForm.icon" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="editForm.sortNo" :min="0" :max="999" />
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
