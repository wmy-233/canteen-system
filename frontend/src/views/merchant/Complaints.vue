<template>
  <div class="page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <span>📮 本店投诉处理</span>
          <el-radio-group v-model="status" size="small" @change="load">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">待回应</el-radio-button>
            <el-radio-button :value="1">已回应</el-radio-button>
            <el-radio-button :value="2">已裁定</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div v-loading="loading" class="c-list">
        <el-card v-for="c in list" :key="c.id" shadow="never" class="c-card">
          <div class="c-head">
            <div>
              <span class="c-name">学生「{{ c.student_name || '匿名' }}」 · 订单 {{ c.order_no }}</span>
              <span class="c-type">{{ COMPLAINT_TYPE[c.type] }}</span>
            </div>
            <el-tag :type="statusType(c.status)" round size="small">{{ COMPLAINT_STATUS[c.status] }}</el-tag>
          </div>
          <div class="c-content">{{ c.content }}</div>
          <div v-if="c.reply" class="c-reply"><b>我的回应：</b>{{ c.reply }}　<i>{{ (c.reply_time || '').slice(0, 16).replace('T', ' ') }}</i></div>
          <div v-if="c.status === 2" class="c-verdict">
            <b>⚖️ 管理员裁定：{{ c.result === 1 ? '成立' : '不成立' }}</b>
            （{{ c.verdict || '—' }}）
            <template v-if="Number(c.refund_amount) > 0">· 顾客已退款 ¥{{ fmt(c.refund_amount) }}</template>
          </div>
          <div class="c-foot">
            <el-tag v-if="c.status === 0" type="warning" size="small" effect="plain" round>⏰ 请尽快回应说明情况（可先与顾客沟通）</el-tag>
            <el-button v-if="c.status === 0" type="primary" size="small" round @click="openReply(c)">提交回应</el-button>
          </div>
        </el-card>
        <el-empty v-if="!loading && list.length === 0" description="本店没有相关投诉，继续保持～" />
      </div>
    </el-card>

    <!-- 回应弹窗 -->
    <el-dialog v-model="replyVisible" title="回应说明" width="440px">
      <p class="r-tip">回应会提交给学生查看，并进入管理员裁定流程（回应后状态变为「已回应」，等待裁定）</p>
      <el-input
        v-model="replyForm.content"
        type="textarea" :rows="4" maxlength="300" show-word-limit
        placeholder="如：已与顾客沟通致歉、情况说明、整改措施……"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" :disabled="replyForm.content.trim().length < 5" @click="submitReply">提交回应</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { COMPLAINT_STATUS, COMPLAINT_TYPE, complaintReply, complaintStallList } from '../../api/complaint'

const fmt = (v) => Number(v ?? 0).toFixed(2)
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'info' }[s] || 'info')

const status = ref(undefined)
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await complaintStallList({ status: status.value })
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

// 回应
const replyVisible = ref(false)
const replying = ref(false)
const target = ref(null)
const replyForm = ref({ content: '' })

function openReply(c) {
  target.value = c
  replyForm.value = { content: '' }
  replyVisible.value = true
}

async function submitReply() {
  replying.value = true
  try {
    await complaintReply(target.value.id, { content: replyForm.value.content })
    ElMessage.success('回应已提交，学生已收到站内消息')
    replyVisible.value = false
    load()
  } finally {
    replying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 950px; margin: 0 auto; }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(169, 132, 103, 0.14);
}
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.c-list { display: flex; flex-direction: column; gap: 12px; min-height: 200px; }
.c-card { border-radius: 12px; border: none; box-shadow: 0 3px 12px rgba(169, 132, 103, 0.1); }
.c-head { display: flex; justify-content: space-between; align-items: center; }
.c-name { font-weight: 600; color: #5e5f4e; }
.c-type {
  margin-left: 10px;
  font-size: 12px;
  color: #a98467;
  background: #faf4ee;
  padding: 2px 8px;
  border-radius: 999px;
}
.c-content { font-size: 13px; color: #4f5040; margin-top: 8px; line-height: 1.6; }
.c-reply {
  margin-top: 8px;
  font-size: 12.5px;
  color: #6f7060;
  background: #f6f4ec;
  border-radius: 8px;
  padding: 8px 10px;
  line-height: 1.6;
}
.c-verdict {
  margin-top: 8px;
  font-size: 12.5px;
  color: #6f7060;
  background: #faf6f0;
  border-radius: 8px;
  padding: 8px 10px;
  line-height: 1.6;
}
.c-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.r-tip { font-size: 12.5px; color: #9b9d84; margin: 0 0 10px; }
</style>
