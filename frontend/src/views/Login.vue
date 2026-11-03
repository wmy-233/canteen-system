<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">智慧校园食堂点餐系统</h2>
      <el-form :model="form" label-width="60px" @keyup.enter="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <!-- TODO: 第1周 D5 联调后端登录接口 /api/user/login -->
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

// TODO: 对接真实登录接口后删除此临时模拟逻辑
function handleLogin() {
  loading.value = true
  setTimeout(() => {
    userStore.setLogin({ token: 'mock-token', userInfo: { username: form.username, role: 'STUDENT' } })
    router.push('/')
    loading.value = false
  }, 300)
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 380px;
}
.title {
  text-align: center;
  margin-bottom: 24px;
}
</style>
