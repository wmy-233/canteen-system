<template>
  <div class="page">
    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <span>用户管理</span>
          <el-input v-model="keyword" placeholder="搜用户名/昵称/手机号" clearable style="width: 220px" @keyup.enter="load" @clear="load" />
        </div>
      </template>
      <el-table :data="users" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="余额" width="100">
          <template #default="{ row }">¥{{ fmt(row.balance) }}</template>
        </el-table-column>
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-tag :type="roleStyle(row.role)" size="small" round>{{ roleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              inline-prompt active-text="正常" inactive-text="禁用"
              :disabled="isSelf(row)"
              @change="v => toggle(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :disabled="isSelf(row)" @click="openRole(row)">改角色</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum" :page-size="pageSize" :total="total"
        layout="prev, pager, next" background class="pager" @current-change="load"
      />
    </el-card>

    <!-- 改角色弹窗 -->
    <el-dialog v-model="roleVisible" title="调整用户角色" width="360px">
      <p style="color:#8a8b7a;font-size:13px">账号：<b>{{ target?.username }}</b>，当前角色：{{ roleName(target?.role) }}</p>
      <el-select v-model="newRole" style="width:100%" size="large">
        <el-option v-for="r in ['STUDENT', 'MERCHANT', 'RIDER', 'ADMIN']" :key="r" :label="roleName(r)" :value="r" />
      </el-select>
      <el-alert type="warning" :closable="false" title="改角色会覆盖原有全部角色，请谨慎操作" style="margin-top:10px" />
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRole">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userChangeRole, userChangeStatus, userPage } from '../../api/admin'
import { useUserStore } from '../../stores/user'

const fmt = (v) => Number(v ?? 0).toFixed(2)
const userStore = useUserStore()

const keyword = ref('')
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)

const roleName = (r) => ({ STUDENT: '学生', MERCHANT: '商家', ADMIN: '管理员', RIDER: '骑手' }[r] || r || '-')
const roleStyle = (r) => ({ STUDENT: 'success', MERCHANT: 'warning', ADMIN: 'danger', RIDER: 'primary' }[r] || 'info')

function isSelf(row) { return row.id === userStore.userInfo?.userId || row.username === userStore.userInfo?.username }

async function load() {
  loading.value = true
  try {
    const res = await userPage({ keyword: keyword.value, pageNum: pageNum.value, pageSize })
    users.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function toggle(row, on) {
  await ElMessageBox.confirm(`确定${on ? '启用' : '禁用'}账号 ${row.username} 吗？`, '提示', { type: 'warning' })
  await userChangeStatus(row.id, { status: on ? 1 : 0 })
  ElMessage.success('状态已更新')
  load()
}

const roleVisible = ref(false)
const saving = ref(false)
const target = ref(null)
const newRole = ref('STUDENT')

function openRole(row) {
  target.value = row
  newRole.value = row.role
  roleVisible.value = true
}

async function saveRole() {
  saving.value = true
  try {
    await userChangeRole(target.value.id, { roleCode: newRole.value })
    ElMessage.success('角色已更新')
    roleVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 1150px; margin: 0 auto; }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 14px; justify-content: center; }
</style>
