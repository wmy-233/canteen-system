<template>
  <el-container class="frame-layout">
    <!-- 顶栏 -->
    <el-header class="header">
      <span class="logo">智慧校园食堂点餐系统</span>
      <span class="role-tag">
        <el-tag :type="roleType" size="small">{{ roleLabel }}</el-tag>
      </span>
      <span class="welcome">欢迎你，{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</span>
      <el-button type="danger" link @click="handleLogout">退出登录</el-button>
    </el-header>

    <el-container>
      <!-- 侧栏菜单：由各角色布局传入 -->
      <el-aside width="200px" class="aside">
        <el-menu
          :default-active="route.path"
          router
          class="menu"
        >
          <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

defineProps({
  // 侧栏菜单：[{ path, title, icon }]
  menus: {
    type: Array,
    default: () => []
  }
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const ROLE_META = {
  STUDENT: { label: '学生端', type: 'success' },
  MERCHANT: { label: '商家端', type: 'warning' },
  ADMIN: { label: '管理端', type: 'danger' }
}

const roleLabel = ROLE_META[userStore.role]?.label || userStore.role
const roleType = ROLE_META[userStore.role]?.type || 'info'

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.frame-layout {
  height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}
.logo {
  font-weight: bold;
  font-size: 18px;
}
.welcome {
  flex: 1;
  color: #666;
}
.aside {
  border-right: 1px solid #e4e7ed;
  background: #fff;
}
.menu {
  border-right: none;
}
.main {
  background: #f5f7fa;
}
</style>
