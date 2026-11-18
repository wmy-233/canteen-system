<template>
  <div class="page">
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="待处理（待出餐）" name="0" />
      <el-tab-pane label="已完成" name="1" />
      <el-tab-pane label="已取消" name="2" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <div class="voice-bar">
      <el-button size="small" :type="voiceOn ? 'success' : 'info'" plain @click="toggleVoice">
        {{ voiceOn ? '🔊 新订单语音播报：开' : '🔇 新订单语音播报：关' }}
      </el-button>
      <span>每 15 秒检查新订单，忙的时候不用一直盯屏幕</span>
    </div>

    <div v-loading="loading" class="order-list">
      <el-card v-for="o in orders" :key="o.id" shadow="never" class="order-card">
        <div class="o-head">
          <div class="o-left">
            <span class="o-code-big" v-if="o.deliveryType === 0">{{ o.pickupCode }}</span>
            <span class="o-emoji" v-else>🛵</span>
            <div>
              <div class="o-user">{{ o.nickname }} · {{ o.items?.length || 0 }} 件菜品</div>
              <div class="o-no">{{ o.orderNo }} · {{ o.createTime }}</div>
            </div>
          </div>
          <div class="o-right">
            <el-tag v-if="o.deliveryType === 1" size="small" round>
              {{ o.deliveryStatus === 2 ? '已送达' : o.deliveryStatus === 1 ? `配送中·${o.riderName || ''}` : '待分配骑手' }}
            </el-tag>
            <el-tag :type="statusType(o.status)" round>{{ ORDER_STATUS[o.status] }}</el-tag>
          </div>
        </div>

        <div class="o-items">
          <span v-for="it in o.items" :key="it.id" class="oi-chip">{{ it.dishName }} ×{{ it.quantity }}</span>
        </div>

        <div class="o-foot">
          <span class="o-pay">实收 <b>¥{{ fmt(o.payAmount) }}</b><template v-if="o.discountAmount > 0">（券减 ¥{{ fmt(o.discountAmount) }}）</template></span>
          <el-button
            v-if="o.status === 0 && o.deliveryType === 0"
            type="primary" size="small" round @click="openScan(o)"
          >扫码核销 · 出餐</el-button>
          <el-tag v-if="o.status === 0 && o.deliveryType === 1" type="info" size="small" round>等待管理员分配骑手</el-tag>
        </div>
      </el-card>
      <el-empty v-if="!loading && orders.length === 0" description="暂时没有相关订单" />
    </div>

    <el-pagination
      v-model:current-page="pageNum" :page-size="pageSize" :total="total"
      layout="prev, pager, next" background class="pager" @current-change="load"
    />

    <!-- 扫码核销：扫码枪输入或手动粘贴二维码内容 -->
    <el-dialog v-model="scanVisible" title="📷 扫码核销出餐" width="440px">
      <div class="scan-box">
        <div class="scan-hint">请用扫码枪扫描学生订单页的二维码，或粘贴二维码内容</div>
        <el-input v-model="scanInput" placeholder="CANTEEN|订单号|取餐码|过期时间戳" size="large" @keyup.enter="doVerify" />
        <div class="scan-expect">本单订单号 <b>{{ scanOrder?.orderNo }}</b> · 取餐码 <b>{{ scanOrder?.pickupCode }}</b></div>
        <el-alert v-if="scanError" :title="scanError" type="error" :closable="false" show-icon />
        <div class="scan-tip">二维码 5 分钟自动刷新，过期需让学生重新出示</div>
      </div>
      <template #footer>
        <el-button @click="scanVisible = false">取消</el-button>
        <el-button type="primary" :loading="verifying" @click="doVerify">确认核销出餐</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ORDER_STATUS } from '../../api/order'
import { orderPage as merchantOrderPage, orderVerify } from '../../api/merchant'

const fmt = (v) => Number(v ?? 0).toFixed(2)
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')

const tab = ref('0')
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await merchantOrderPage({
      status: tab.value === 'all' ? undefined : Number(tab.value),
      pageNum: pageNum.value,
      pageSize
    })
    orders.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

