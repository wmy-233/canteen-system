<template>
  <div class="page">
    <div class="row">
      <!-- 发券 -->
      <el-card shadow="never" class="panel">
        <template #header>🎫 新建发放优惠券</template>
        <el-form :model="grantForm" label-width="86px">
          <el-form-item label="券名" required>
            <el-input v-model="grantForm.name" maxlength="30" placeholder="如：满20减5开学季券" />
          </el-form-item>
          <el-form-item label="使用门槛" required>
            <el-input-number v-model="grantForm.threshold" :min="0" :precision="2" style="width:100%" />
            <div class="tip">0 表示无门槛</div>
          </el-form-item>
          <el-form-item label="面额(元)" required>
            <el-input-number v-model="grantForm.amount" :min="0.01" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="库存(张)" required>
            <el-input-number v-model="grantForm.stock" :min="1" :max="100000" style="width:100%" />
          </el-form-item>
          <el-form-item label="到 期 时 间" required>
            <el-date-picker
              v-model="grantForm.expireTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择到期时间"
              style="width:100%"
            />
          </el-form-item>
          <el-button type="primary" style="width:100%" size="large" round :loading="granting" @click="grant">
            发布到「限时领券」
          </el-button>
        </el-form>
      </el-card>

      <!-- 已有券列表 -->
      <el-card shadow="never" class="panel">
        <template #header>已发布的券（{{ coupons.length }}）</template>
        <el-table :data="coupons" stripe v-loading="loading" size="small">
          <el-table-column prop="name" label="券名" min-width="130" />
          <el-table-column label="面额" width="80">
            <template #default="{ row }">¥{{ fmt(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="满减" width="90">
            <template #default="{ row }">满¥{{ fmt(row.threshold) }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="剩余库存" width="90" />
          <el-table-column label="到期" width="150">
            <template #default="{ row }">{{ (row.expireTime || '').slice(0, 16) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { couponGrant, couponList } from '../../api/admin'

const fmt = (v) => Number(v ?? 0).toFixed(2)

const coupons = ref([])
const loading = ref(false)
const granting = ref(false)
const grantForm = ref({
  name: '',
  threshold: 20,
  amount: 5,
  stock: 100,
  expireTime: undefined
})

async function load() {
  loading.value = true
  try {
    const res = await couponList()
    coupons.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function grant() {
  granting.value = true
  try {
    await couponGrant(grantForm.value)
    ElMessage.success('已发布，可在学生端「限时领券」看到')
    grantForm.value = { name: '', threshold: 20, amount: 5, stock: 100, expireTime: undefined }
    await load()
  } finally {
    granting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 1150px; margin: 0 auto; }
.row { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
@media (max-width: 1000px) { .row { grid-template-columns: 1fr; } }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.tip { font-size: 11.5px; color: #9b9d84; }
</style>
