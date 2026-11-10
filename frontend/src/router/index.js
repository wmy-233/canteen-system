import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// 各角色的默认首页，登录后按角色跳转
export const ROLE_HOME = {
  STUDENT: '/student',
  MERCHANT: '/merchant',
  ADMIN: '/admin'
}

const placeholder = (title) => ({ component: () => import('../views/Placeholder.vue'), props: { title } })

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },

  // ---- 学生端 ----
  {
    path: '/student',
    component: () => import('../layouts/FrameLayout.vue'),
    meta: { roles: ['STUDENT'] },
    children: [
      { path: '', component: () => import('../views/student/Dashboard.vue') },
      { path: 'menu', component: () => import('../views/student/Menu.vue') },
      { path: 'orders', component: () => import('../views/student/Orders.vue') },
      { path: 'wallet', component: () => import('../views/student/Wallet.vue') },
      { path: 'coupons', component: () => import('../views/student/Coupons.vue') },
      { path: 'dining', ...placeholder('食堂堂食') }
    ]
  },

  // ---- 商家端 ----
  {
    path: '/merchant',
    component: () => import('../layouts/FrameLayout.vue'),
    meta: { roles: ['MERCHANT'] },
    children: [
      { path: '', component: () => import('../views/merchant/Dashboard.vue') },
      { path: 'dishes', component: () => import('../views/merchant/Dishes.vue') },
      { path: 'orders', component: () => import('../views/merchant/OrderBoard.vue') },
      { path: 'stats', component: () => import('../views/merchant/Stats.vue') }
    ]
  },

  // ---- 管理端 ----
  {
    path: '/admin',
    component: () => import('../layouts/FrameLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      { path: '', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'users', component: () => import('../views/admin/Users.vue') },
      { path: 'merchants', component: () => import('../views/admin/StallAudit.vue') },
      { path: 'riders', component: () => import('../views/admin/Riders.vue') },
      { path: 'coupons', component: () => import('../views/admin/Coupons.vue') },
      { path: 'stats', component: () => import('../views/admin/Stats.vue') }
    ]
  },

  // 根路径：按角色跳对应布局，未登录去登录页
  {
    path: '/',
    redirect: () => {
      const userStore = useUserStore()
      return userStore.token ? (ROLE_HOME[userStore.role] || '/login') : '/login'
    }
  },
  // 兜底 404
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 判断当前登录状态是否有效：有 token 且角色是三种合法角色之一
// （防止 localStorage 里残留旧 token 导致空白页）
function hasValidLogin(userStore) {
  return userStore.token && !!ROLE_HOME[userStore.role]
}

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (!hasValidLogin(userStore)) {
    // token 或角色无效：清掉残留状态，避免循环重定向
    userStore.logout()
    return to.path === '/login' ? true : { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录（且角色有效）访问 /login → 跳回自己的首页（role 有效保证不会死循环）
  if (to.path === '/login') {
    return ROLE_HOME[userStore.role]
  }

  const roles = to.meta.roles
  if (roles && !roles.includes(userStore.role)) {
    return ROLE_HOME[userStore.role] || '/login'
  }
  return true
})

export default router
