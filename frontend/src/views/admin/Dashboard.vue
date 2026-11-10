<template>
  <div class="page admin-dash">
    <!-- 行政总览 hero -->
    <div class="hero">
      <div>
        <h2 class="hero-title">平台运营总览 🏙️</h2>
        <p class="hero-sub">把每一个档口，都经营得更好 · {{ greeting }}</p>
        <div class="hero-btns">
          <el-button round class="hb primary" @click="$router.push('/admin/merchants')">待审核档口 →</el-button>
          <el-button round class="hb" @click="$router.push('/admin/riders')">配送调度</el-button>
        </div>
      </div>
      <div class="clock-card">
        <div class="ck-time">{{ clock }}</div>
        <div class="ck-date">{{ dateText }}</div>
      </div>
    </div>

    <!-- 核心指标 -->
    <div class="ov-cards">
      <div v-for="c in cards" :key="c.label" class="ov-card">
        <div class="ovv">{{ c.value }}</div>
        <div class="ovl">{{ c.label }}</div>
      </div>
    </div>

    <div class="row">
      <!-- 趋势 -->
      <el-card shadow="never" class="panel">
        <template #header>📊 平台交易趋势（近7天）</template>
        <div ref="trendEl" v-loading="trendLoading" class="chart" />
      </el-card>
      <!-- 热销 -->
      <el-card shadow="never" class="panel">
        <template #header>🏆 全平台热销 Top</template>
        <div v-for="(d, i) in topDishes" :key="d.dishId" class="hot-item">
          <span class="hrank" :class="'t' + (i + 1)">{{ i + 1 }}</span>
          <span class="hname">{{ d.name }}</span>
          <span class="hcount">{{ d.saleCount }} 份</span>
        </div>
        <el-empty v-if="topDishes.length === 0" description="暂无数据" :image-size="46" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { statsOverview, statsTrend } from '../../api/admin'

const fmt = (v) => Number(v ?? 0).toFixed(2)

const greeting = computed(() => {
  const h = new Date().getHours()
  return h < 9 ? '晨会开始前' : h < 12 ? '上午巡查中' : h < 17 ? '午后数据看板' : '日结时段'
})

// 时钟
const clock = ref('--:--:--')
const dateText = ref('')
let timer = null

const overview = ref(null)
const trendLoading = ref(false)
const trendEl = ref(null)
let chart = null

const cards = computed(() => overview.value ? [
  { label: '累计营业额(元)', value: fmt(overview.value.totalGmv) },
  { label: '累计订单', value: overview.value.totalOrders ?? '-' },
  { label: '注册用户', value: overview.value.totalUsers ?? '-' },
  { label: '营业中档口', value: overview.value.openStalls ?? '-' },
  { label: '今日营业额', value: fmt(overview.value.todayGmv) },
  { label: '今日订单', value: overview.value.todayOrders ?? '-' }
] : [{ label: '加载中', value: '·' }])

const topDishes = computed(() => overview.value?.topDishes?.slice(0, 8) || [])

const AXIS = { color: '#9b9d84' }

async function loadTrend() {
  trendLoading.value = true
  try {
    const res = await statsTrend({ days: 7 })
    const list = (res.data || []).slice().sort((a, b) => (a.statDate || '').localeCompare(b.statDate || ''))
    chart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 56, right: 52, top: 36, bottom: 28 },
      legend: { data: ['营业额', '订单量'], textStyle: AXIS, top: 0 },
      xAxis: { type: 'category', data: list.map(d => (d.statDate || '').slice(5) || '-'), axisLabel: { color: AXIS.color } },
      yAxis: [
        { type: 'value', name: '营业额', axisLabel: { color: AXIS.color }, splitLine: { lineStyle: { color: '#eceada' } } },
        { type: 'value', name: '订单', axisLabel: { color: AXIS.color }, splitLine: { show: false } }
      ],
      series: [
        {
          name: '营业额', type: 'line', smooth: true, data: list.map(d => Number(d.turnover)),
          lineStyle: { color: '#7e94a8', width: 3 }, itemStyle: { color: '#7e94a8' },
          areaStyle: { color: 'rgba(126,148,168,0.18)' }
        },
        {
          name: '订单量', type: 'bar', yAxisIndex: 1, data: list.map(d => d.orderCount),
          itemStyle: { color: '#c2cccd', borderRadius: [4, 4, 0, 0] }
        }
      ]
    }, true)
  } finally {
    trendLoading.value = false
  }
}

