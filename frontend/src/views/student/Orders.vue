<template>
  <div class="page">
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待处理" name="0" />
      <el-tab-pane label="已完成" name="1" />
      <el-tab-pane label="已取消" name="2" />
    </el-tabs>

    <div v-loading="loading" class="order-list">
      <el-card v-for="o in orders" :key="o.id" shadow="never" class="order-card">
        <div class="o-head">
          <div>
            <span class="o-no">{{ o.orderNo }}</span>
            <span class="o-stall">{{ o.stallName }}</span>
          </div>
          <el-tag :type="statusType(o.status)" round>{{ ORDER_STATUS[o.status] }}</el-tag>
        </div>

        <div class="o-items">
          <div v-for="it in o.items" :key="it.id" class="o-item">
            <span>{{ it.dishName }} × {{ it.quantity }}</span>
            <span>¥{{ fmt(it.subtotal) }}</span>
          </div>
        </div>

        <div class="o-foot">
          <div class="o-extra">
            <el-tag size="small" :type="o.deliveryType === 0 ? 'info' : 'primary'" round>
              {{ o.deliveryType === 0 ? '🍽 自取' : '🛵 配送' }}
            </el-tag>
            <span v-if="o.deliveryType === 0 && o.pickupCode" class="o-code">取餐码 <b>{{ o.pickupCode }}</b></span>
            <span v-if="o.deliveryType === 1 && o.deliveryStatus != null" class="o-code">
              配送：{{ DELIVERY_STATUS[o.deliveryStatus] || '-' }}<template v-if="o.riderName">（{{ o.riderName }}）</template>
            </span>
            <span class="o-time">{{ o.createTime }}</span>
          </div>
          <div class="o-ops">
            <span class="o-pay">实付 <b>¥{{ fmt(o.payAmount) }}</b></span>
            <el-button v-if="o.status === 0" size="small" type="danger" round plain @click="cancel(o)">取消订单</el-button>
            <el-button v-if="o.status === 1 && o.items?.length" size="small" round @click="openReview(o)">评价</el-button>
            <el-button size="small" round plain type="warning" @click="openComplaint(o)">投诉</el-button>
          </div>
        </div>
      </el-card>
      <el-empty v-if="!loading && orders.length === 0" description="这里还没有订单，去点一单吧～" />
    </div>

    <el-pagination
      v-model:current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next" background class="pager"
      @current-change="load"
    />

    <!-- 投诉弹窗 -->
    <el-dialog v-model="complaintVisible" title="📮 发起投诉" width="440px">
      <p class="cmp-tip">订单 <b>{{ complaintOrder?.orderNo }}</b> · {{ complaintOrder?.stallName }}
        <span style="color:#9b9d84">（投诉后由商家回应、管理员裁定，金可用进度请在「我的投诉」查看）</span></p>
      <el-form label-position="top">
        <el-form-item label="投诉类型" required>
          <el-radio-group v-model="complaintForm.type">
            <el-radio :value="1">菜品质量</el-radio>
            <el-radio :value="2">漏发缺件</el-radio>
            <el-radio :value="3">配送问题</el-radio>
            <el-radio :value="4">服务态度</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="问题描述（5-300字）" required>
          <el-input
            v-model="complaintForm.content"
            type="textarea" :rows="4" maxlength="300" show-word-limit
            placeholder="请客观描述遇到的问题，管理员会据此核实裁定"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintVisible = false">取消</el-button>
        <el-button type="primary" :loading="complaintSubmitting" :disabled="complaintForm.content.trim().length < 5" @click="submitComplaint">提交投诉</el-button>
      </template>
    </el-dialog>

    <!-- 评价弹窗（已完成订单逐菜品评分） -->
    <el-dialog v-model="reviewVisible" title="给这单打个分吧 ✨" width="460px">
      <div v-for="item in reviewOrder?.items || []" :key="item.dishId" class="rev-row">
        <div class="rev-name">{{ item.dishName }}</div>
        <el-rate v-model="reviewForm[item.dishId].score" :colors="['#c8b273', '#c8b273', '#c8b273']" />
        <el-input v-model="reviewForm[item.dishId].content" type="textarea" :rows="1"
          placeholder="想说点什么（选填）" @input="e => (reviewForm[item.dishId].content = e)" style="margin-top:6px" />
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">下次再说</el-button>
        <el-button type="primary" :loading="reviewing" :disabled="allScoresZero" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DELIVERY_STATUS,
  ORDER_STATUS,
  orderCancel, orderPage, reviewByOrder, reviewCreate
} from '../../api/order'
import { COMPLAINT_TYPE, complaintCreate } from '../../api/complaint'

