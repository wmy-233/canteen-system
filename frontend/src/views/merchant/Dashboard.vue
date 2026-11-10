<template>
  <div class="page merchant-dash">
    <!-- 驾驶舱 hero -->
    <div class="hero">
      <div class="hero-left">
        <h2 class="hero-title">经营驾驶舱 📈</h2>
        <p class="hero-sub">{{ stallName }} · {{ greeting }}</p>
        <div class="hero-btns">
          <el-button round class="hb primary" @click="$router.push('/merchant/orders')">处理出餐 →</el-button>
          <el-button round class="hb" @click="$router.push('/merchant/dishes')">管理菜品</el-button>
        </div>
      </div>
      <div class="traffic-ring">
        <el-progress type="dashboard" :percentage="pendingPct" :width="130" :color="ringColor">
          <template #default>
            <div class="ring-num">{{ pending.length }}</div>
            <div class="ring-label">待出餐</div>
          </template>
        </el-progress>
      </div>
    </div>

    <!-- 指标 -->
    <div class="kpi-cards">
      <div class="kpi"><div class="kv">¥{{ fmt(todayTurnover) }}</div><div class="kl">今日营业额</div></div>
      <div class="kpi"><div class="kv">{{ todayOrders.length }}</div><div class="kl">今日订单</div></div>
      <div class="kpi"><div class="kv">{{ countDone }}</div><div class="kl">今日已完成</div></div>
      <div class="kpi"><div class="kv">{{ countCancel }}</div><div class="kl">今日取消</div></div>
    </div>

    <!-- 最近订单 -->
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <span>🚦 实时订单（最新 6 单）</span>
          <el-button link type="primary" @click="$router.push('/merchant/orders')">全部订单 ›</el-button>
        </div>
      </template>
      <el-table :data="recent" stripe v-loading="loading" size="small">
        <el-table-column prop="pickupCode" label="出餐码" width="90">
          <template #default="{ row }">
            <span v-if="row.deliveryType === 0" class="code-chip">{{ row.pickupCode }}</span>
            <span v-else class="code-chip delivery">🛵 配送</span>
          </template>
        </el-table-column>
        <el-table-column label="菜品" min-width="150">
          <template #default="{ row }">{{ row.items?.map(i => `${i.dishName}×${i.quantity}`).join(' / ') }}</template>
        </el-table-column>
        <el-table-column label="实收" width="100" align="right">
          <template #default="{ row }"><b class="money">¥{{ fmt(row.payAmount) }}</b></template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单" width="170" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" round>{{ ORDER_STATUS[row.status] }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ORDER_STATUS } from '../../api/order'
import { orderPage, statsTrend, stallMine } from '../../api/merchant'
import { useUserStore } from '../../stores/user'

const fmt = (v) => Number(v ?? 0).toFixed(2)
const userStore = useUserStore()
const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 12 ? '早市正忙，稳住！' : h < 15 ? '午市高峰，注意出餐节奏' : '收档前记得核对账目'
})
void greeting

const stallName = ref('我的档口')
const pending = ref([])
const todayOrders = ref([])
const loading = ref(false)

const countDone = computed(() => todayOrders.value.filter(o => o.status === 1).length)
const countCancel = computed(() => todayOrders.value.filter(o => o.status === 2).length)
const todayTurnover = computed(() =>
  todayOrders.value.filter(o => o.status !== 2).reduce((s, o) => s + Number(o.payAmount), 0)
)
const pendingPct = computed(() => Math.min(100, pending.value.length * 12.5))
const ringColor = 'rgba(169,132,103,0.85)'

const recent = computed(() => todayOrders.value.slice(0, 6))
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    const [stall, p0, all] = await Promise.all([
      stallMine(),
      orderPage({ status: 0, pageNum: 1, pageSize: 50 }),
      orderPage({ pageNum: 1, pageSize: 50 })
    ])
    stallName.value = stall.data?.[0]?.name || '我的档口'
    pending.value = p0.data?.list || []
    todayOrders.value = (all.data?.list || []).filter(o => (o.createTime || '').slice(0, 10) === today)
  } finally {
    loading.value = false
  }
}

void statsTrend
onMounted(load)
</script>

<style scoped>
/* 商家端主题：莫兰迪驼金 */
.merchant-dash { --ac: #a98467; --ac-soft: #96735a; }
.page { max-width: 1080px; margin: 0 auto; }

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 30px;
  border-radius: 18px;
  background: linear-gradient(120deg, #a98467 0%, #bb9984 60%, #d9c1b0 100%);
  box-shadow: 0 14px 34px rgba(169, 132, 103, 0.4);
}
.hero-title { margin: 0; font-size: 21px; color: #4a3b30; }
.hero-sub { margin: 7px 0 0; font-size: 13px; color: #6d5648; }
.hero-btns { margin-top: 16px; }
.hb.primary { background: #fffdf2; border: none; color: #7a5c45; font-weight: 600; }
.hb.primary:hover { background: #fff; color: #5d4636; }
.hb { border-color: rgba(255, 255, 255, 0.6); color: #fff; }
.hb:hover { border-color: #fff; color: #fff; }
.traffic-ring { flex: none; }
.ring-num { font-size: 30px; font-weight: 800; color: #4a3b30; }
.ring-label { font-size: 11.5px; color: #6d5648; margin-top: 2px; }

.kpi-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin: 16px 0;
}
@media (max-width: 900px) { .kpi-cards { grid-template-columns: repeat(2, 1fr); } }
.kpi {
  background: #fff;
  border-radius: 13px;
  padding: 15px 18px;
  box-shadow: 0 4px 14px rgba(169, 132, 103, 0.16);
  position: relative;
  overflow: hidden;
}
.kpi::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 4px;
  background: var(--ac);
}
.kv { font-size: 22px; font-weight: 800; color: #5e5f4e; }
.kl { font-size: 12px; color: #9b9d84; margin-top: 3px; }

.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(169, 132, 103, 0.14);
}
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.code-chip {
  font-weight: 800;
  font-size: 15px;
  letter-spacing: 2px;
  color: #a98467;
  background: #faf4ee;
  padding: 3px 9px;
  border-radius: 8px;
}
.code-chip.delivery { letter-spacing: 0; font-size: 12.5px; }
.money { color: #b0714f; }
</style>
