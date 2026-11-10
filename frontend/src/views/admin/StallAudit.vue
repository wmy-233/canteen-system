<template>
  <div class="page">
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="待审核" name="2" />
      <el-tab-pane label="已审核（营业/停业）" name="all" />
    </el-tabs>

    <div v-loading="loading" class="stall-list">
      <el-card v-for="s in stalls" :key="s.id" shadow="never" class="stall-card">
        <div class="s-head">
          <div>
            <div class="s-name">{{ s.name }}</div>
            <div class="s-meta">{{ s.canteenName }} · 商家 {{ s.merchantUsername }}</div>
            <div class="s-meta">{{ s.createTime }}</div>
          </div>
          <el-tag :type="stallStyle(s.status)" round>{{ stallName(s.status) }}</el-tag>
        </div>
        <div v-if="s.status === 2" class="s-ops">
          <el-button type="success" round @click="audit(s, true)">✓ 通过入驻（转营业）</el-button>
          <el-button type="danger" round plain @click="audit(s, false)">✕ 驳回</el-button>
        </div>
      </el-card>
      <el-empty v-if="!loading && stalls.length === 0" description="暂无相关档口" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stallAudit, stallList } from '../../api/admin'

const tab = ref('2')
const stalls = ref([])
const loading = ref(false)

const stallName = (s) => ({ 0: '停业', 1: '营业中', 2: '待审核' }[s] || '-')
const stallStyle = (s) => ({ 0: 'info', 1: 'success', 2: 'warning' }[s] || 'info')

async function load() {
  loading.value = true
  try {
    const res = await stallList(tab.value === 'all' ? {} : { status: Number(tab.value) })
    // 管理端 tab=全部时也会包含待审核，这里把两种数据都放到一个列表由 tag 区分
    stalls.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function audit(s, pass) {
  await ElMessageBox.confirm(
    pass ? `通过「${s.name}」的入驻申请，档口将转为营业状态。` : `驳回「${s.name}」的入驻申请。`,
    '档口审核', { type: pass ? 'success' : 'warning' }
  )
  await stallAudit(s.id, { pass })
  ElMessage.success(pass ? '已通过，档口开始营业' : '已驳回')
  load()
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 1080px; margin: 0 auto; }
.stall-list { display: flex; flex-direction: column; gap: 12px; min-height: 200px; }
.stall-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.s-head { display: flex; justify-content: space-between; align-items: flex-start; }
.s-name { font-weight: 700; font-size: 16px; color: #4f5040; }
.s-meta { font-size: 12.5px; color: #9b9d84; margin-top: 3px; }
.s-ops { margin-top: 14px; display: flex; gap: 10px; }
</style>
