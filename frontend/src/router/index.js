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
      { path: 'menu', ...placeholder('菜品浏览') },
      { path: 'orders', ...placeholder('我的订单') },
      { path: 'wallet', ...placeholder('我的钱包') },
      { path: 'coupons', ...placeholder('优惠券') }
    ]
  },

  // ---- 商家端 ----
  {
    path: '/merchant',
    component: () => import('../layouts/FrameLayout.vue'),
    meta: { roles: ['MERCHANT'] },
    children: [
      { path: '', component: () => import('../views/merchant/Dashboard.vue') },
      { path: 'dishes', ...placeholder('菜品管理') },
      { path: 'orders', ...placeholder('出餐看板') },
      { path: 'stats', ...placeholder('销量统计') }
    ]
  },

  // ---- 管理端 ----
  {
    path: '/admin',
    component: () => import('../layouts/FrameLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      { path: '', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'users', ...placeholder('用户管理') },
      { path: 'merchants', ...placeholder('档口审核') },
      { path: 'stats', ...placeholder('全局统计') }
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

// 路由守卫：
// 1. 未登录访问受保护页面 → /login
// 2. 已登录访问 /login → 按角色跳回自己的布局
// 3. 角色越权访问（如学生访问 /admin）→ 踢回自己的首页
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
