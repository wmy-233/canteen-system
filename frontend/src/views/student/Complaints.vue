<template>
  <div class="page">
    <el-card shadow="never" class="panel">
      <template #header>📮 我的投诉</template>
      <div v-loading="loading" class="c-list">
        <div v-for="c in list" :key="c.id" class="c-item">
          <div class="c-head">
            <div>
              <span class="c-stall">{{ c.stall_name }}</span>
              <span class="c-order">订单 {{ c.order_no }}</span>
            </div>
            <div class="c-tags">
              <el-tag size="small" round effect="plain" type="info">{{ COMPLAINT_TYPE[c.type] }}</el-tag>
              <el-tag size="small" round :type="statusType(c.status)">{{ COMPLAINT_STATUS[c.status] }}</el-tag>
            </div>
          </div>
          <div class="c-content"> {{ c.content }}</div>
          <template v-if="c.status !== 0">
            <div class="c-reply"><b>🏪 商家回应：</b>{{ c.reply || '（暂无）' }}</div>
          </template>
          <template v-if="c.status === 2">
            <div class="c-verdict" :class="c.result === 1 ? 'win' : 'lose'">
              <b>{{ c.result === 1 ? '⚖️ 投诉成立' : '⚖️ 投诉未成立' }}</b>
              ：{{ c.verdict }}
              <template v-if="c.result === 1 && Number(c.refund_amount) > 0">
                · 退款 ¥{{ fmt(c.refund_amount) }} 已退回校园卡
              </template>
            </div>
          </template>
          <div class="c-time">{{ (c.create_time || '').slice(0, 19).replace('T', ' ') }}</div>
        </div>
        <el-empty v-if="!loading && list.length === 0"
          description="还没有投诉记录。对订单不满意时，可在「我的订单」里点「投诉」发起" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { COMPLAINT_STATUS, COMPLAINT_TYPE, complaintMine } from '../../api/complaint'

const fmt = (v) => Number(v ?? 0).toFixed(2)
const list = ref([])
const loading = ref(false)
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const res = await complaintMine()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 900px; margin: 0 auto; }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.c-list { display: flex; flex-direction: column; min-height: 200px; }
.c-item { padding: 14px 8px; border-bottom: 1px dashed #eceada; }
.c-item:last-child { border-bottom: none; }
.c-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 6px; }
.c-stall { font-weight: 700; color: #4f5040; }
.c-order { font-size: 12px; color: #9b9d84; margin-left: 10px; }
.c-tags { display: flex; gap: 6px; }
.c-content { font-size: 13px; color: #5e5f4e; margin-top: 8px; line-height: 1.6; }
.c-reply {
  margin-top: 8px;
  font-size: 13px;
  color: #6f7060;
  background: #f6f4ec;
  border-radius: 8px;
  padding: 8px 10px;
  line-height: 1.6;
}
.c-verdict {
  margin-top: 8px;
  font-size: 13px;
  border-radius: 8px;
  padding: 8px 10px;
  line-height: 1.6;
}
.c-verdict.win { background: rgba(116, 148, 90, 0.12); color: #5f7d48; }
.c-verdict.lose { background: rgba(176, 113, 79, 0.1); color: #a5674a; }
.c-time { font-size: 11.5px; color: #b3b5a0; margin-top: 6px; }
</style>
