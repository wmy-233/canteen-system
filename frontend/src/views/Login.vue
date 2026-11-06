<template>
  <div class="login-page">
    <!-- 莫兰迪极光动态背景：三团缓慢漂移的彩色光晕 + 飘浮气泡 -->
    <div class="aurora">
      <span class="blob blob-1"></span>
      <span class="blob blob-2"></span>
      <span class="blob blob-3"></span>
      <span v-for="n in 14" :key="n" class="bubble" :style="bubbleStyle(n)">🍚</span>
    </div>

    <!-- 登录卡片（玻璃拟态） -->
    <div class="login-card">
      <div class="badge">🍽️ 智慧校园 · 一卡通</div>
      <h1 class="title">智慧校园食堂点餐系统</h1>
      <p class="subtitle">Serving warmth, one bowl at a time</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" clearable :prefix-icon="'User'" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password :prefix-icon="'Lock'" size="large" />
        </el-form-item>
        <el-form-item>
          <el-button class="submit-btn" type="primary" size="large" :loading="loading" round @click="handleLogin">
            进入食堂 →
          </el-button>
        </el-form-item>
      </el-form>

      <div class="divider"><span>还没有账号？</span></div>
      <el-button class="register-btn" text type="primary" @click="showRegister = true">
        注册一个新账号，开启校园食光 ✨
      </el-button>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegister" title="🍜 加入智慧校园食堂" width="420px" class="reg-dialog">
      <p class="reg-tip">注册后默认为学生账号，即刻探索全校档口美食</p>
      <el-form ref="regFormRef" :model="regForm" :rules="rules" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model="regForm.username" placeholder="4-20 位字母数字" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="regForm.nickname" placeholder="同学都怎么称呼你？" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="regForm.password" type="password" placeholder="6-20 位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="showRegister = false">取消</el-button>
        <el-button type="primary" round :loading="regLoading" @click="handleRegister">注册并进入</el-button>
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
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

// 让每个气泡随机分布、随机速度，避免同屏移动整齐划一
function bubbleStyle(n) {
  const seed = (n * 137) % 100
  return {
    left: `${(seed * 3.7 + n * 4) % 96}%`,
    animationDuration: `${16 + (seed % 10) * 2.2}s`,
    animationDelay: `${-(seed % 9) * 1.8}s`,
    fontSize: `${14 + (seed % 4) * 5}px`
  }
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
/* ---- 鼠尾草绿莫兰迪色板：主色 #93977A / 米白底 #F6F4EC ---- */
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #f6f4ec 0%, #eceadf 50%, #e7e4d6 100%);
}

/* ---------- 极光光晕 ---------- */
.aurora {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.55;
  will-change: transform;
}
.blob-1 {
  width: 46vw; height: 46vw;
  left: -12vw; top: -10vw;
  background: radial-gradient(circle, #a9ae8e 0%, transparent 70%);
  animation: drift1 22s ease-in-out infinite alternate;
}
.blob-2 {
  width: 40vw; height: 40vw;
  right: -10vw; bottom: -12vw;
  background: radial-gradient(circle, #c6c9a4 0%, transparent 70%);
  animation: drift2 26s ease-in-out infinite alternate;
}
.blob-3 {
  width: 30vw; height: 30vw;
  right: 18vw; top: -8vw;
  background: radial-gradient(circle, #bcb9a1 0%, transparent 70%);
  animation: drift3 19s ease-in-out infinite alternate;
}
@keyframes drift1 { to { transform: translate(9vw, 12vh) scale(1.15); } }
@keyframes drift2 { to { transform: translate(-11vw, -9vh) scale(1.1); } }
@keyframes drift3 { to { transform: translate(-6vw, 10vh) scale(0.9); } }

/* 飘浮的美食气泡 */
.bubble {
  position: absolute;
  top: 102%;
  opacity: 0.5;
  animation: rise linear infinite;
}
@keyframes rise {
  0%   { transform: translateY(0) rotate(0deg); opacity: 0; }
  8%   { opacity: 0.5; }
  92%  { opacity: 0.35; }
  100% { transform: translateY(-115vh) rotate(22deg); opacity: 0; }
}

/* ---------- 登录卡片（玻璃拟态） ---------- */
.login-card {
  position: relative;
  width: 396px;
  padding: 42px 40px 30px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
  box-shadow:
    0 20px 60px rgba(120, 122, 96, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  animation: card-in 0.7s cubic-bezier(0.22, 1, 0.36, 1);
  z-index: 1;
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(26px) scale(0.97); }
}
.badge {
  display: inline-block;
  margin-bottom: 14px;
  padding: 4px 12px;
  font-size: 12px;
  letter-spacing: 1px;
  color: #77786a;
  background: rgba(147, 151, 122, 0.18);
  border: 1px solid rgba(147, 151, 122, 0.35);
  border-radius: 999px;
}
.title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 2px;
  /* 流动渐变色标题 */
  background: linear-gradient(90deg, #83896a 0%, #93977a 30%, #b3b79a 60%, #c2c0a8 100%);
  background-size: 220% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: shimmer 7s linear infinite;
}
@keyframes shimmer { to { background-position: 220% 0; } }
.subtitle {
  margin: 0 0 26px;
  font-size: 12.5px;
  color: #9b9d84;
  letter-spacing: 1px;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  background: #93977a;
  border-color: #93977a;
  box-shadow: 0 6px 16px rgba(147, 151, 122, 0.4);
}
.submit-btn:hover,
.submit-btn:focus {
  background: #83896a;
  border-color: #83896a;
}
.register-btn {
  width: 100%;
  color: #8a8f70 !important;
}

.divider {
  display: flex;
  align-items: center;
  margin: 18px 0 4px;
  color: #b3b0b8;
  font-size: 12px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #dedccc;
}
.divider span {
  padding: 0 10px;
}

/* 登录卡片内部标签贴纸视觉 */
.login-card :deep(.el-form-item__label) {
  color: #8a8b7a;
  font-size: 13px;
}
.login-card :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.75);
  box-shadow: 0 0 0 1px #e3e1d2 inset;
  transition: box-shadow 0.25s ease;
}
.login-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px #93977a inset;
}

/* 注册弹窗微调 */
.reg-tip {
  margin: 0 0 14px;
  padding: 8px 12px;
  font-size: 12.5px;
  color: #7d8168;
  background: rgba(147, 151, 122, 0.14);
  border-radius: 8px;
}
</style>
