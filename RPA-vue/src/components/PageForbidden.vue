<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  title: {
    type: String,
    default: ''
  },
  message: {
    type: String,
    default: ''
  }
})

const router = useRouter()

const resultTitle = computed(() => props.title || '\u65e0\u6743\u9650\u8bbf\u95ee\u5f53\u524d\u9875\u9762')
const resultMessage = computed(() => props.message || '\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u5f00\u901a\u76f8\u5e94\u6743\u9650')
const dashboardText = '\u8fd4\u56de\u9996\u9875'
const reloadText = '\u91cd\u65b0\u52a0\u8f7d'

function goDashboard() {
  router.push('/dashboard')
}

function reloadPage() {
  window.location.reload()
}
</script>

<template>
  <div class="page-forbidden">
    <el-result icon="warning" :title="resultTitle" :sub-title="resultMessage">
      <template #extra>
        <div class="page-forbidden-actions">
          <el-button type="primary" @click="goDashboard">{{ dashboardText }}</el-button>
          <el-button @click="reloadPage">{{ reloadText }}</el-button>
        </div>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.page-forbidden {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  padding: 24px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: inset 0 0 0 1px #e5edf7;
}

.page-forbidden-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}
</style>
