<script setup>
import { computed, onMounted, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageForbidden from '../components/PageForbidden.vue'
import { useAuthStore } from '../stores/auth'
import { pageAccessState } from '../stores/pageAccess'
import { getDefaultAuthorizedPath } from '../utils/permission'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const topMenuDefinitions = [
  { label: '首页', key: 'dashboard', route: '/dashboard', permissions: ['dashboard:view'] },
  {
    label: 'RPA运营管理',
    key: 'rpa',
    permissions: [
      'task:page',
      'execution:page',
      'robot:page',
      'process:page',
      'data:collection:page',
      'data:analysis:page',
      'data:processing:page',
      'data:business:page'
    ]
  },
  { label: '系统管理', key: 'system', route: '/system/profile' },
  { label: '指标管理', key: 'indicator' }
]

const menuSections = {
  dashboard: {
    title: '首页',
    groups: []
  },
  system: {
    title: '系统管理',
    groups: [
      {
        title: '系统管理',
        items: [
          { label: '个人信息', path: '/system/profile' },
          { label: '用户管理', path: '/system/users', permission: 'system:user:page' },
          { label: '角色管理', path: '/system/roles', permission: 'system:role:page' },
          { label: '资源管理', path: '/system/resources', permission: 'system:resource:tree' }
        ]
      }
    ]
  },
  rpa: {
    title: 'RPA运营管理',
    groups: [
      {
        title: '任务管理',
        items: [
          { label: '任务列表', path: '/rpa/tasks', permission: 'task:page' },
          { label: '执行记录', path: '/rpa/executions', permission: 'execution:page' }
        ]
      },
      {
        title: '机器人管理',
        items: [{ label: '机器人列表', path: '/rpa/robots', permission: 'robot:page' }]
      },
      {
        title: '流程管理',
        items: [{ label: '流程列表', path: '/rpa/processes', permission: 'process:page' }]
      },
      {
        title: '数据管理',
        items: [
          { label: '数据采集', path: '/rpa/data-collection', permission: 'data:collection:page' },
          { label: '数据解析', path: '/rpa/data-analysis', permission: 'data:analysis:page' },
          { label: '数据加工', path: '/rpa/data-processing', permission: 'data:processing:page' },
          { label: '业务数据', path: '/rpa/business-data', permission: 'data:business:page' }
        ]
      }
    ]
  },
  indicator: {
    title: '指标管理',
    groups: [
      {
        title: '指标管理',
        items: [
          { label: '指标计算', path: '/indicators/calculate' },
          { label: '指标额度计算', path: '/indicators/quota' },
          { label: 'AI 辅助', path: '/indicators/agent-assist' }
        ]
      }
    ]
  }
}

const collapsedGroups = reactive({})
const currentSectionKey = computed(() => route.meta.section || 'system')
const currentSection = computed(() => menuSections[currentSectionKey.value] || menuSections.system)
const showPageTitle = computed(() => !route.meta.hidePageTitle)
const showForbiddenPage = computed(() => pageAccessState.deniedPath === route.path)
const forbiddenMessage = computed(() => pageAccessState.message)

const topMenus = computed(() =>
  topMenuDefinitions.filter((item) => !item.permissions || authStore.hasAnyPermission(item.permissions))
)

const visibleGroups = computed(() =>
  (currentSection.value.groups || [])
    .map((group) => ({
      ...group,
      items: (group.items || []).filter(canAccessMenuItem)
    }))
    .filter((group) => group.items.length > 0)
)

const showSidebar = computed(() => visibleGroups.value.length > 0)

const activeTopMenu = computed(() => {
  const sectionKey = currentSectionKey.value
  return topMenus.value.some((item) => item.key === sectionKey) ? sectionKey : topMenus.value[0]?.key
})

const pageTitle = computed(() => route.meta.title || currentSection.value.title)
const userName = computed(() => authStore.displayName)
const userInitial = computed(() => (userName.value || 'U').slice(0, 1).toUpperCase())
const userAvatar = computed(() => authStore.user?.avatarUrl || '')

function canAccessMenuItem(item) {
  return !item.permission || authStore.hasPermission(item.permission)
}

function getGroupKey(group) {
  return `${currentSectionKey.value}:${group.title}`
}

function isGroupCollapsed(group) {
  return Boolean(collapsedGroups[getGroupKey(group)])
}

function toggleGroup(group) {
  const key = getGroupKey(group)
  collapsedGroups[key] = !collapsedGroups[key]
}

function findFirstVisiblePath(sectionKey) {
  const groups = menuSections[sectionKey]?.groups || []
  for (const group of groups) {
    const item = (group.items || []).find(canAccessMenuItem)
    if (item?.path) {
      return item.path
    }
  }
  const topMenu = topMenuDefinitions.find((item) => item.key === sectionKey)
  return topMenu?.route || getDefaultAuthorizedPath(authStore.user)
}

function expandActiveGroup() {
  const activeGroup = visibleGroups.value.find((group) => group.items?.some((item) => item.path === route.path))

  if (activeGroup) {
    collapsedGroups[getGroupKey(activeGroup)] = false
  }
}

function handleTopSelect(key) {
  router.push(findFirstVisiblePath(key))
}

async function initUser() {
  if (authStore.isLoggedIn) {
    try {
      await authStore.loadCurrentUser(true)
    } catch {
      authStore.logout()
      ElMessage.error('登录状态已失效，请重新登录')
      router.replace('/login')
    }
  }
}

function handleCommand(command) {
  if (command === 'logout') {
    authStore.logout()
    router.replace('/login')
  }
}

watch([() => route.path, currentSectionKey, visibleGroups], expandActiveGroup, { immediate: true })

onMounted(initUser)
</script>

<template>
  <div class="app-shell">
    <header class="shell-header">
      <div class="brand">RPA 管理平台</div>
      <el-menu
        mode="horizontal"
        :ellipsis="false"
        :default-active="activeTopMenu"
        class="top-nav"
        background-color="#1f3f95"
        text-color="#dce8ff"
        active-text-color="#ffffff"
        @select="handleTopSelect"
      >
        <el-menu-item v-for="item in topMenus" :key="item.key" :index="item.key">
          {{ item.label }}
        </el-menu-item>
      </el-menu>
      <el-dropdown class="header-user" @command="handleCommand">
        <div class="header-user trigger-reset">
          <el-avatar :size="40" :src="userAvatar" class="header-avatar">{{ userInitial }}</el-avatar>
          <span>{{ userName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <div class="shell-body" :class="{ 'shell-body-full': !showSidebar }">
      <aside v-if="showSidebar" class="shell-sidebar">
        <template v-for="group in visibleGroups" :key="group.title">
          <div class="sidebar-title" @click="toggleGroup(group)">
            <span>{{ group.title }}</span>
            <el-icon>
              <ArrowDown v-if="isGroupCollapsed(group)" />
              <ArrowUp v-else />
            </el-icon>
          </div>
          <div v-show="!isGroupCollapsed(group)" class="sidebar-menu">
            <router-link
              v-for="item in group.items"
              :key="item.label"
              :to="item.path"
              class="sidebar-link"
              :class="{ active: route.path === item.path }"
            >
              {{ item.label }}
            </router-link>
          </div>
        </template>
      </aside>

      <main class="shell-content" :class="{ 'shell-content-dashboard': !showSidebar }">
        <h1 v-if="showPageTitle" class="page-title">{{ pageTitle }}</h1>
        <PageForbidden v-if="showForbiddenPage" :message="forbiddenMessage" />
        <router-view v-else />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f3f6fb;
}

.shell-header {
  display: grid;
  grid-template-columns: 210px 1fr auto;
  align-items: center;
  gap: 24px;
  height: 72px;
  padding: 0 24px;
  background: linear-gradient(90deg, #1f3f95 0%, #2454c6 100%);
  color: #fff;
  box-shadow: 0 10px 24px rgba(31, 63, 149, 0.18);
}

.brand {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0;
}

.top-nav {
  border-bottom: none;
}

.header-user {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  cursor: pointer;
}

.trigger-reset:focus,
.trigger-reset:active {
  outline: none;
}

.header-avatar {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}

.shell-body {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  min-height: calc(100vh - 72px);
}

.shell-body-full {
  grid-template-columns: minmax(0, 1fr);
}

.shell-sidebar {
  padding: 18px 14px;
  background: #ffffff;
  border-right: 1px solid #e5edf7;
}

.sidebar-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  margin-bottom: 10px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 700;
  border-radius: 8px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease, color 0.2s ease;
}

.sidebar-title:hover {
  background: #f8fbff;
  color: #1d4ed8;
}

.sidebar-menu {
  display: grid;
  gap: 8px;
  margin-bottom: 24px;
}

.sidebar-link {
  display: block;
  padding: 12px 12px;
  border-radius: 8px;
  color: #475569;
  text-decoration: none;
  transition: all 0.2s ease;
}

.sidebar-link:hover {
  background: #eef4ff;
  color: #1d4ed8;
}

.sidebar-link.active {
  background: linear-gradient(135deg, #e8f0ff 0%, #d8e7ff 100%);
  color: #1d4ed8;
  font-weight: 700;
}

.shell-content {
  padding: 24px;
  min-width: 0;
  overflow-x: auto;
}

.shell-body-full .shell-content {
  grid-column: 1 / -1;
  width: 100%;
  min-width: 0;
}

.shell-content-dashboard {
  padding: 18px 18px 28px;
}

.page-title {
  margin: 0 0 20px;
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .shell-header {
    grid-template-columns: 1fr;
    height: auto;
    padding: 18px;
  }

  .shell-body {
    grid-template-columns: 1fr;
  }

  .shell-sidebar {
    border-right: none;
    border-bottom: 1px solid #e5edf7;
  }
}
</style>