function tick() {
  const d = new Date()
  clock.value = d.toTimeString().slice(0, 8)
  const wk = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  dateText.value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} 星期${wk}`
}

onMounted(async () => {
  tick()
  timer = setInterval(tick, 1000)
  const res = await statsOverview()
  overview.value = res.data
  chart = echarts.init(trendEl.value)
  await loadTrend()
  window.addEventListener('resize', onResize)
})

function onResize() { chart?.resize() }
onBeforeUnmount(() => {
  clearInterval(timer)
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<style scoped>
/* 管理端主题：雾蓝灰 */
.admin-dash { --ac: #7e94a8; --ac-soft: #6d8093; }
.page { max-width: 1150px; margin: 0 auto; }

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 30px;
  border-radius: 18px;
  background: linear-gradient(120deg, #7e94a8 0%, #97aabb 60%, #c5d2d9 100%);
  box-shadow: 0 14px 34px rgba(126, 148, 168, 0.4);
  margin-bottom: 16px;
}
.hero-title { margin: 0; font-size: 21px; color: #37454f; }
.hero-sub { margin: 7px 0 0; font-size: 13px; color: #4f5f6b; }
.hero-btns { margin-top: 16px; }
.hb.primary { background: #fffdf2; border: none; color: #4a5b68; font-weight: 600; }
.hb.primary:hover { background: #fff; color: #37454f; }
.hb { background: rgba(255, 255, 255, 0.55); border: none; color: #4a5b68; font-weight: 600; }
.hb:hover { background: rgba(255, 255, 255, 0.85); color: #37454f; }

.clock-card {
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 14px;
  padding: 14px 22px;
  text-align: center;
  color: #fff;
  min-width: 200px;
  font-variant-numeric: tabular-nums;
}
.ck-time { font-size: 28px; font-weight: 800; letter-spacing: 2px; }
.ck-date { font-size: 12px; opacity: 0.85; margin-top: 3px; }

.ov-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
@media (max-width: 1100px) { .ov-cards { grid-template-columns: repeat(3, 1fr); } }
.ov-card {
  background: #fff;
  border-radius: 13px;
  padding: 15px 16px;
  box-shadow: 0 4px 14px rgba(126, 148, 168, 0.16);
  position: relative;
  overflow: hidden;
}
.ov-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 4px;
  background: var(--ac);
}
.ovv {
  font-size: 19px;
  font-weight: 800;
  color: #5e5f4e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ovl { font-size: 12px; color: #9b9d84; margin-top: 3px; }

.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(126, 148, 168, 0.14);
}
.chart { height: 300px; width: 100%; }
.row { display: grid; grid-template-columns: 1.5fr 1fr; gap: 16px; }
@media (max-width: 1000px) { .row { grid-template-columns: 1fr; } }

.hot-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  border-bottom: 1px dashed #e6e9e2;
  font-size: 13px;
}
.hot-item:last-child { border-bottom: none; }
.hrank {
  width: 22px; height: 22px;
  display: grid; place-items: center;
  border-radius: 7px;
  font-size: 12px; font-weight: 700;
  background: #eef1f2; color: #83909a;
  flex: none;
}
.hrank.t1 { background: #d9a05b; color: #fff; }
.hrank.t2 { background: #9cb4c4; color: #fff; }
.hrank.t3 { background: #b7c6ce; color: #fff; }
.hname { flex: 1; color: #4f5040; }
.hcount { color: #9b9d84; font-size: 12px; }
</style>
