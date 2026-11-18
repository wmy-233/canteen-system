<template>
  <div class="rider-page">
    <div class="rd-title">🛵 我的跑腿单</div>
    <el-card shadow="never" class="rider-card">
      <el-empty v-if="!loading && list.length === 0" description="还没有接到跑腿单，去跑腿大厅看看～" />
      <div v-for="o in list" :key="o.id" class="order-row">
        <div class="or-left">
          <div class="or-top">
            <span class="or-no">{{ o.orderNo }}</span>
            <span class="or-stall">🏪 {{ o.stallName }}</span>
            <el-tag :type="o.deliveryStatus === 2 ? 'success' : 'warning'" size="small" round>
              {{ STATUS[o.deliveryStatus] }}
            </el-tag>
          </div>
          <div class="or-meta">实付 <b>¥{{ fmt(o.payAmount) }}</b> · 跑腿费 <b>¥{{ fmt(o.deliveryFee) }}</b>{{ o.deliveryStatus === 2 ? '（已结算到钱包）' : '（送达后结算）' }} · 📍 {{ o.deliveryLocation || '校内指定地点' }} · {{ o.createTime }}</div>
        </div>
        <el-button v-if="o.deliveryStatus === 1" type="success" round size="small" @click="finish(o)">
          标记送达
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { riderDeliver, riderMine } from '../../api/rider'

const STATUS = { 0: '待接单', 1: '配送中', 2: '已送达' }
const list = ref([])
const loading = ref(false)
const fmt = v => Number(v ?? 0).toFixed(2)

async function load() {
  loading.value = true
  try {
    const res = await riderMine()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function finish(o) {
  try {
    await ElMessageBox.confirm(`确认订单 ${o.orderNo} 已送达？`, '完成配送', { type: 'info' })
  } catch { return }
  await riderDeliver(o.id)
  ElMessage.success('配送完成，学生已收到通知 🎉')
  load()
}

onMounted(load)
</script>

<style scoped>
.rider-page { max-width: 900px; margin: 0 auto; }
.rd-title { font-size: 20px; font-weight: 700; margin-bottom: 16px; }
.rider-card { border-radius: 14px; }
.order-row {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 14px 6px; border-bottom: 1px solid #eee;
}
.order-row:last-child { border-bottom: none; }
.or-top { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.or-no { font-weight: 700; color: #4a4c3a; }
.or-stall { font-size: 13px; color: #7d8168; }
.or-meta { font-size: 13px; color: #7d8168; margin-top: 6px; }
.or-meta b { color: #c06b4a; }
</style>