// ---------- 扫码核销（解析学生端带时效的二维码） ----------
const scanVisible = ref(false)
const scanOrder = ref(null)
const scanInput = ref('')
const scanError = ref('')
const verifying = ref(false)

function openScan(o) {
  scanOrder.value = o
  scanInput.value = ''
  scanError.value = ''
  scanVisible.value = true
}

async function doVerify() {
  scanError.value = ''
  const parts = (scanInput.value || '').trim().split('|')
  if (parts.length !== 4 || parts[0] !== 'CANTEEN') {
    scanError.value = '二维码格式不正确，请重新扫描'
    return
  }
  const [, orderNo, pickupCode, expire] = parts
  if (Number(expire) < Date.now()) {
    scanError.value = '二维码已过期，请让学生重新出示'
    return
  }
  if (orderNo !== scanOrder.value?.orderNo || pickupCode !== scanOrder.value?.pickupCode) {
    scanError.value = '订单号或取餐码与本单不匹配，核销失败'
    return
  }
  verifying.value = true
  try {
    await orderVerify(scanOrder.value.id)
    ElMessage.success('扫码核销成功，该单已完成 ✅')
    scanVisible.value = false
    load()
  } finally {
    verifying.value = false
  }
}

// ---------- 新订单语音播报（浏览器 Web Speech API，无需后端） ----------
const voiceOn = ref(true)
let voiceTimer = null
let lastPending = null

function speak(text) {
  if (!voiceOn.value || !('speechSynthesis' in window)) return
  try {
    const u = new SpeechSynthesisUtterance(text)
    u.lang = 'zh-CN'
    u.rate = 1.05
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(u)
  } catch { /* 浏览器不支持时静默降级 */ }
}

function toggleVoice() {
  voiceOn.value = !voiceOn.value
  if (!voiceOn.value && 'speechSynthesis' in window) window.speechSynthesis.cancel()
  ElMessage.info(voiceOn.value ? '已开启新订单语音播报' : '已关闭语音播报')
}

async function pollPending() {
  try {
    const res = await merchantOrderPage({ status: 0, pageNum: 1, pageSize: 1 })
    const n = res.data?.total ?? 0
    if (lastPending !== null && n > lastPending) {
      speak(`你有 ${n - lastPending} 笔新订单，请尽快出餐`)
    }
    lastPending = n
  } catch { /* 轮询失败忽略 */ }
}

onMounted(() => {
  load()
  pollPending()
  voiceTimer = setInterval(pollPending, 15000)
})
onBeforeUnmount(() => {
  if (voiceTimer) clearInterval(voiceTimer)
  if ('speechSynthesis' in window) window.speechSynthesis.cancel()
})
</script>

<style scoped>
.page { max-width: 1080px; margin: 0 auto; }
.voice-bar { display: flex; align-items: center; gap: 10px; margin: -4px 0 12px; font-size: 12px; color: #9b9d84; }
.scan-box { display: flex; flex-direction: column; gap: 12px; }
.scan-hint, .scan-tip { font-size: 12px; color: #9b9d84; }
.scan-expect { font-size: 13px; color: #5e5f4e; }
.scan-expect b { color: #93977a; font-size: 15px; letter-spacing: 2px; }
.order-list { display: flex; flex-direction: column; gap: 14px; min-height: 180px; }
.order-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.o-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.o-left { display: flex; align-items: center; gap: 14px; }
.o-code-big {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 5px;
  color: #93977a;
  background: #f4f4e8;
  padding: 4px 14px;
  border-radius: 10px;
}
.o-emoji { font-size: 26px; }
.o-user { font-weight: 600; color: #4f5040; }
.o-no { font-size: 12px; color: #9b9d84; margin-top: 2px; }
.o-right { display: flex; gap: 8px; align-items: center; }
.o-items { display: flex; flex-wrap: wrap; gap: 6px; padding: 10px 0; }
.oi-chip {
  font-size: 12px;
  background: #f6f4ec;
  color: #5e5f4e;
  padding: 3px 10px;
  border-radius: 999px;
}
.o-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #e6e4d4;
  padding-top: 10px;
}
.o-pay b { color: #b0714f; font-size: 16px; }
.pager { margin-top: 16px; justify-content: center; }
</style>
