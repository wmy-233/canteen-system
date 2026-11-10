<template>
  <div class="page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <div class="head-left">
            <span>🔔 消息中心</span>
            <el-radio-group v-model="tab" size="small" @change="load">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="unread">未读{{ unread ? `(${unread})` : '' }}</el-radio-button>
            </el-radio-group>
          </div>
          <el-button size="small" round :disabled="unread === 0" @click="readAll">全部标记已读</el-button>
        </div>
      </template>

      <div v-loading="loading" class="msg-list">
        <div
          v-for="m in list" :key="m.id"
          class="msg-item" :class="{ unread: m.isRead === 0 }"
          @click="readOne(m)"
        >
          <span class="mi-icon">{{ MESSAGE_ICON[m.type] || '📢' }}</span>
          <div class="mi-body">
            <div class="mi-title">
              {{ m.title }}
              <el-tag v-if="m.isRead === 0" size="small" type="danger" effect="plain" round class="mi-dot">未读</el-tag>
            </div>
            <div class="mi-content">{{ m.content }}</div>
            <div class="mi-time">{{ (m.createTime || '').slice(0, 19).replace('T', ' ') }}</div>
          </div>
          <el-tag size="small" effect="plain" round>{{ MESSAGE_TYPE[m.type] || '系统' }}</el-tag>
        </div>
        <el-empty v-if="!loading && list.length === 0" description="暂时没有消息～" />
      </div>

      <el-pagination
        v-model:current-page="pageNum" :page-size="pageSize" :total="total"
        layout="prev, pager, next" background class="pager" @current-change="load"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MESSAGE_ICON, MESSAGE_TYPE, messagePage, messageRead, messageReadAll, messageUnreadCount } from '../../api/message'

const tab = ref('all')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const unread = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await messagePage({
      isRead: tab.value === 'unread' ? 0 : undefined,
      pageNum: pageNum.value,
      pageSize
    })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
    const u = await messageUnreadCount()
    unread.value = u.data ?? 0
  } finally {
    loading.value = false
  }
}

async function readOne(m) {
  if (m.isRead === 0) {
    await messageRead(m.id)
    m.isRead = 1
    unread.value = Math.max(0, unread.value - 1)
  }
}

async function readAll() {
  await messageReadAll()
  ElMessage.success('已全部标记为已读')
  load()
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
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.head-left { display: flex; align-items: center; gap: 14px; }
.msg-list { display: flex; flex-direction: column; min-height: 220px; }
.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 10px;
  border-bottom: 1px dashed #eceada;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.msg-item:hover { background: #f7f6ea; }
.msg-item.unread { background: rgba(147, 151, 122, 0.08); }
.mi-icon { font-size: 20px; flex: none; margin-top: 2px; }
.mi-body { flex: 1; min-width: 0; }
.mi-title { font-weight: 600; font-size: 14px; color: #4f5040; display: flex; align-items: center; gap: 8px; }
.mi-dot { flex: none; }
.mi-content { font-size: 13px; color: #6f7060; margin-top: 3px; line-height: 1.55; white-space: pre-wrap; }
.mi-time { font-size: 11.5px; color: #b3b5a0; margin-top: 4px; }
.pager { margin-top: 14px; justify-content: center; }
</style>
