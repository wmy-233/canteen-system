<template>
  <div class="login-page" :class="'p-' + userStore.portal">
    <!-- 动态极光背景：随门户主色联动 -->
    <div class="aurora">
      <span class="blob blob-1"></span>
      <span class="blob blob-2"></span>
      <span class="blob blob-3"></span>
      <span v-for="n in 12" :key="n" class="bubble">{{ portalEmoji }}</span>
    </div>

    <!-- 三端门户选择 -->
    <div class="portal-bar">
      <button
        v-for="p in portals" :key="p.key" type="button"
        class="portal-tab" :class="[{ active: userStore.portal === p.key }, p.key]"
        @click="pick(p.key)"
      >
        <span class="pt-emoji">{{ p.emoji }}</span>
        <span class="pt-text">
          <b>{{ p.label }}</b>
          <i>{{ p.desc }}</i>
        </span>
      </button>
    </div>

    <!-- 登录卡片（玻璃拟态，主题色随门户变化） -->
    <div class="login-card">
      <div class="badge">{{ currentPortal?.emoji }} {{ currentPortal?.label }} · 智慧校园食堂</div>
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
            {{ currentPortal?.label }}登 录 →
          </el-button>
        </el-form-item>
      </el-form>

      <div class="divider"><span>还没有账号？</span></div>
      <el-button v-if="userStore.portal !== 'ADMIN'" class="register-btn" text type="primary" @click="openRegister">
        {{ userStore.portal === 'STUDENT' ? '注册一个新账号，开启校园食光 ✨' : '商家入驻申请（提交后等管理员审核） 🏪' }}
      </el-button>
      <div v-else class="admin-note">🔒 管理员账号不开放注册，由超级管理员在「用户管理」中授权</div>

      <!-- 演示账号一键填入 -->
      <div class="demo-row">
        <span class="demo-tip">演示账号：</span>
        <button v-for="d in demoAccounts" :key="d.u" type="button" class="demo-chip" @click="fillDemo(d)">
          {{ d.emoji }} {{ d.u }}
        </button>
      </div>
    </div>

    <!-- 注册弹窗：学生注册 / 商家入驻 -->
      <el-dialog v-model="showRegister" :title="userStore.portal === 'STUDENT' ? '🍜 加入校园食堂' : '🏪 商家入驻申请'" width="440px" class="reg-dialog">
        <p v-if="userStore.portal === 'STUDENT'" class="reg-tip">注册后默认为<b>学生账号</b>，即刻探索全校档口美食</p>
        <div v-else class="reg-tip merchant">
          入驻流程：<b>① 提交申请</b> → ② 管理员审核档口 → ③ 审核通过即可上架菜品、开门营业
        </div>

        <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-position="top" @keyup.enter="handleRegister">
          <el-form-item label="账号" prop="username">
            <el-input v-model="regForm.username" placeholder="3-20 位字母数字下划线" clearable />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="regForm.nickname" :placeholder="userStore.portal === 'STUDENT' ? '同学都怎么称呼你？' : '店铺联系人称呼'" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="regForm.password" type="password" placeholder="6-20 位" show-password />
          </el-form-item>

          <!-- 商家专属：档口申请信息 -->
          <template v-if="userStore.portal === 'MERCHANT'">
            <el-divider content-position="left">档口申请信息</el-divider>
            <el-form-item label="档口名称" prop="stallName">
              <el-input v-model="regForm.stallName" placeholder="如：香满楼卤味档（2-30字）" maxlength="30" show-word-limit />
            </el-form-item>
            <el-form-item label="所属食堂" prop="canteenId">
              <el-select v-model="regForm.canteenId" placeholder="选择入驻的食堂" style="width:100%">
                <el-option v-for="c in canteenOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="regForm.phone" placeholder="11 位手机号（审核沟通用）" maxlength="11" />
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <el-button round @click="showRegister = false">取消</el-button>
          <el-button round type="primary" :loading="regLoading" @click="handleRegister">
            {{ userStore.portal === 'STUDENT' ? '注册并进入' : '提交入驻申请' }}
          </el-button>
        </template>
      </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, registerMerchant } from '../api/user'
