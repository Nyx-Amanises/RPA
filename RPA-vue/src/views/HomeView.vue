<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Connection,
  DataLine,
  Files,
  Monitor,
  Plus,
  Tickets
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { fetchDashboardRecentTasks, fetchDashboardSummary } from '../api/system'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const recentTasks = ref([])

const dashboard = reactive({
  taskSummary: {
    total: 0,
    todayNew: 0,
    pending: 0,
    queued: 0,
    running: 0,
    success: 0,
    failed: 0
  },
  robotSummary: {
    total: 0,
    online: 0,
    working: 0,
    offline: 0
  },
  processSummary: {
    total: 0,
    enabled: 0,
    disabled: 0
  },
  dataSummary: {
    total: 0,
    available: 0,
    todayCollected: 0
  },
  taskStatusOverview: {
    pending: 0,
    queued: 0,
    running: 0,
    success: 0,
    failed: 0
  },
  executionMetrics: {
    totalExecutions: 0,
    successExecutions: 0,
    failedExecutions: 0,
    runningExecutions: 0,
    queuedExecutions: 0,
    successRate: 0,
    averageDurationSeconds: 0
  },
  failureTopReasons: [],
  systemInfo: {
    systemName: '',
    systemVersion: '',
    version: '',
    runningDays: 0,
    runtimeDays: 0,
    dataSource: '',
    database: '',
    lastUpdateTime: ''
  }
})

const welcomeName = computed(() => authStore.displayName || '系统管理员')

