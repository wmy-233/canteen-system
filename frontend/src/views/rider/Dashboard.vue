<template>
  <div class="rider-page">
    <div class="rider-hero">
      <div>
        <div class="rh-emoji">🛵</div>
        <div>
          <div class="rh-title">接单大厅</div>
          <div class="rh-sub">{{ me?.name || '骑手' }}，抢到单后请尽快取餐配送～ 超时未接的单由管理员兜底指派</div>
        </div>
      </div>
      <div class="rh-stat">
        <div class="rh-num">{{ pending.length }}</div>
        <div class="rh-label">待抢配送单</div>
      </div>
    </div>

    <el-card shadow="never" class="rider-card">
      <template #header>
        <div class="card-head">
          <span>🍱 可抢订单</span>
          <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>

      <el-empty v-if="!loading && pending.length === 0" description="暂时没有待抢订单，喝口水等等吧～" />

      <div v-for="o in pending" :key="o.id" class="order-row">
        <div class="or-left">
          <div class="or-top">
            <span class="or-no">{{ o.orderNo }}</span>
            <span class="or-stall">🏪 {{ o.stallName }}</span>
          </div>
          <div class="or-meta">
            实付 <b>¥{{ fmt(o.payAmount) }}</b>
            <span class="or-time">{{ o.createTime }}</span>
          </div>
        </div>
        <el-button type="primary" round size="small" :loading="grabbing === o.id" @click="grab(o)">
          立即抢单
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { riderGrab, riderMe, riderPending } from '../../api/rider'

const me = ref(null)
const pending = ref([])
const loading = ref(false)
const grabbing = ref(null)
const fmt = v => Number(v ?? 0).toFixed(2)

async function load() {
  loading.value = true
  try {
    const [p, m] = await Promise.all([riderPending(), riderMe()])
    pending.value = p.data || []
    me.value = m.data
  } finally {
    loading.value = false
  }
}

async function grab(o) {
  try {
    await ElMessageBox.confirm(`确认抢订单 ${o.orderNo}？抢到后请尽快到档口取餐。`, '抢单', { type: 'info' })
  } catch { return }
  grabbing.value = o.id
  try {
    await riderGrab(o.id)
    ElMessage.success('抢单成功！请在「我的配送」查看订单 🛵')
    await load()
  } catch (e) {
    // 并发失败：条件更新保证只有一人抢到
    ElMessage.warning(e?.message || '手慢了，该单可能已被其他骑手抢走')
    await load()
  } finally {
    grabbing.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.rider-page { max-width: 900px; margin: 0 auto; }
.rider-hero {
  display: flex; align-items: center; justify-content: space-between;
  padding: 22px 26px; border-radius: 16px; margin-bottom: 18px;
  background: linear-gradient(120deg, #7a9b8e, #94b2a6); color: #fff;
}
.rider-hero > div:first-child { display: flex; align-items: center; gap: 16px; }
.rh-emoji { font-size: 40px; }
.rh-title { font-size: 22px; font-weight: 700; }
.rh-sub { font-size: 12px; opacity: 0.9; margin-top: 4px; }
.rh-stat { text-align: center; }
.rh-num { font-size: 32px; font-weight: 700; }
.rh-label { font-size: 12px; opacity: 0.9; }
.rider-card { border-radius: 14px; }
.card-head { display: flex; align-items: center; justify-content: space-between; font-weight: 700; }
.order-row {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 14px 6px; border-bottom: 1px solid #eee;
}
.order-row:last-child { border-bottom: none; }
.or-top { display: flex; gap: 12px; align-items: center; }
.or-no { font-weight: 700; color: #4a4c3a; }
.or-stall { font-size: 13px; color: #7d8168; }
.or-meta { font-size: 13px; color: #7d8168; margin-top: 6px; }
.or-meta b { color: #c06b4a; }
.or-time { margin-left: 14px; color: #9b9d84; font-size: 12px; }
</style>
