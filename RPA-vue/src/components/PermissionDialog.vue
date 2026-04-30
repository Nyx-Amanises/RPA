<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchResourceTree, fetchRoleResourceIds, saveRoleResources } from '../api/system'
import { useAuthStore } from '../stores/auth'

const props = defineProps({
  modelValue: Boolean,
  role: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const loading = ref(false)
const treeData = ref([])
const checkedKeys = ref([])
const treeRef = ref()
const authStore = useAuthStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

watch(
  () => props.modelValue,
  async (value) => {
    if (value && props.role) {
      loading.value = true
      try {
        const [treeRes, checkedRes] = await Promise.all([
          fetchResourceTree({}, { permissionScope: 'action' }),
          fetchRoleResourceIds(props.role.id)
        ])
        treeData.value = treeRes.data || []
        checkedKeys.value = checkedRes.data || []
      } catch (error) {
        ElMessage.error(error.message || '权限数据加载失败')
      } finally {
        loading.value = false
      }
    }
  }
)

async function handleSave() {
  try {
    const resourceIds = treeRef.value?.getCheckedKeys?.() || []
    await saveRoleResources(props.role.id, resourceIds)
    await authStore.loadCurrentUser(true)
    ElMessage.success('权限分配成功')
    emit('saved')
    visible.value = false
  } catch (error) {
    ElMessage.error(error.message || '权限分配失败')
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="分配权限" width="880px" destroy-on-close>
    <div v-loading="loading" class="permission-dialog">
      <el-tree
        ref="treeRef"
        node-key="id"
        show-checkbox
        check-strictly
        default-expand-all
        :data="treeData"
        :default-checked-keys="checkedKeys"
        :props="{ label: 'resourceName', children: 'children' }"
      />
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
