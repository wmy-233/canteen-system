<template>
  <div class="page">
    <!-- 余额卡 -->
    <div class="balance-card">
      <div>
        <div class="bc-label">校园卡余额</div>
        <div class="bc-value">¥{{ fmt(balance) }}</div>
      </div>
      <el-button type="primary" size="large" round class="bc-btn" @click="openRecharge">
        <span style="letter-spacing: 2px">充 值</span>
      </el-button>
    </div>

    <!-- 流水 -->
    <el-card shadow="never" class="panel">
      <template #header>收支明细</template>
      <el-table :data="records" stripe v-loading="loading">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeStyle(row.type)" size="small" round>{{ typeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === 1 ? 'in' : 'out'">{{ row.type === 1 ? '+' : '-' }}¥{{ fmt(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="refOrderNo" label="关联订单" min-width="160" />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next" background class="pager"
        @current-change="load"
      />
    </el-card>

    <!-- 充值弹窗 -->
    <el-dialog v-model="rechargeVisible" title="校园卡充值 💰" width="400px">
      <div class="quick-amounts">
        <button
          v-for="a in [10, 20, 50, 100, 200]"
          :key="a" type="button"
          class="qa-btn" :class="{ active: amount === a }"
          @click="amount = a"
        >¥{{ a }}</button>
      </div>
      <el-input-number
        v-model="amount"
        :min="0.01" :max="100000" :precision="2" :step="10"
        style="width: 100%; margin-top: 12px" size="large" controls-position="right"
      />
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" @click="rechargeConfirm = true">下一步</el-button>
      </template>
    </el-dialog>

    <!-- 确认充值 -->
    <el-dialog v-model="rechargeConfirm" title="确认充值" width="320px">
      <p style="text-align: center; font-size: 18px">
        确定往校园卡充值
        <b style="color: #b0714f; font-size: 22px">¥{{ fmt(amount) }}</b>
        吗？
      </p>
      <template #footer>
        <el-button @click="rechargeConfirm = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="doRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { walletInfo, walletRecharge } from '../../api/wallet'

const fmt = (v) => Number(v ?? 0).toFixed(2)

const balance = ref(0)
const records = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)

const rechargeVisible = ref(false)
const rechargeConfirm = ref(false)
const recharging = ref(false)
const amount = ref(50)

const typeName = (t) => ({ 1: '充值', 2: '消费', 3: '退款' }[t] || '其他')
const typeStyle = (t) => ({ 1: 'success', 2: 'warning', 3: 'info' }[t] || 'info')

async function load() {
  loading.value = true
  try {
    const res = await walletInfo({ pageNum: pageNum.value, pageSize })
    balance.value = res.data?.balance ?? 0
    records.value = res.data?.records?.list || []
    total.value = res.data?.records?.total || 0
  } finally {
    loading.value = false
  }
}

function openRecharge() {
  amount.value = 50
  rechargeVisible.value = true
}

async function doRecharge() {
  recharging.value = true
  try {
    await walletRecharge({ amount: amount.value })
    ElMessage.success('充值成功！')
    rechargeConfirm.value = false
    rechargeVisible.value = false
    await load()
  } finally {
    recharging.value = false
  }
}

watch(rechargeVisible, v => { if (v) amount.value = 50 })
onMounted(load)
</script>

<style scoped>
.page { max-width: 1080px; margin: 0 auto; }
.balance-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26px 30px;
  margin-bottom: 18px;
  border-radius: 16px;
  background: linear-gradient(120deg, #93977a 0%, #a4ab8d 60%, #bcc0a3 100%);
  box-shadow: 0 10px 24px rgba(147, 151, 122, 0.4);
  color: #fff;
}
.bc-label { font-size: 13px; opacity: 0.85; }
.bc-value { font-size: 34px; font-weight: 800; margin-top: 4px; letter-spacing: 2px; }
.bc-btn { background: #fffdf5; border: none; color: #6f7458; }
.bc-btn:hover { background: #fff; color: #575c44; }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.pager { margin-top: 14px; justify-content: center; }
.in { color: #74945a; font-weight: 700; }
.out { color: #b0714f; font-weight: 700; }
.quick-amounts { display: flex; gap: 10px; }
.qa-btn {
  flex: 1;
  padding: 10px 0;
  border: 1.5px solid #dcdcc6;
  background: #fbf9f1;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  color: #5e5f4e;
  transition: all 0.2s;
}
.qa-btn.active {
  border-color: #93977a;
  background: #93977a;
  color: #fff;
}
</style>
