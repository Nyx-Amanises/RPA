import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import UserManageView from '../views/UserManageView.vue'
import RoleManageView from '../views/RoleManageView.vue'
import ResourceManageView from '../views/ResourceManageView.vue'
import TaskManageView from '../views/TaskManageView.vue'
import ExecutionManageView from '../views/ExecutionManageView.vue'
import RobotManageView from '../views/RobotManageView.vue'
import ProcessManageView from '../views/ProcessManageView.vue'
import DataCollectionView from '../views/DataCollectionView.vue'
import DataAnalysisView from '../views/DataAnalysisView.vue'
import DataProcessingView from '../views/DataProcessingView.vue'
import DataQueryView from '../views/DataQueryView.vue'
import IndicatorCalculateView from '../views/IndicatorCalculateView.vue'
import IndicatorQuotaView from '../views/IndicatorQuotaView.vue'
import AgentAssistView from '../views/AgentAssistView.vue'
import { clearPageForbidden } from '../stores/pageAccess'
import { getAuthToken, getAuthUser } from '../utils/auth'
import { getDefaultAuthorizedPath, hasPermission } from '../utils/permission'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true, title: '登录' }
    },
    {
      path: '/',
      component: MainLayout,
      redirect: '/dashboard',
      children: [
        {
          path: '/dashboard',
          name: 'dashboard',
          component: HomeView,
          meta: { title: '首页', section: 'dashboard', hidePageTitle: true, permission: 'dashboard:view' }
        },
        {
          path: '/system/profile',
          name: 'profile',
          component: ProfileView,
          meta: { title: '个人信息', section: 'system' }
        },
        {
          path: '/system/users',
          name: 'users',
          component: UserManageView,
          meta: { title: '用户管理', section: 'system', permission: 'system:user:page' }
        },
        {
          path: '/system/roles',
          name: 'roles',
          component: RoleManageView,
          meta: { title: '角色管理', section: 'system', permission: 'system:role:page' }
        },
        {
          path: '/system/resources',
          name: 'resources',
          component: ResourceManageView,
          meta: { title: '资源管理', section: 'system', permission: 'system:resource:tree' }
        },
        {
          path: '/rpa/tasks',
          name: 'tasks',
          component: TaskManageView,
          meta: { title: '任务列表', section: 'rpa', permission: 'task:page' }
        },
        {
          path: '/rpa/executions',
          name: 'executions',
          component: ExecutionManageView,
          meta: { title: '执行记录', section: 'rpa', permission: 'execution:page' }
        },
        {
          path: '/rpa/robots',
          name: 'robots',
          component: RobotManageView,
          meta: { title: '机器人管理', section: 'rpa', permission: 'robot:page' }
        },
        {
          path: '/rpa/processes',
          name: 'processes',
          component: ProcessManageView,
          meta: { title: '流程管理', section: 'rpa', permission: 'process:page' }
        },
        {
          path: '/rpa/data-collection',
          name: 'data-collection',
          component: DataCollectionView,
          meta: { title: '数据采集', section: 'rpa', permission: 'data:collection:page' }
        },
        {
          path: '/rpa/data-analysis',
          name: 'data-analysis',
          component: DataAnalysisView,
          meta: { title: '数据解析', section: 'rpa', permission: 'data:analysis:page' }
        },
        {
          path: '/rpa/data-processing',
          name: 'data-processing',
          component: DataProcessingView,
          meta: { title: '数据加工', section: 'rpa', permission: 'data:processing:page' }
        },
        {
          path: '/rpa/business-data',
          name: 'business-data',
          component: DataQueryView,
          meta: { title: '业务数据', section: 'rpa', permission: 'data:business:page' }
        },
        {
          path: '/indicators/calculate',
          name: 'indicator-calculate',
          component: IndicatorCalculateView,
          meta: { title: '指标计算', section: 'indicator' }
        },
        {
          path: '/indicators/quota',
          name: 'indicator-quota',
          component: IndicatorQuotaView,
          meta: { title: '指标额度计算', section: 'indicator' }
        },
        {
          path: '/indicators/agent-assist',
          name: 'agent-assist',
          component: AgentAssistView,
          meta: { title: 'AI 辅助', section: 'indicator' }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  clearPageForbidden()
  const token = getAuthToken()

  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.path === '/login' && token) {
    return { path: getDefaultAuthorizedPath(getAuthUser()) }
  }

  if (token && to.meta.permission && !hasPermission(to.meta.permission, getAuthUser())) {
    return { path: getDefaultAuthorizedPath(getAuthUser()), replace: true }
  }

  return true
})

export default router
