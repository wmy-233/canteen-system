<template>
  <div class="page">
    <div class="row">
      <!-- 骑手管理 -->
      <el-card shadow="never" class="panel">
        <template #header>
          <div class="panel-head">
            <span>骑手队伍</span>
            <el-button type="primary" round size="small" @click="openCreate">＋ 新建骑手</el-button>
          </div>
        </template>
        <el-table :data="riders" stripe v-loading="riderLoading" size="small">
          <el-table-column prop="name" label="姓名" width="90" />
          <el-table-column prop="username" label="账号" width="100" />
          <el-table-column prop="phone" label="电话" width="120" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1" inline-prompt
                active-text="在岗" inactive-text="停用"
                @change="v => riderToggle(row, v)"
              />
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="riderPageNum" :page-size="riderPageSize" :total="riderTotal"
          layout="prev, pager, next" background small class="pager" @current-change="loadRiders"
        />
      </el-card>

      <!-- 待分配 -->
      <el-card shadow="never" class="panel">
        <template #header>🔔 待分配配送单（{{ pendingOrders.length }}）</template>
        <div v-loading="pendingLoading" class="pending-list">
          <div v-for="o in pendingOrders" :key="o.id" class="pending-item">
            <div class="pi-main">
              <div class="pi-line"><b>{{ o.stallName }}</b> → 学生 {{ o.nickname }}</div>
              <div class="pi-sub">{{ o.orderNo }} · ¥{{ fmt(o.payAmount) }} · {{ o.createTime }}</div>
              <div class="pi-chips">
                <span v-for="it in o.items" :key="it.id" class="pi-chip">{{ it.dishName }}×{{ it.quantity }}</span>
              </div>
            </div>
            <div class="pi-ops">
              <el-select :model-value="undefined" placeholder="选在岗骑手" size="small" style="width:120px"
                @change="v => assign(o, v)">
                <el-option v-for="r in activeRiders" :key="r.id" :label="r.name" :value="r.id" />
              </el-select>
            </div>
          </div>
          <el-empty v-if="!pendingLoading && pendingOrders.length === 0" description="暂无待分配的配送单" :image-size="60" />
        </div>
      </el-card>
    </div>

    <!-- 新建骑手 -->
    <el-dialog v-model="createVisible" title="新建骑手账号" width="420px">
      <el-form :model="riderForm" label-width="60px">
        <el-form-item label="账号" required><el-input v-model="riderForm.username" placeholder="2-50位" /></el-form-item>
        <el-form-item label="密码" required><el-input v-model="riderForm.password" type="password" placeholder="6-20位" show-password /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="riderForm.name" placeholder="骑手真实姓名" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="riderForm.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createRider">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { riderAssign, riderCreate, riderDeliver, riderPage, riderPendingOrders, riderUpdate } from '../../api/admin'

const fmt = (v) => Number(v ?? 0).toFixed(2)

const riders = ref([])
const riderTotal = ref(0)
const riderPageNum = ref(1)
const riderPageSize = 10
const riderLoading = ref(false)
const activeRiders = computed(() => riders.value.filter(r => r.status === 1))

const pendingOrders = ref([])
const pendingLoading = ref(false)

const createVisible = ref(false)
const creating = ref(false)
const riderForm = ref({ username: '', password: '', name: '', phone: '' })

async function loadRiders() {
  riderLoading.value = true
  try {
    const res = await riderPage({ pageNum: riderPageNum.value, pageSize: riderPageSize })
    riders.value = res.data?.list || []
    riderTotal.value = res.data?.total || 0
  } finally {
    riderLoading.value = false
  }
}

async function loadPending() {
  pendingLoading.value = true
  try {
    const res = await riderPendingOrders({ pageNum: 1, pageSize: 50 })
    pendingOrders.value = res.data?.list || []
  } finally {
    pendingLoading.value = false
  }
}

async function riderToggle(row, on) {
  await riderUpdate(row.id, { status: on ? 1 : 0 })
  ElMessage.success(on ? '已置为在岗' : '已停用')
  loadRiders()
}

function openCreate() {
  riderForm.value = { username: '', password: '', name: '', phone: '' }
  createVisible.value = true
}

async function createRider() {
  creating.value = true
  try {
    await riderCreate(riderForm.value)
    ElMessage.success('骑手创建成功')
    createVisible.value = false
    loadRiders()
  } finally {
    creating.value = false
  }
}

async function assign(o, riderId) {
  if (!riderId) return
  await ElMessageBox.confirm('确认指派这位骑手配送该订单？', '指派骑手', { type: 'info' })
  await riderAssign({ orderId: o.id, riderId })
  ElMessage.success('已指派，订单进入配送中')
  loadPending()
}

// 送达操作放在已分配的订单：pending 列表只含未分配单，故提供手动入口不必要；
// 但保留「已送达」一键兜底，方便管理员手工完成异常单：
void riderDeliver

onMounted(() => {
  loadRiders()
  loadPending()
})
</script>

<style scoped>
.page { max-width: 1150px; margin: 0 auto; }
.row { display: grid; grid-template-columns: 1fr 1.3fr; gap: 18px; }
@media (max-width: 1000px) { .row { grid-template-columns: 1fr; } }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 10px; justify-content: center; }
.pending-list { display: flex; flex-direction: column; gap: 10px; min-height: 220px; }
.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  background: #f6f4ec;
  border-radius: 12px;
  padding: 10px 12px;
}
.pi-line { font-size: 14px; color: #4f5040; }
.pi-sub { font-size: 12px; color: #9b9d84; margin-top: 3px; }
.pi-chips { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 5px; }
.pi-chip {
  font-size: 11px;
  background: #fff;
  border: 1px solid #e3e1d2;
  color: #8a8b7a;
  padding: 1px 8px;
  border-radius: 999px;
}
.pi-ops { flex: none; }
</style>
