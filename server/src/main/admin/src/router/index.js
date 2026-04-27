import Vue from 'vue'
import Router from 'vue-router'
import { isAuthenticated } from '../utils/auth'

Vue.use(Router)

const LoginView = () => import('../views/LoginView.vue')
const OverviewView = () => import('../views/OverviewView.vue')
const UserView = () => import('../views/UserView.vue')
const RoleView = () => import('../views/RoleView.vue')
const RegionView = () => import('../views/RegionView.vue')
const OrgView = () => import('../views/OrgView.vue')
const DeviceView = () => import('../views/DeviceView.vue')
const ConfigView = () => import('../views/ConfigView.vue')
const PromptView = () => import('../views/PromptView.vue')
const SymptomTemplateView = () => import('../views/SymptomTemplateView.vue')
const DataPackageView = () => import('../views/DataPackageView.vue')
const ReleaseView = () => import('../views/ReleaseView.vue')
const LogView = () => import('../views/LogView.vue')
const FeedbackView = () => import('../views/FeedbackView.vue')

function resolveRedirectPath(value) {
  return typeof value === 'string' && value.indexOf('/') === 0 ? value : '/overview'
}

const router = new Router({
  mode: 'hash',
  routes: [
    { path: '/login', component: LoginView, meta: { title: '管理员登录', public: true } },
    { path: '/', redirect: '/overview' },
    { path: '/overview', component: OverviewView, meta: { title: '首页概览' } },
    { path: '/users', component: UserView, meta: { title: '用户管理' } },
    { path: '/roles', component: RoleView, meta: { title: '角色管理' } },
    { path: '/regions', component: RegionView, meta: { title: '区域管理' } },
    { path: '/orgs', component: OrgView, meta: { title: '机构管理' } },
    { path: '/devices', component: DeviceView, meta: { title: '设备管理' } },
    { path: '/configs', component: ConfigView, meta: { title: '模型配置' } },
    { path: '/prompts', component: PromptView, meta: { title: '提示词管理' } },
    { path: '/symptom-templates', component: SymptomTemplateView, meta: { title: '症状模板' } },
    { path: '/data-packages', component: DataPackageView, meta: { title: '数据包管理' } },
    { path: '/releases', component: ReleaseView, meta: { title: '版本发布' } },
    { path: '/logs', component: LogView, meta: { title: '操作日志' } },
    { path: '/feedbacks', component: FeedbackView, meta: { title: '反馈管理' } },
    { path: '*', redirect: '/overview' }
  ]
})

router.beforeEach((to, from, next) => {
  const loggedIn = isAuthenticated()
  const isPublicRoute = to.matched.some(record => record.meta && record.meta.public)

  if (isPublicRoute) {
    if (loggedIn && to.path === '/login') {
      next(resolveRedirectPath(to.query.redirect))
      return
    }
    next()
    return
  }

  if (!loggedIn) {
    next({
      path: '/login',
      query: {
        redirect: to.fullPath || '/overview'
      }
    })
    return
  }

  next()
})

router.afterEach(to => {
  if (typeof document !== 'undefined') {
    document.title = `${to.meta.title || '管理端'} - floating-ball-server`
  }
})

export default router
