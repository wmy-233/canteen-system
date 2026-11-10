<template>
  <div class="page">
    <!-- 总览卡片 -->
    <div class="stat-cards">
      <div v-for="s in overviewCards" :key="s.label" class="stat-card">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <span>平台交易趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadTrend">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendEl" v-loading="trendLoading" class="chart-lg" />
    </el-card>

    <div class="row">
      <el-card shadow="never" class="panel">
        <template #header>档口经营排行</template>
        <el-table :data="stallRanking" stripe height="300">
          <el-table-column type="index" width="50" />
          <el-table-column prop="stallName" label="档口" min-width="130" />
          <el-table-column label="营业额" width="110">
            <template #default="{ row }">¥{{ fmt(row.gmv) }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单数" width="90" />
        </el-table>
      </el-card>
      <el-card shadow="never" class="panel">
        <template #header>🔥 全平台热销 Top</template>
        <div class="hot-list">
          <div v-for="(d, i) in topDishes" :key="d.dishId" class="hot-item">
            <span class="hot-rank" :class="'top' + (i + 1)">{{ i + 1 }}</span>
            <span class="hot-name">{{ d.name }}</span>
            <span class="hot-count">已售 {{ d.saleCount }} 份</span>
          </div>
          <el-empty v-if="topDishes.length === 0" description="暂无数据" :image-size="50" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { statsOverview, statsStallRanking, statsTrend } from '../../api/admin'

const fmt = (v) => Number(v ?? 0).toFixed(2)

const overview = ref(null)
const stallRanking = ref([])
const days = ref(7)
const trendLoading = ref(false)
const trendEl = ref(null)
let chart = null

const overviewCards = computed(() => overview.value ? [
  { label: '累计营业额(元)', value: fmt(overview.value.totalGmv) },
  { label: '累计订单数', value: overview.value.totalOrders ?? '-' },
  { label: '注册用户', value: overview.value.totalUsers ?? '-' },
  { label: '营业中档口', value: overview.value.openStalls ?? '-' },
  { label: '今日营业额(元)', value: fmt(overview.value.todayGmv) },
  { label: '今日订单量', value: overview.value.todayOrders ?? '-' }
] : [])

const topDishes = computed(() => overview.value?.topDishes || [])

const AXIS = { color: '#9b9d84' }

async function loadTrend() {
  trendLoading.value = true
  try {
    const res = await statsTrend({ days: days.value })
    const list = (res.data || []).slice().sort((a, b) => (a.statDate || '').localeCompare(b.statDate || ''))
    chart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 60, right: 60, top: 40, bottom: 30 },
      legend: { data: ['营业额', '订单量'], textStyle: AXIS },
      xAxis: { type: 'category', data: list.map(d => d.statDate || '-'), axisLabel: { color: AXIS.color } },
      yAxis: [
        { type: 'value', name: '营业额(元)', axisLabel: { color: AXIS.color }, splitLine: { lineStyle: { color: '#eceada' } } },
        { type: 'value', name: '订单数', axisLabel: { color: AXIS.color }, splitLine: { show: false } }
      ],
      series: [
        {
          name: '营业额', type: 'line', smooth: true, data: list.map(d => Number(d.turnover)),
          lineStyle: { color: '#93977a', width: 3 }, itemStyle: { color: '#93977a' },
          areaStyle: { color: 'rgba(147,151,122,0.18)' }
        },
        {
          name: '订单量', type: 'bar', yAxisIndex: 1, data: list.map(d => d.orderCount),
          itemStyle: { color: '#c6c9a4', borderRadius: [4, 4, 0, 0] }
        }
      ]
    }, true)
  } finally {
    trendLoading.value = false
  }
}

onMounted(async () => {
  const [a, b] = await Promise.all([statsOverview(), statsStallRanking()])
  overview.value = a.data
  stallRanking.value = (b.data || []).slice().sort((x, y) => Number(y.gmv) - Number(x.gmv))
  chart = echarts.init(trendEl.value)
  await loadTrend()
  window.addEventListener('resize', onResize)
})

function onResize() { chart?.resize(); }
onBeforeUnmount(() => { window.removeEventListener('resize', onResize); chart?.dispose() })
</script>

<style scoped>
.page { max-width: 1150px; margin: 0 auto; }
.stat-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}
@media (max-width: 1100px) { .stat-cards { grid-template-columns: repeat(3, 1fr); } }
.stat-card {
  padding: 18px 16px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.stat-value {
  font-size: 21px;
  font-weight: 700;
  color: #5e5f4e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-label { margin-top: 4px; font-size: 12px; color: #9b9d84; }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
  margin-bottom: 18px;
}
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.chart-lg { height: 320px; width: 100%; }
.row { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
@media (max-width: 1000px) { .row { grid-template-columns: 1fr; } }
.hot-list { display: flex; flex-direction: column; }
.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 4px;
  border-bottom: 1px dashed #eceada;
  font-size: 13.5px;
}
.hot-item:last-child { border-bottom: none; }
.hot-rank {
  width: 22px; height: 22px;
  display: grid; place-items: center;
  border-radius: 7px;
  background: #efede1;
  color: #8a8b7a;
  font-size: 12px;
  font-weight: 700;
}
.hot-rank.top1 { background: #d9a05b; color: #fff; }
.hot-rank.top2 { background: #b3b79a; color: #fff; }
.hot-rank.top3 { background: #c9a37c; color: #fff; }
.hot-name { flex: 1; color: #4f5040; }
.hot-count { color: #9b9d84; font-size: 12px; }
</style>
