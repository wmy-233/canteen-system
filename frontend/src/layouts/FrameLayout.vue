<template>
  <div class="frame-layout" :class="'t-' + userStore.role">
    <!-- 顶栏 -->
    <header class="header">
      <div class="brand">
        <span class="brand-icon">{{ roleEmoji }}</span>
        <span class="brand-name">{{ roleFeature }} · 智慧校园食堂点餐系统</span>
      </div>
      <div class="header-right">
        <span class="role-tag">
          <el-tag :type="roleType" size="small" effect="light" round>{{ roleLabel }}</el-tag>
        </span>
        <div class="user-chip">
          <el-avatar :size="30" class="avatar">{{ avatarLetter }}</el-avatar>
          <span class="welcome">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</span>
        </div>
        <el-button type="danger" link size="small" @click="handleLogout">退出登录</el-button>
      </div>
    </header>

    <div class="body">
      <!-- 侧栏菜单 -->
      <aside class="aside">
        <el-menu :default-active="route.path" router class="menu">
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 主内容区 -->
      <main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 智能助手：胆子肥嘟嘟 -->
    <Dududu />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import Dududu from '../components/Dududu.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 每个角色一段完整的侧栏菜单，按当前路由前缀自动匹配，
// 所有子页面（含开发中的页面）都共享同一个布局，侧栏不再消失
const MENU_CONFIG = {
  '/student': [
    { path: '/student', title: '首页', icon: 'HomeFilled' },
    { path: '/student/menu', title: '菜品浏览', icon: 'Dish' },
    { path: '/student/orders', title: '我的订单', icon: 'List' },
    { path: '/student/wallet', title: '我的钱包', icon: 'Wallet' },
    { path: '/student/coupons', title: '优惠券', icon: 'Ticket' }
  ],
  '/merchant': [
    { path: '/merchant', title: '经营首页', icon: 'HomeFilled' },
    { path: '/merchant/dishes', title: '菜品管理', icon: 'Dish' },
    { path: '/merchant/orders', title: '出餐看板', icon: 'List' },
    { path: '/merchant/stats', title: '销量统计', icon: 'TrendCharts' }
  ],
  '/admin': [
    { path: '/admin', title: '总览首页', icon: 'HomeFilled' },
    { path: '/admin/users', title: '用户管理', icon: 'User' },
    { path: '/admin/merchants', title: '档口审核', icon: 'Shop' },
    { path: '/admin/riders', title: '骑手调度', icon: 'Van' },
    { path: '/admin/coupons', title: '优惠券管理', icon: 'Ticket' },
    { path: '/admin/stats', title: '全局统计', icon: 'TrendCharts' }
  ]
}

const menus = computed(() => {
  const prefix = Object.keys(MENU_CONFIG).find(p => route.path === p || route.path.startsWith(p + '/'))
  return prefix ? MENU_CONFIG[prefix] : []
})

const ROLE_META = {
  STUDENT: { label: '学生端', type: 'success' },
  MERCHANT: { label: '商家端', type: 'warning' },
  ADMIN: { label: '管理端', type: 'danger' }
}

const roleLabel = computed(() => ROLE_META[userStore.role]?.label || userStore.role)
const roleType = computed(() => ROLE_META[userStore.role]?.type || 'info')
const avatarLetter = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || '友').slice(0, 1))

// 三端差异化：专属图标、品牌文案、主题色
const ROLE_THEME = {
  STUDENT: { emoji: '🍱', feature: '学生端', accent: '#93977a', accentSoft: '#a4a88b' },
  MERCHANT: { emoji: '🏪', feature: '商家端', accent: '#a98467', accentSoft: '#bb9984' },
  ADMIN: { emoji: '📊', feature: '管理端', accent: '#7e94a8', accentSoft: '#97aabb' }
}
const roleEmoji = computed(() => ROLE_THEME[userStore.role]?.emoji || '🍜')
const roleFeature = computed(() => ROLE_THEME[userStore.role]?.feature || '智慧食堂')

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* ---- 鼠尾草绿莫兰迪色板：主色 #93977A / 米白底 #F5F3EA / 墨绿文字 #5E5F4E ----
   主题色随角色变化：学生=鼠尾草绿 商家=莫兰迪驼金 管理=雾蓝灰 */
.frame-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f3ec;
}
/* 三个端各自的沉浸式内容区底色（微弱差异，一眼可辨）+ 主题变量 */
.frame-layout.t-STUDENT { --ac: #93977a; --ac-soft: #83886a; --ac-glow: rgba(147, 151, 122, 0.45); background: linear-gradient(180deg, #f3f3e9 0%, #f6f5ee 240px, #f4f3ec 100%); }
.frame-layout.t-MERCHANT { --ac: #a98467; --ac-soft: #96735a; --ac-glow: rgba(169, 132, 103, 0.45); background: linear-gradient(180deg, #f5f0ea 0%, #f6f4f0 240px, #f4f2ee 100%); }
.frame-layout.t-ADMIN { --ac: #7e94a8; --ac-soft: #6d8093; --ac-glow: rgba(126, 148, 168, 0.45); background: linear-gradient(180deg, #eff2f2 0%, #f3f4f1 240px, #f2f2ec 100%); }

/* 顶栏 */
.header {
  height: 58px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  background: #ffffffee;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e4e2d3;
  box-shadow: 0 2px 12px rgba(147, 151, 122, 0.14);
}
.t-MERCHANT .header {
  background: linear-gradient(120deg, #fdfbf7 0%, #faf5ee 100%);
  border-bottom-color: #e8ded3;
}
.t-ADMIN .header {
  background: linear-gradient(120deg, #fcfdfd 0%, #f4f7f7 100%);
  border-bottom-color: #dae1e2;
}
.frame-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f3ea;
}

/* 顶栏 */
.header {
  height: 58px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 22px;
  background: #ffffffee;
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e4e2d3;
  box-shadow: 0 2px 12px rgba(147, 151, 122, 0.14);
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand-icon {
  font-size: 22px;
}
.brand-name {
  font-weight: 600;
  font-size: 16.5px;
  letter-spacing: 1px;
  color: #6f7060;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: 999px;
  background: #efede1;
}
.avatar {
  background: var(--ac);
  color: #fff;
  font-size: 14px;
}
.welcome {
  color: #8a8b7a;
  font-size: 13px;
}

/* 主体 */
.body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* 侧栏 */
.aside {
  width: 208px;
  flex: none;
  background: #edeadd;
  border-right: 1px solid #e2e0d0;
  padding-top: 14px;
}
.menu {
  border-right: none;
  background: transparent;
  padding: 0 10px;
}
.menu :deep(.el-menu-item) {
  margin-bottom: 4px;
  border-radius: 9px;
  height: 44px;
  color: #7d8168;
  transition: all 0.25s ease;
}
.menu :deep(.el-menu-item:hover) {
  background: #e1dfcd;
}
.menu :deep(.el-menu-item.is-active) {
  background: var(--ac);
  color: #fff;
  box-shadow: 0 4px 10px var(--ac-glow);
}

/* 内容区 */
.main {
  flex: 1;
  overflow: auto;
  padding: 22px;
  background: #f5f3ea;
}

/* 路由切换动画：轻微上滑淡入 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
