<template>
  <div class="page">
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="待处理（待出餐）" name="0" />
      <el-tab-pane label="已完成" name="1" />
      <el-tab-pane label="已取消" name="2" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

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
            type="primary" size="small" round @click="verify(o)"
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

async function verify(o) {
  await ElMessageBox.confirm(`确认核销订单 ${o.orderNo}？取餐码应为 ${o.pickupCode}。`, '出餐核销', { type: 'warning' })
  await orderVerify(o.id)
  ElMessage.success('核销完成，该单转已完成')
  load()
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 1080px; margin: 0 auto; }
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