import { canteenList } from '../api/basic'
import { useUserStore } from '../stores/user'
import { ROLE_HOME } from '../router'

const PORTALS = {
  STUDENT: { key: 'STUDENT', label: '学生端', emoji: '🍱', desc: '点餐 · 卡包 · 校园券', accent: '#93977a' },
  MERCHANT: { key: 'MERCHANT', label: '商家端', emoji: '🏪', desc: '菜品 · 出餐 · 生意', accent: '#a98467' },
  ADMIN: { key: 'ADMIN', label: '管理端', emoji: '📊', desc: '审核 · 调度 · 全局', accent: '#7e94a8' }
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const portals = Object.values(PORTALS)
const currentPortal = computed(() => PORTALS[userStore.portal] || PORTALS.STUDENT)
const portalEmoji = computed(() => ({ STUDENT: '🍚', MERCHANT: '🍜', ADMIN: '📚' }[userStore.portal] || '🍚'))

function pick(key) { userStore.setPortal(key) }

onMounted(() => {
  if (!userStore.portal) userStore.setPortal('STUDENT')
})

const formRef = ref()
const regFormRef = ref()
const loading = ref(false)
const regLoading = ref(false)
const showRegister = ref(false)

const form = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', nickname: '', password: '', stallName: '', canteenId: undefined, phone: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

// 注册表单规则：与后端 @Validated 注解严格一致，前端先拦一道，后端兜底
const ACCOUNT_RE = /^[a-zA-Z0-9_]{3,20}$/
const PHONE_RE = /^1[3-9]\d{9}$/
const regRules = computed(() => ({
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: ACCOUNT_RE, message: '账号为3-20位字母数字下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  // 商家入驻专属
  stallName: [
    { required: true, message: '请填写档口名称', trigger: 'blur' },
    { min: 2, max: 30, message: '档口名2-30字', trigger: 'blur' }
  ],
  canteenId: [{ required: true, message: '请选择所属食堂', trigger: 'change' }],
  phone: [
    { pattern: PHONE_RE, message: '手机号格式不正确', trigger: 'blur' }
  ]
}))

// 商家入驻表单的食堂选项（公开接口 /api/basic/canteen/list 登录即可，未登录注册前经 request 也可拿——
// 若 401 则兜底不展示列表并由用户联系管理员；实际上 canteen list 未挂权限，正常可取）
const canteenOptions = ref([])
async function loadCanteens() {
  try {
    const res = await canteenList()
    canteenOptions.value = res.data || []
  } catch { /* 注册前无 token，可选接口失败时静默，表单仍可提交走后端校验 */ }
}

function openRegister() {
  Object.assign(regForm, { username: '', nickname: '', password: '', stallName: '', canteenId: undefined, phone: '' })
  if (userStore.portal === 'MERCHANT' && canteenOptions.value.length === 0) loadCanteens()
  showRegister.value = true
}

const demoAccounts = [
  { u: 'student', emoji: '🍱', portal: 'STUDENT' },
  { u: 'merchant', emoji: '🏪', portal: 'MERCHANT' },
  { u: 'admin', emoji: '📊', portal: 'ADMIN' }
]

function fillDemo(d) {
  userStore.setPortal(d.portal)
  form.username = d.u
  form.password = '123456'
}

// 登录成功后：若所选门户与实际角色一致直接进；不一致则提示实际身份跳对应端
function goHome(userInfo) {
  if (userInfo.role !== userStore.portal && PORTALS[userInfo.role]) {
    ElMessage.info(`你的身份是${PORTALS[userInfo.role].label}，已为你切换`)
  }
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
    const isMerchant = userStore.portal === 'MERCHANT'
    const res = isMerchant
      ? await registerMerchant({
          username: regForm.username,
          password: regForm.password,
          nickname: regForm.nickname || undefined,
          phone: regForm.phone || undefined,
          stallName: regForm.stallName,
          canteenId: regForm.canteenId
        })
      : await register({ ...regForm })
    userStore.setLogin({ token: res.data.token, userInfo: res.data })
    showRegister.value = false
    if (isMerchant) {
      ElMessage.success('入驻申请已提交！档口待管理员审核，通过后即可营业')
    } else {
      ElMessage.success('注册成功')
    }
    goHome(res.data)
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
/* 门户主题色：CSS 变量驱动整卡换肤 */
.login-page {
  --ac: #93977a;
  --ac-soft: #a4a88b;
  --ac-glow: rgba(147, 151, 122, 0.55);
}
.login-page.p-MERCHANT { --ac: #a98467; --ac-soft: #bb9984; --ac-glow: rgba(169, 132, 103, 0.55); }
.login-page.p-ADMIN   { --ac: #7e94a8; --ac-soft: #97aabb; --ac-glow: rgba(126, 148, 168, 0.55); }

.login-page {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 22px;
  height: 100vh;
  overflow: hidden;
  background: linear-gradient(135deg, #f6f4ec 0%, #eceadf 50%, #e7e4d6 100%);
}

/* ---------- 极光（颜色绑定 --ac 系） ---------- */
.aurora {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  will-change: transform;
  transition: background 0.5s ease;
}
.blob-1 {
  width: 46vw; height: 46vw;
  left: -12vw; top: -10vw;
  background: radial-gradient(circle, var(--ac-glow) 0%, transparent 70%);
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
  background: radial-gradient(circle, var(--ac-glow) 0%, transparent 70%);
  animation: drift3 19s ease-in-out infinite alternate;
}
@keyframes drift1 { to { transform: translate(9vw, 12vh) scale(1.15); } }
@keyframes drift2 { to { transform: translate(-11vw, -9vh) scale(1.1); } }
@keyframes drift3 { to { transform: translate(-6vw, 10vh) scale(0.9); } }

.bubble {
  position: absolute;
  top: 102%;
  font-size: 18px;
  opacity: 0;
  animation: rise linear infinite;
}
@keyframes rise {
  0%   { transform: translateY(0) rotate(0deg); opacity: 0; }
  10%  { opacity: 0.45; }
  92%  { opacity: 0.3; }
  100% { transform: translateY(-112vh) rotate(22deg); opacity: 0; }
}
/* 气泡随机分布与速度 */
.bubble:nth-child(3n+1) { left: 8%;  animation-duration: 17s; animation-delay: -2s; font-size: 22px; }
.bubble:nth-child(3n+2) { left: 34%; animation-duration: 21s; animation-delay: -9s; }
.bubble:nth-child(3n)   { left: 58%; animation-duration: 15s; animation-delay: -5s; font-size: 15px; }
.bubble:nth-child(4n)   { left: 76%; animation-duration: 19s; animation-delay: -12s; }
.bubble:nth-child(5n)   { left: 90%; animation-duration: 18s; animation-delay: -1s; font-size: 24px; }
.bubble:nth-child(6n)   { left: 20%; animation-duration: 16s; animation-delay: -7s; }
.bubble:nth-child(7n)   { left: 45%; animation-duration: 22s; animation-delay: -15s; }
.bubble:nth-child(8n)   { left: 65%; animation-duration: 14s; animation-delay: -4s; font-size: 20px; }
.bubble:nth-child(9n)   { left: 85%; animation-duration: 20s; animation-delay: -11s; }
.bubble:nth-child(10n)  { left: 15%; animation-duration: 18s; animation-delay: -6s; font-size: 16px; }
.bubble:nth-child(11n)  { left: 50%; animation-duration: 23s; animation-delay: -18s; font-size: 21px; }
.bubble:nth-child(12n)  { left: 70%; animation-duration: 15s; animation-delay: -13s; }

/* ---------- 三端门户选择条 ---------- */
.portal-bar {
  display: flex;
  gap: 14px;
  z-index: 2;
  animation: portal-in 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes portal-in { from { opacity: 0; transform: translateY(-14px); } }
.portal-tab {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px 10px 14px;
  border-radius: 14px;
  border: 1.5px solid #e0ddd0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  text-align: left;
}
.portal-tab:hover { transform: translateY(-3px); box-shadow: 0 8px 18px rgba(120, 122, 96, 0.16); }
.portal-tab .pt-emoji { font-size: 26px; }
.portal-tab .pt-text { display: flex; flex-direction: column; }
.portal-tab .pt-text b { font-size: 14px; color: #5e5f4e; }
.portal-tab .pt-text i { font-style: normal; font-size: 11px; color: #9b9d84; margin-top: 2px; }

/* 每个门户选中时使用各自专属色打勾描边 */
.portal-tab.active.STUDENT { border-color: #93977a; background: rgba(147, 151, 122, 0.13); box-shadow: 0 6px 16px rgba(147, 151, 122, 0.3); }
.portal-tab.active.MERCHANT { border-color: #a98467; background: rgba(169, 132, 103, 0.12); box-shadow: 0 6px 16px rgba(169, 132, 103, 0.3); }
.portal-tab.active.ADMIN { border-color: #7e94a8; background: rgba(126, 148, 168, 0.13); box-shadow: 0 6px 16px rgba(126, 148, 168, 0.3); }

/* ---------- 登录卡片 ---------- */
.login-card {
  position: relative;
  width: 396px;
  padding: 36px 40px 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
  box-shadow: 0 20px 60px rgba(120, 122, 96, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.8);
  animation: card-in 0.7s cubic-bezier(0.22, 1, 0.36, 1);
  z-index: 1;
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(26px) scale(0.97); }
}
.badge {
  display: inline-block;
  margin-bottom: 12px;
  padding: 4px 12px;
  font-size: 12px;
  letter-spacing: 1px;
  color: var(--ac);
  background: color-mix(in srgb, var(--ac) 14%, white);
  border: 1px solid color-mix(in srgb, var(--ac) 35%, transparent);
  border-radius: 999px;
  transition: color 0.4s;
}
.title {
  margin: 0 0 6px;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 2px;
  background: linear-gradient(90deg, #83896a 0%, var(--ac) 35%, var(--ac-soft) 65%, #b3b79a 100%);
  background-size: 220% 100%;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: shimmer 7s linear infinite;
}
@keyframes shimmer { to { background-position: 220% 0; } }
.subtitle {
  margin: 0 0 22px;
  font-size: 12.5px;
  color: #a29ea8;
  letter-spacing: 1px;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  background: var(--ac);
  border-color: var(--ac);
  box-shadow: 0 6px 16px var(--ac-glow);
  transition: all 0.4s;
}
.submit-btn:hover, .submit-btn:focus {
  background: var(--ac-soft);
  border-color: var(--ac-soft);
}
.register-btn { width: 100%; color: var(--ac) !important; }

.divider {
  display: flex;
  align-items: center;
  margin: 16px 0 4px;
  color: #b3b0b8;
  font-size: 12px;
}
.divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: #dedccc; }
.divider span { padding: 0 10px; }

/* 演示账号 chips */
.demo-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.demo-tip { font-size: 11.5px; color: #9b9d84; }
.demo-chip {
  border: 1px dashed #c9c8b2;
  background: #fbfaf2;
  color: #7d8168;
  font-size: 11.5px;
  padding: 3px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}
.demo-chip:hover { background: var(--ac); color: #fff; border-style: solid; }

/* 卡片内 Element 输入框随主题 */
.login-card :deep(.el-form-item__label) { color: #8a8b7a; font-size: 13px; }
.login-card :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.75);
  box-shadow: 0 0 0 1px #e3e1d2 inset;
  transition: box-shadow 0.25s ease;
}
.login-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--ac) inset;
}

/* 注册弹窗 */
.reg-tip {
  margin: 0 0 14px;
  padding: 8px 12px;
  font-size: 12.5px;
  color: #7d8168;
  background: rgba(147, 151, 122, 0.14);
  border-radius: 8px;
}
.reg-tip.merchant {
  line-height: 1.7;
  background: rgba(169, 132, 103, 0.12);
  color: #8a6b52;
}
.admin-note {
  padding: 10px 12px;
  font-size: 12.5px;
  color: #6d8093;
  background: rgba(126, 148, 168, 0.12);
  border-radius: 10px;
  text-align: center;
}
</style>
