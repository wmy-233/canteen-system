<template>
  <div class="home-page">
    <el-container>
      <el-header class="header">
        <span class="logo">智慧校园食堂点餐系统</span>
        <span class="welcome">欢迎你，{{ userStore.userInfo?.username || '游客' }}</span>
        <el-button type="danger" link @click="handleLogout">退出</el-button>
      </el-header>
      <el-main>
        <!-- TODO: 第2周 B 开发菜品浏览（分类Tab + 菜品卡片 + 加入购物车） -->
        <el-empty description="菜品列表开发中（第2周 D1-D3）" />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  gap: 24px;
  border-bottom: 1px solid #eee;
}
.logo {
  font-weight: bold;
  font-size: 18px;
}
.welcome {
  flex: 1;
}
</style>
