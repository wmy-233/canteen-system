<template>
  <!-- 学生端首页：布局由路由父级 FrameLayout 统一提供 -->
  <div class="page">
    <div class="hero">
      <div>
        <h2 class="hero-title">今天，想吃点什么？ 😋</h2>
        <p class="hero-sub">为你挑选全校今日好评菜品</p>
      </div>
      <span class="hero-emoji">🍱</span>
    </div>

    <div class="stat-cards">
      <el-card v-for="s in stats" :key="s.label" shadow="never" class="stat-card">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="panel">
      <template #header>今日推荐菜品（mock 数据）</template>
      <el-table :data="dishes" stripe>
        <el-table-column prop="name" label="菜品" />
        <el-table-column prop="window" label="档口" />
        <el-table-column prop="price" label="价格(元)" width="100" />
        <el-table-column prop="stock" label="今日限量剩余" width="120">
          <template #default="{ row }">
            <el-progress :percentage="Math.min(100, row.stock * 6)" :show-text="false" :stroke-width="8" style="width: 90%" />
          </template>
        </el-table-column>
      </el-table>
      <!-- TODO: 第2周 B 开发菜品浏览（分类Tab + 菜品卡片 + 加入购物车） -->
      <el-empty description="菜品浏览 / 购物车开发中（第2周 D1-D3）" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
const stats = [
  { label: '钱包余额(元)', value: '58.50' },
  { label: '进行中订单', value: '1' },
  { label: '可用优惠券', value: '2' }
]

const dishes = [
  { name: '红烧肉套餐', window: '一号档口', price: 12, stock: 8 },
  { name: '番茄炒蛋', window: '二号档口', price: 8, stock: 25 },
  { name: '麻辣香锅', window: '三号档口', price: 15, stock: 3 },
  { name: '牛肉拉面', window: '面食档口', price: 10, stock: 16 }
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
  color: #5e5f4e;
  background: linear-gradient(120deg, #b2b79a 0%, #c2c7ab 55%, #d8d9c3 100%);
  box-shadow: 0 8px 22px rgba(147, 151, 122, 0.35);
}
.hero-title {
  margin: 0;
  font-size: 20px;
  color: #4c4e3e;
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
