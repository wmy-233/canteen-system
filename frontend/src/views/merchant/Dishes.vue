<template>
  <div class="page">
    <!-- 档口状态条 -->
    <div v-if="stalls.length" class="stall-bar">
      <span class="sb-label">我的档口：</span>
      <el-radio-group v-model="currentStallId" @change="load">
        <el-radio-button v-for="s in stalls" :key="s.id" :value="s.id">
          {{ s.name }}（{{ s.status === 1 ? '营业中' : s.status === 0 ? '已停业' : '待审核' }}）
        </el-radio-button>
      </el-radio-group>
    </div>
    <el-empty v-else description="你还没有入驻档口，请联系管理员审核开通" />

    <el-card shadow="never" class="panel">
      <template #header>
        <div class="panel-head">
          <span>菜品管理</span>
          <div class="panel-ops">
            <el-input v-model="keyword" placeholder="搜菜名" clearable style="width: 180px" @keyup.enter="load" @clear="load" />
            <el-button type="primary" round @click="openSave()">＋ 新增菜品</el-button>
          </div>
        </div>
      </template>

      <el-table :data="dishes" stripe v-loading="loading">
        <el-table-column prop="name" label="菜品" min-width="140" />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">{{ cateName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="90">
          <template #default="{ row }">¥{{ fmt(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="saleCount" label="销量" width="80" />
        <el-table-column label="日限量" width="90">
          <template #default="{ row }">{{ row.dailyStock > 0 ? row.dailyStock : '不限' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" inline-prompt active-text="上架" inactive-text="下架" @change="v => toggle(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openSave(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pageNum" :page-size="pageSize" :total="total"
        layout="prev, pager, next" background class="pager" @current-change="load"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="saveVisible" :title="form.id ? '编辑菜品' : '新增菜品'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="菜品名" required>
          <el-input v-model="form.name" maxlength="50" placeholder="不超过50字" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryId" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格(元)" required>
          <el-input-number v-model="form.price" :min="0.01" :max="9999" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="日限量">
          <el-input-number v-model="form.dailyStock" :min="0" :max="9999" style="width:100%" />
          <div class="form-tip">0 表示不限量；限量数会在前端展示「仅剩N份」紧迫感</div>
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.image" placeholder="选填，留空显示默认美食图" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="form.on" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryList } from '../../api/basic'
import { dishChangeStatus, dishDelete, dishPage, dishSave, stallMine } from '../../api/merchant'

const fmt = (v) => Number(v ?? 0).toFixed(2)

const stalls = ref([])
const currentStallId = ref(undefined)
const categories = ref([])
const dishes = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const keyword = ref('')
const loading = ref(false)
const cateMap = {}

function cateName(id) { return cateMap[id] || '未知' }

async function load() {
  if (!currentStallId.value) return
  loading.value = true
  try {
    const res = await dishPage({ stallId: currentStallId.value, keyword: keyword.value, pageNum: pageNum.value, pageSize })
    dishes.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

// ---------- 新增/编辑 ----------
const saveVisible = ref(false)
const saving = ref(false)
const form = ref({})

function openSave(row) {
  form.value = row
    ? { ...row, on: row.status === 1 }
    : { name: '', categoryId: null, price: 1, dailyStock: 0, image: '', description: '', on: true }
  saveVisible.value = true
}

async function save() {
  saving.value = true
  try {
    const f = form.value
    await dishSave({
      id: f.id,
      stallId: currentStallId.value,
      categoryId: f.categoryId,
      name: f.name,
      description: f.description || undefined,
      price: f.price,
      image: f.image || undefined,
      dailyStock: f.dailyStock ?? 0,
      status: f.on ? 1 : 0
    })
    ElMessage.success('保存成功')
    saveVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row, on) {
  await dishChangeStatus(row.id, on ? 1 : 0)
  ElMessage.success(on ? '已上架' : '已下架')
  load()
}

async function del(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？该操作不可恢复`, '删除菜品', { type: 'warning' })
  await dishDelete(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  const [a, b] = await Promise.all([stallMine(), categoryList()])
  stalls.value = a.data || []
  categories.value = b.data || []
  categories.value.forEach(c => { cateMap[c.id] = c.name })
  if (stalls.value.length) {
    currentStallId.value = stalls.value[0].id
    await load()
  }
})
</script>

<style scoped>
.page { max-width: 1080px; margin: 0 auto; }
.stall-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.sb-label { color: #5e5f4e; font-size: 13px; }
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.panel-head { display: flex; justify-content: space-between; align-items: center; }
.panel-ops { display: flex; gap: 10px; }
.pager { margin-top: 14px; justify-content: center; }
.form-tip { font-size: 11.5px; color: #9b9d84; line-height: 1.5; }
</style>