const todayText = computed(() => {
  const now = new Date()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}年${month}月${day}日 ${weekDays[now.getDay()]}`
})

const statCards = computed(() => [
  {
    title: '总任务数',
    value: dashboard.taskSummary.total,
    hint: `今日新增 ${dashboard.taskSummary.todayNew}`,
    icon: Tickets,
    theme: 'violet'
  },
  {
    title: '机器人总数',
    value: dashboard.robotSummary.total,
    hint: `在线 ${dashboard.robotSummary.online}`,
    icon: Monitor,
    theme: 'pink'
  },
  {
    title: '流程总数',
    value: dashboard.processSummary.total,
    hint: `启用 ${dashboard.processSummary.enabled}`,
    icon: Connection,
    theme: 'cyan'
  },
  {
    title: '数据总量',
    value: dashboard.dataSummary.total,
    hint: `今日采集 ${dashboard.dataSummary.todayCollected}`,
    icon: Files,
    theme: 'green'
  }
])

const taskStatusItems = computed(() => [
  { label: '运行中', value: dashboard.taskStatusOverview.running, tone: 'warning' },
  { label: '排队中', value: dashboard.taskStatusOverview.queued, tone: 'warning' },
  { label: '待执行', value: dashboard.taskStatusOverview.pending, tone: 'pending' },
  { label: '已完成', value: dashboard.taskStatusOverview.success, tone: 'success' },
  { label: '失败', value: dashboard.taskStatusOverview.failed, tone: 'danger' }
])

const executionMetricItems = computed(() => [
  { label: '任务成功率', value: formatRate(dashboard.executionMetrics.successRate), tone: 'success' },
  { label: '平均耗时', value: formatSeconds(dashboard.executionMetrics.averageDurationSeconds), tone: 'primary' },
  { label: '执行总数', value: dashboard.executionMetrics.totalExecutions, tone: 'pending' },
  { label: '失败次数', value: dashboard.executionMetrics.failedExecutions, tone: 'danger' }
])

const quickEntries = [
  {
    title: '创建任务',
    desc: '快速创建新的RPA任务',
    icon: Plus,
    theme: 'violet',
    path: '/rpa/tasks'
  },
  {
    title: '流程定义',
    desc: '定义和管理RPA流程',
    icon: Connection,
    theme: 'green',
    path: '/rpa/processes'
  },
  {
    title: '机器人列表',
    desc: '查看和管理机器人',
    icon: Monitor,
    theme: 'pink',
    path: '/rpa/robots'
  },
  {
    title: '数据查询',
    desc: '查询已处理的业务数据',
    icon: DataLine,
    theme: 'cyan',
    path: '/rpa/business-data'
  }
]

const systemInfoRows = computed(() => [
  { label: '系统名称', value: dashboard.systemInfo.systemName || 'RPA管理系统' },
  { label: '系统版本', value: dashboard.systemInfo.systemVersion || dashboard.systemInfo.version || '--' },
  { label: '运行时间', value: formatRunningDays(dashboard.systemInfo.runningDays ?? dashboard.systemInfo.runtimeDays) },
  { label: '数据源', value: dashboard.systemInfo.dataSource || dashboard.systemInfo.database || '--' },
  { label: '最后更新', value: formatDateTime(dashboard.systemInfo.lastUpdateTime) }
])

function normalizeSummary(data = {}) {
  const taskStatusOverview = data.taskStatusOverview || data.taskStatusSummary || {}

  return {
    taskSummary: {
      total: data.taskSummary?.total ?? data.taskTotal ?? 0,
      todayNew: data.taskSummary?.todayNew ?? 0,
      pending: data.taskSummary?.pending ?? taskStatusOverview.pending ?? 0,
      queued: data.taskSummary?.queued ?? taskStatusOverview.queued ?? 0,
      running: data.taskSummary?.running ?? taskStatusOverview.running ?? 0,
      success: data.taskSummary?.success ?? taskStatusOverview.success ?? 0,
      failed: data.taskSummary?.failed ?? taskStatusOverview.failed ?? 0
    },
    robotSummary: {
      total: data.robotSummary?.total ?? data.robotTotal ?? 0,
      online: data.robotSummary?.online ?? 0,
      working: data.robotSummary?.working ?? 0,
      offline: data.robotSummary?.offline ?? 0
    },
    processSummary: {
      total: data.processSummary?.total ?? data.processTotal ?? 0,
      enabled: data.processSummary?.enabled ?? 0,
      disabled: data.processSummary?.disabled ?? 0
    },
    dataSummary: {
      total: data.dataSummary?.total ?? data.dataTotal ?? 0,
      available: data.dataSummary?.available ?? 0,
      todayCollected: data.dataSummary?.todayCollected ?? 0
    },
    taskStatusOverview: {
      pending: taskStatusOverview.pending ?? 0,
      queued: taskStatusOverview.queued ?? 0,
      running: taskStatusOverview.running ?? 0,
      success: taskStatusOverview.success ?? 0,
      failed: taskStatusOverview.failed ?? 0
    },
    executionMetrics: {
      totalExecutions: data.executionMetrics?.totalExecutions ?? 0,
      successExecutions: data.executionMetrics?.successExecutions ?? 0,
      failedExecutions: data.executionMetrics?.failedExecutions ?? 0,
      runningExecutions: data.executionMetrics?.runningExecutions ?? 0,
      queuedExecutions: data.executionMetrics?.queuedExecutions ?? 0,
      successRate: data.executionMetrics?.successRate ?? 0,
      averageDurationSeconds: data.executionMetrics?.averageDurationSeconds ?? 0
    },
    failureTopReasons: Array.isArray(data.failureTopReasons) ? data.failureTopReasons : [],
    systemInfo: {
      systemName: data.systemInfo?.systemName || 'RPA管理系统',
      systemVersion: data.systemInfo?.systemVersion || data.systemInfo?.version || '',
      version: data.systemInfo?.version || data.systemInfo?.systemVersion || '',
      runningDays: data.systemInfo?.runningDays ?? data.systemInfo?.runtimeDays ?? 0,
      runtimeDays: data.systemInfo?.runtimeDays ?? data.systemInfo?.runningDays ?? 0,
      dataSource: data.systemInfo?.dataSource || data.systemInfo?.database || '',
      database: data.systemInfo?.database || data.systemInfo?.dataSource || '',
      lastUpdateTime: data.systemInfo?.lastUpdateTime || ''
    }
  }
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').split('.')[0]
}

function formatRunningDays(value) {
  if (value === null || typeof value === 'undefined' || value === '') {
    return '--'
  }
  return `${value}天`
}

function formatRate(value) {
  const rate = Number(value)
  return Number.isFinite(rate) ? `${rate.toFixed(1)}%` : '0.0%'
}

function formatSeconds(value) {
  const seconds = Number(value)
  if (!Number.isFinite(seconds)) {
    return '--'
  }
  return seconds >= 60 ? `${(seconds / 60).toFixed(1)}分钟` : `${seconds.toFixed(1)}秒`
}

function getTaskStatusMeta(status) {
  if (status === 4) {
    return { text: '排队中', type: 'warning' }
  }
  if (status === 1) {
    return { text: '运行中', type: 'warning' }
  }
  if (status === 2) {
    return { text: '已完成', type: 'success' }
  }
  if (status === 3) {
    return { text: '失败', type: 'danger' }
  }
  return { text: '待执行', type: 'info' }
}

function goTo(path) {
  router.push(path)
}

async function loadDashboard() {
  loading.value = true
  try {
    const [summaryRes, recentRes] = await Promise.all([
      fetchDashboardSummary(),
      fetchDashboardRecentTasks({ limit: 5 })
    ])
    Object.assign(dashboard, normalizeSummary(summaryRes.data || {}))
    recentTasks.value = Array.isArray(recentRes.data)
      ? recentRes.data.map((item) => ({
          ...item,
          statusLabel: item.statusLabel || getTaskStatusMeta(item.status).text
        }))
      : []
  } catch (error) {
    ElMessage.error(error.message || '首页数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <div v-loading="loading" class="home-page">
    <section class="home-hero">
      <div class="hero-copy">
        <h2>欢迎回来，{{ welcomeName }}！</h2>
        <p>今天是 {{ todayText }}，系统运行正常</p>
      </div>
      <el-button type="primary" size="large" class="hero-button" @click="goTo('/rpa/tasks')">
        + 创建任务
      </el-button>
    </section>

    <section class="home-stats">
      <article v-for="item in statCards" :key="item.title" class="stat-card">
        <div class="stat-main">
          <div class="stat-icon" :class="`theme-${item.theme}`">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-title">{{ item.title }}</div>
          </div>
        </div>
        <div class="stat-footer">{{ item.hint }}</div>
      </article>
    </section>

    <section class="content-grid">
      <div class="content-main">
        <el-card shadow="never" class="panel-card home-card">
          <template #header>
            <div class="panel-head">
              <span>任务状态概览</span>
              <button class="panel-link" @click="goTo('/rpa/tasks')">查看详情</button>
            </div>
          </template>
          <div class="status-grid">
            <div v-for="item in taskStatusItems" :key="item.label" class="status-item">
              <span class="status-dot" :class="`tone-${item.tone}`"></span>
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card home-card">
          <template #header>
            <div class="panel-head">
              <span>执行监控</span>
              <button class="panel-link" @click="goTo('/rpa/executions')">查看日志</button>
            </div>
          </template>
          <div class="monitor-grid">
            <div v-for="item in executionMetricItems" :key="item.label" class="monitor-item" :class="`tone-${item.tone}`">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>
          <div class="failure-panel">
            <div class="failure-title">失败 Top 原因</div>
            <div v-if="dashboard.failureTopReasons.length" class="failure-list">
              <div v-for="item in dashboard.failureTopReasons" :key="item.reason" class="failure-row">
                <span>{{ item.reason }}</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
            <div v-else class="failure-empty">暂无失败记录</div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card home-card">
          <template #header>
            <div class="panel-head">
              <span>最近任务</span>
              <button class="panel-link" @click="goTo('/rpa/tasks')">查看全部</button>
            </div>
          </template>
          <el-table :data="recentTasks" stripe empty-text="暂无最近任务">
            <el-table-column prop="taskCode" label="任务编码" min-width="220" />
            <el-table-column prop="processName" label="流程名称" min-width="170" />
            <el-table-column prop="enterpriseName" label="企业名称" min-width="180" />
            <el-table-column label="状态" min-width="110">
              <template #default="{ row }">
                <el-tag :type="getTaskStatusMeta(row.status).type" effect="light">
                  {{ row.statusLabel || getTaskStatusMeta(row.status).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <aside class="content-side">
        <el-card shadow="never" class="panel-card home-card">
          <template #header>
            <div class="panel-head">
              <span>快捷入口</span>
            </div>
          </template>
          <div class="quick-list">
            <button v-for="item in quickEntries" :key="item.title" type="button" class="quick-item" @click="goTo(item.path)">
              <span class="quick-icon" :class="`theme-${item.theme}`">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="quick-copy">
                <strong>{{ item.title }}</strong>
                <small>{{ item.desc }}</small>
              </span>
              <span class="quick-arrow">></span>
            </button>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card home-card">
          <template #header>
            <div class="panel-head">
              <span>系统信息</span>
            </div>
          </template>
          <div class="system-grid">
            <div v-for="item in systemInfoRows" :key="item.label" class="system-row">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </el-card>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.home-hero {
  width: 100%;
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 38px 34px;
  border-radius: 16px;
  background: linear-gradient(90deg, #5a77e5 0%, #7b4eb5 100%);
  color: #fff;
}

.hero-copy h2 {
  margin: 0 0 18px;
  font-size: 30px;
  line-height: 1.2;
}

.hero-copy p {
  margin: 0;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
}

.hero-button {
  min-width: 116px;
  height: 40px;
  flex-shrink: 0;
}

.home-stats {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}

.stat-card {
  min-width: 0;
  padding: 22px;
  border-radius: 14px;
  background: #fff;
  box-shadow: var(--shadow-soft);
}

.stat-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.stat-icon,
.quick-icon {
  width: 68px;
  height: 68px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  color: #fff;
  font-size: 28px;
  flex-shrink: 0;
}

.theme-violet {
  background: linear-gradient(135deg, #6a66df 0%, #7255d9 100%);
}

.theme-pink {
  background: linear-gradient(135deg, #f48fe8 0%, #f45d7f 100%);
}

.theme-cyan {
  background: linear-gradient(135deg, #54aaf7 0%, #28d2ea 100%);
}

.theme-green {
  background: linear-gradient(135deg, #47df82 0%, #39deb8 100%);
}

.stat-content {
  min-width: 0;
}

.stat-value {
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
  color: #24324b;
}

.stat-title {
  margin-top: 10px;
  font-size: 15px;
  color: #7d8ba6;
}

.stat-footer {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #edf1f7;
  color: #7d8ba6;
  font-size: 14px;
}

.content-grid {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
  gap: 24px;
  align-items: start;
}

.content-main,
.content-side {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.home-card {
  width: 100%;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #24324b;
}

.panel-link {
  border: none;
  background: transparent;
  color: #409eff;
  font-weight: 600;
  cursor: pointer;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.status-item {
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 24px 12px;
  border-radius: 14px;
  background: #fafbfd;
}

.status-item strong {
  font-size: 24px;
  color: #24324b;
}

.status-item span:last-child {
  color: #7d8ba6;
}

.status-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
}

.tone-warning {
  background: #f5a623;
}

.tone-pending {
  background: #a0a4ad;
}

.tone-success {
  background: #67c23a;
}

.tone-danger {
  background: #f56c6c;
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.monitor-item {
  min-width: 0;
  padding: 18px 14px;
  border-radius: 10px;
  background: #f8fafc;
  box-shadow: inset 0 0 0 1px #edf1f7;
}

.monitor-item strong {
  display: block;
  color: #24324b;
  font-size: 24px;
  line-height: 1;
}

.monitor-item span {
  display: block;
  margin-top: 10px;
  color: #7d8ba6;
}

.monitor-item.tone-success {
  background: #f0fdf4;
}

.monitor-item.tone-primary {
  background: #eff6ff;
}

.monitor-item.tone-danger {
  background: #fff1f2;
}

.failure-panel {
  margin-top: 18px;
}

.failure-title {
  margin-bottom: 12px;
  color: #24324b;
  font-weight: 700;
}

.failure-list {
  display: grid;
  gap: 10px;
}

.failure-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #fee2e2;
  border-radius: 8px;
  background: #fff7f7;
}

.failure-row span {
  min-width: 0;
  overflow: hidden;
  color: #5b6475;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.failure-row strong {
  color: #ef4444;
  text-align: right;
}

.failure-empty {
  display: flex;
  min-height: 78px;
  align-items: center;
  justify-content: center;
  border: 1px dashed #d7e2f0;
  border-radius: 8px;
  color: #94a3b8;
  background: #f8fbff;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.quick-item {
  width: 100%;
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 20px;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition: 0.2s ease;
}

.quick-item:hover {
  box-shadow: 0 12px 24px rgba(33, 65, 154, 0.08);
  transform: translateY(-1px);
}

.quick-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
}

.quick-copy strong {
  color: #24324b;
  font-size: 16px;
}

.quick-copy small {
  color: #7d8ba6;
  font-size: 13px;
}

.quick-arrow {
  color: #b0b9c8;
  font-size: 22px;
}

.system-grid {
  border: 1px solid #e6ebf2;
  border-radius: 12px;
  overflow: hidden;
}

.system-row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
}

.system-row span,
.system-row strong {
  padding: 14px 16px;
  border-bottom: 1px solid #e6ebf2;
}

.system-row:last-child span,
.system-row:last-child strong {
  border-bottom: none;
}

.system-row span {
  background: #f5f8fc;
  font-weight: 600;
  color: #5e6d87;
}

.system-row strong {
  color: #24324b;
  font-weight: 500;
  word-break: break-word;
}

@media (max-width: 1280px) {
  .home-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .home-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 28px 22px;
  }

  .home-stats,
  .status-grid,
  .monitor-grid {
    grid-template-columns: 1fr;
  }

  .system-row {
    grid-template-columns: 1fr;
  }
}
</style>
