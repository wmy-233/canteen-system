<template>
  <!-- 商家端首页：布局由路由父级 FrameLayout 统一提供 -->
  <div class="page">
    <div class="hero">
      <div>
        <h2 class="hero-title">今日生意概览 📈</h2>
        <p class="hero-sub">接单即上灶，出餐赢好评</p>
      </div>
      <span class="hero-emoji">🏪</span>
    </div>

    <div class="stat-cards">
      <el-card v-for="s in stats" :key="s.label" shadow="never" class="stat-card">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="panel">
      <template #header>今日订单看板（mock 数据）</template>
      <el-table :data="orders" stripe>
        <el-table-column prop="id" label="订单号" width="140" />
        <el-table-column prop="dish" label="菜品" />
        <el-table-column prop="count" label="份数" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '待出餐' ? 'danger' : 'success'" size="small" round>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="下单时间" width="120" />
      </el-table>
      <!-- TODO: 第2-3周 开发菜品管理 / 出餐看板 / 销量图表 -->
      <el-empty description="菜品管理 / 销量图表开发中" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
const stats = [
  { label: '今日订单数', value: '46' },
  { label: '今日营业额(元)', value: '528.00' },
  { label: '待出餐订单', value: '3' }
]

const orders = [
  { id: '202611040001', dish: '红烧肉套餐 x1', count: 1, status: '待出餐', time: '11:32' },
  { id: '202611040002', dish: '麻辣香锅 x2', count: 2, status: '待出餐', time: '11:35' },
  { id: '202611040003', dish: '牛肉拉面 x1', count: 1, status: '已取餐', time: '11:40' }
]
</script>

<style scoped>
.page {
  max-width: 1080px;
  margin: 0 auto;
}
@media (max-width: 1200px) {
  .page { max-width: none; }
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 26px;
  margin-bottom: 18px;
  border-radius: 16px;
  background: linear-gradient(120deg, #b7b79a 0%, #c8c7ab 55%, #dcdcc6 100%);
  box-shadow: 0 8px 22px rgba(147, 151, 122, 0.35);
}
.hero-title {
  margin: 0;
  font-size: 20px;
  color: #4c4b3a;
}
.hero-sub {
  margin: 6px 0 0;
  color: #6c6e58;
  font-size: 13px;
}
.hero-emoji {
  font-size: 42px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.12));
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 18px;
}
.stat-card {
  border-radius: 14px;
  border: none;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(147, 151, 122, 0.3);
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #5e5f4e;
}
.stat-label {
  margin-top: 4px;
  color: #9b9d84;
  font-size: 13px;
}
.stat-card:nth-child(1) { background: linear-gradient(135deg, #f1efe0, #eae8d7); }
.stat-card:nth-child(2) { background: linear-gradient(135deg, #e9ecdb, #e2e6d1); }
.stat-card:nth-child(3) { background: linear-gradient(135deg, #efeee3, #e6e7d8); }

.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.16);
}
</style>
