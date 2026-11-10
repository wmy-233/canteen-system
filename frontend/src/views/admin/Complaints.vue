<template>
  <div class="page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <span>⚖️ 投诉管理（裁定中心）</span>
          <el-radio-group v-model="status" size="small" @change="load">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">待处理</el-radio-button>
            <el-radio-button :value="1">已回应</el-radio-button>
            <el-radio-button :value="2">已裁定</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div v-loading="loading" class="c-list">
        <el-card v-for="c in list" :key="c.id" shadow="never" class="c-card">
          <div class="c-head">
            <div>
              <span class="c-stall">🏪 {{ c.stall_name }}</span>
              <span class="c-student">学生「{{ c.student_name || '匿名' }}」· 订单 {{ c.order_no }}</span>
            </div>
            <div class="c-tags">
              <el-tag size="small" effect="plain" round type="info">{{ COMPLAINT_TYPE[c.type] }}</el-tag>
              <el-tag :type="statusType(c.status)" size="small" round>{{ COMPLAINT_STATUS[c.status] }}</el-tag>
            </div>
          </div>
          <div class="c-content">{{ c.content }}</div>
          <div v-if="c.reply" class="c-reply"><b>🏪 商家回应：</b>{{ c.reply }}　<i>{{ (c.reply_time || '').slice(0, 16).replace('T', ' ') }}</i></div>
          <div v-if="c.status === 2" class="c-verdict">
            <b>⚖️ 裁定：{{ c.result === 1 ? '成立' : '不成立' }}</b>（{{ c.verdict || '—' }}）
            <template v-if="Number(c.refund_amount) > 0">· 已退款 ¥{{ fmt(c.refund_amount) }}</template>
          </div>
          <div class="c-foot">
            <el-tag v-if="c.status === 0" type="info" size="small" effect="plain" round>商家尚未回应，可直接裁定</el-tag>
            <el-button v-if="c.status !== 2" type="primary" size="small" round @click="openResolve(c)">裁定</el-button>
          </div>
        </el-card>
        <el-empty v-if="!loading && list.length === 0" description="没有待处理的投诉～" />
      </div>
    </el-card>

    <!-- 裁定弹窗 -->
    <el-dialog v-model="resolveVisible" title="⚖️ 投诉裁定" width="470px">
      <div v-if="target">
        <p class="r-tip">
          投诉：{{ COMPLAINT_TYPE[target.type] }} · 订单 {{ target.order_no }}（实付 ¥{{ fmt(orderPay) }}）
          <template v-if="target.reply"><br/>商家回应：{{ target.reply.slice(0, 120) }}</template>
        </p>
        <el-radio-group v-model="resolveForm.pass">
          <el-radio :value="true">✅ 投诉成立（支持学生）</el-radio>
          <el-radio :value="false">❌ 投诉不成立（结案）</el-radio>
        </el-radio-group>
        <el-alert v-if="resolveForm.pass" type="success" :closable="false" style="margin-top:10px"
          title="成立：可选择将订单实付金额全额退款给学生（钱包原路退回+退款流水）" />
        <el-alert v-else type="warning" :closable="false" style="margin-top:10px"
          title="不成立：填写裁定理由结案，学生与商家均会收到站内消息" />
        <el-checkbox v-if="resolveForm.pass" v-model="resolveForm.refund" style="margin-top:8px">
          同时全额退款（¥{{ fmt(orderPay) }}）
        </el-checkbox>
        <el-input
          v-model="resolveForm.content"
          type="textarea" :rows="3" maxlength="300" show-word-limit style="margin-top:8px"
          placeholder="裁定说明（会通知学生与商家）"
        />
      </div>
      <template #footer>
        <el-button @click="resolveVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolving" @click="submitResolve">提交裁定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { COMPLAINT_STATUS, COMPLAINT_TYPE, complaintAdminList, complaintResolve } from '../../api/complaint'

const fmt = (v) => Number(v ?? 0).toFixed(2)
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'info' }[s] || 'info')

const status = ref(undefined)
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await complaintAdminList({ status: status.value })
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

// 裁定
const resolveVisible = ref(false)
const resolving = ref(false)
const target = ref(null)
const resolveForm = reactive({ pass: true, content: '', refund: true })
const orderPay = computed(() => {
  const c = list.value.find(x => x.id === target.value?.id)
  return c?.pay_amount ?? target.value?.pay_amount ?? 0
})

function openResolve(c) {
  target.value = c
  Object.assign(resolveForm, { pass: true, content: '', refund: true })
  resolveVisible.value = true
}

async function submitResolve() {
  resolving.value = true
  try {
    await complaintResolve(target.value.id, {
      pass: resolveForm.pass,
      content: resolveForm.content || undefined,
      refund: resolveForm.pass && resolveForm.refund
    })
    ElMessage.success('裁定已生效，学生与商家均会收到站内消息')
    resolveVisible.value = false
    load()
  } finally {
    resolving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(126, 148, 168, 0.14);
}
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.c-list { display: flex; flex-direction: column; gap: 12px; min-height: 200px; }
.c-card { border-radius: 12px; border: none; box-shadow: 0 3px 12px rgba(126, 148, 168, 0.1); }
.c-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 6px; }
.c-stall { font-weight: 700; color: #4f5040; }
.c-student { font-size: 12.5px; color: #9b9d84; margin-left: 10px; }
.c-tags { display: flex; gap: 6px; }
.c-content { font-size: 13px; color: #4f5040; margin-top: 8px; line-height: 1.6; }
.c-reply {
  margin-top: 8px;
  font-size: 12.5px;
  color: #6f7060;
  background: #f2f4f6;
  border-radius: 8px;
  padding: 8px 10px;
  line-height: 1.6;
}
.c-verdict {
  margin-top: 8px;
  font-size: 12.5px;
  color: #6f7060;
  background: #eef3f6;
  border-radius: 8px;
  padding: 8px 10px;
  line-height: 1.6;
}
.c-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.r-tip { font-size: 12.5px; color: #8a8b7a; margin: 0 0 10px; line-height: 1.7; }
</style>