const ORDER_STATUS_MAP = ORDER_STATUS

const fmt = (v) => Number(v ?? 0).toFixed(2)

const tab = ref('all')
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const res = await orderPage({
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

async function cancel(o) {
  await ElMessageBox.confirm(`确定取消订单 ${o.orderNo} 吗？已付金额将退回钱包。`, '取消订单', { type: 'warning' })
  await orderCancel(o.id)
  ElMessage.success('已取消，款项将原路退回钱包')
  load()
}

// ---------- 评价 ----------
const reviewVisible = ref(false)
const reviewing = ref(false)
const reviewOrder = ref(null)
const reviewForm = reactive({})

const allScoresZero = computed(() =>
  Object.values(reviewForm).every(r => !r.score)
)

async function openReview(o) {
  reviewOrder.value = o
  // 若已评价过，带出已有评分
  try {
    const exist = (await reviewByOrder(o.id)).data || []
    o.items.forEach(it => {
      reviewForm[it.dishId] = {
        score: exist.find(r => r.dishId === it.dishId)?.score || 5,
        content: exist.find(r => r.dishId === it.dishId)?.content || ''
      }
    })
  } catch {
    o.items?.forEach(it => { reviewForm[it.dishId] = { score: 5, content: '' } })
  }
  reviewVisible.value = true
}

async function submitReview() {
  reviewing.value = true
  try {
    for (const [dishId, r] of Object.entries(reviewForm)) {
      if (r.score) {
        await reviewCreate({ orderId: reviewOrder.value.id, dishId: Number(dishId), score: r.score, content: r.content || undefined })
      }
    }
    ElMessage.success('感谢评价！')
    reviewVisible.value = false
  } finally {
    reviewing.value = false
  }
}

// ---------- 投诉 ----------
const complaintVisible = ref(false)
const complaintSubmitting = ref(false)
const complaintOrder = ref(null)
const complaintForm = reactive({ type: 1, content: '' })

function openComplaint(o) {
  complaintOrder.value = o
  Object.assign(complaintForm, { type: 1, content: '' })
  complaintVisible.value = true
}

async function submitComplaint() {
  complaintSubmitting.value = true
  try {
    await complaintCreate({
      orderId: complaintOrder.value.id,
      type: complaintForm.type,
      content: complaintForm.content
    })
    ElMessage.success('投诉已提交！商家回应与管理员裁定将通过「消息中心」通知你')
    complaintVisible.value = false
  } finally {
    complaintSubmitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 1080px; margin: 0 auto; }
.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 180px;
}
.order-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.o-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px dashed #e6e4d4;
}
.o-no { font-size: 12.5px; color: #9b9d84; margin-right: 10px; }
.o-stall { font-weight: 600; color: #4f5040; }
.o-items { padding: 8px 0; }
.o-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #5e5f4e;
  padding: 3px 0;
}
.o-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.o-extra { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.o-code { font-size: 12.5px; color: #8a8b7a; }
.o-code b { color: #93977a; font-size: 15px; }
.o-time { font-size: 12px; color: #b3b5a0; }
.o-ops { display: flex; align-items: center; gap: 10px; }
.o-pay { color: #5e5f4e; font-size: 13px; }
.o-pay b { color: #b0714f; font-size: 16px; }
.rev-row { padding: 8px 0; }
.cmp-tip { font-size: 13px; color: #5e5f4e; margin: 0 0 10px; }
.rev-name { font-weight: 600; font-size: 14px; color: #4f5040; margin-bottom: 4px; }
.pager { margin-top: 16px; justify-content: center; }
</style>
