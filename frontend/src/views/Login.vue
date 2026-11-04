<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="title">智慧校园食堂点餐系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="60px" @keyup.enter="handleLogin">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
        <el-form-item>
          <el-button link type="primary" style="width: 100%" @click="showRegister = true">没有账号？去注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegister" title="注册账号" width="400px">
      <el-form ref="regFormRef" :model="regForm" :rules="rules" label-width="60px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="regForm.username" placeholder="4-20 位字母数字" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="regForm.nickname" placeholder="请输入昵称" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="regForm.password" type="password" placeholder="6-20 位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="handleRegister">注册并登录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/user'
import { useUserStore } from '../stores/user'
import { ROLE_HOME } from '../router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const regFormRef = ref()
const loading = ref(false)
const regLoading = ref(false)
const showRegister = ref(false)

const form = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', nickname: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 登录成功后按角色跳转：学生→/student 商家→/merchant 管理员→/admin
function goHome(userInfo) {
  const target = route.query.redirect || ROLE_HOME[userInfo.role] || '/'
  router.replace(target)
}

async function handleLogin() {
  await formRef.value.validate().catch(() => Promise.reject())
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    userStore.setLogin({ token: res.data.token, userInfo: res.data })
    ElMessage.success('登录成功')
    goHome(res.data)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await regFormRef.value.validate().catch(() => Promise.reject())
  regLoading.value = true
  try {
    const res = await register({ ...regForm })
    userStore.setLogin({ token: res.data.token, userInfo: res.data })
    ElMessage.success('注册成功')
    showRegister.value = false
    goHome(res.data)
  } finally {
    regLoading.value = false
  }
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
