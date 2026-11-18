<template>
  <div class="logs-page">
    <div class="lp-head">
      <div>
        <div class="lp-title">📜 操作日志</div>
        <div class="lp-sub">AOP 切面自动记录关键操作，落库留痕、可追溯（最近 300 条）</div>
      </div>
      <div class="lp-tools">
        <el-input v-model="keyword" placeholder="操作人 / 操作 / 参数" size="small" clearable style="width:200px" @keyup.enter="load" />
        <el-select v-model="module" placeholder="全部模块" size="small" clearable style="width:130px" @change="load">
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select>
        <el-button size="small" @click="load">查询</el-button>
        <el-button size="small" type="danger" plain @click="clearLogs">清空</el-button>
      </div>
    </div>

    <el-table :data="logs" v-loading="loading" size="small" border stripe class="lp-table">
      <el-table-column prop="createTime" label="时间" width="165" />
      <el-table-column prop="operator" label="操作人" width="105" />
      <el-table-column prop="module" label="模块" width="80" />
      <el-table-column prop="action" label="操作" width="110" />
      <el-table-column prop="method" label="方法" min-width="180" show-overflow-tooltip />
      <el-table-column prop="params" label="参数" min-width="200" show-overflow-tooltip />
      <el-table-column prop="costMs" label="耗时" width="75">
        <template #default="{ row }">{{ row.costMs }}ms</template>
      </el-table-column>
      <el-table-column label="结果" width="70">
        <template #default="{ row }">
          <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { operationLogs, operationLogsClear } from '../../api/admin'

const logs = ref([])
const keyword = ref('')
const module = ref('')
const loading = ref(false)
const modules = ['用户', '订单', '骑手', '优惠', '档口', '钱包', '评价', '投诉']

async function load() {
  loading.value = true
  try {
    const res = await operationLogs({ keyword: keyword.value || undefined, module: module.value || undefined })
    logs.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function clearLogs() {
  try {
    await ElMessageBox.confirm('确定清空全部操作日志吗？', '提示', { type: 'warning' })
  } catch { return }
  await operationLogsClear()
  ElMessage.success('已清空')
  load()
}

onMounted(load)
</script>

<style scoped>
.logs-page { max-width: 1400px; margin: 0 auto; }
.lp-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.lp-title { font-size: 20px; font-weight: 700; }
.lp-sub { font-size: 12px; color: #9b9d84; margin-top: 4px; }
.lp-tools { display: flex; gap: 8px; }
.lp-table { border-radius: 12px; overflow: hidden; }
</style>
