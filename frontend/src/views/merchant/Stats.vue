<template>
  <div class="page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <span>经营趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadTrend">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="14">近14天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendEl" v-loading="trendLoading" class="chart-lg" />
    </el-card>

    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <span>菜品销量榜</span>
          <el-radio-group v-model="topDays" size="small" @change="loadTop">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="topEl" v-loading="topLoading" class="chart-lg" />
      <el-empty v-if="!topLoading && !topData.length" description="暂无销量数据" />
    </el-card>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { statsTopDishes, statsTrend } from '../../api/merchant'

const days = ref(7)
const topDays = ref(7)
const trendEl = ref(null)
const topEl = ref(null)
const topData = ref([])
const trendLoading = ref(false)
const topLoading = ref(false)
let trendChart = null
let topChart = null

// 莫兰迪灰绿主题
const AXIS = { color: '#9b9d84' }

async function loadTrend() {
  trendLoading.value = true
  try {
    const res = await statsTrend({ days: days.value })
    const list = (res.data || []).slice().sort((a, b) => (a.statDate || '').localeCompare(b.statDate || ''))
    trendChart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 50, top: 40, bottom: 30 },
      legend: { data: ['营业额', '订单量'], textStyle: AXIS },
      xAxis: { type: 'category', data: list.map(d => d.statDate || '-'), axisLabel: { color: AXIS.color } },
      yAxis: [
        { type: 'value', name: '营业额(元)', axisLabel: { color: AXIS.color }, splitLine: { lineStyle: { color: '#eceada' } } },
        { type: 'value', name: '订单数', axisLabel: { color: AXIS.color }, splitLine: { show: false } }
      ],
      series: [
        {
          name: '营业额', type: 'line', smooth: true,
          data: list.map(d => Number(d.turnover)),
          lineStyle: { color: '#93977a', width: 3 },
          itemStyle: { color: '#93977a' },
          areaStyle: { color: 'rgba(147,151,122,0.18)' }
        },
        {
          name: '订单量', type: 'bar', yAxisIndex: 1,
          data: list.map(d => d.orderCount),
          itemStyle: { color: '#c6c9a4', borderRadius: [4, 4, 0, 0] }
        }
      ]
    }, true)
  } finally {
    trendLoading.value = false
  }
}

async function loadTop() {
  topLoading.value = true
  try {
    const res = await statsTopDishes({ days: topDays.value, limit: 10 })
    topData.value = (res.data || []).slice().reverse()
    topChart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 110, right: 40, top: 20, bottom: 30 },
      xAxis: { type: 'value', axisLabel: { color: AXIS.color } },
      yAxis: { type: 'category', data: topData.value.map(d => d.dishName), axisLabel: { color: '#5e5f4e' } },
      series: [{
        type: 'bar',
        data: topData.value.map(d => d.sold),
        itemStyle: { color: '#93977a', borderRadius: [0, 6, 6, 0] },
        label: { show: true, position: 'right', color: '#9b9d84', formatter: '{c} 份' }
      }]
    }, true)
  } finally {
    topLoading.value = false
  }
}

onMounted(async () => {
  trendChart = echarts.init(trendEl.value)
  topChart = echarts.init(topEl.value)
  await Promise.all([loadTrend(), loadTop()])
  window.addEventListener('resize', onResize)
})

function onResize() {
  trendChart?.resize()
  topChart?.resize()
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  trendChart?.dispose()
  topChart?.dispose()
})
</script>

<style scoped>
.page {
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.chart-lg { height: 320px; width: 100%; }
</style>
