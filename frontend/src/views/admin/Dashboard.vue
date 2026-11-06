<template>
  <!-- 管理端首页：布局由路由父级 FrameLayout 统一提供 -->
  <div class="page">
    <div class="hero">
      <div>
        <h2 class="hero-title">平台运营总览 🏙️</h2>
        <p class="hero-sub">把每一个档口，都经营得更好</p>
      </div>
      <span class="hero-emoji">📊</span>
    </div>

    <div class="stat-cards">
      <el-card v-for="s in stats" :key="s.label" shadow="never" class="stat-card">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="panel">
      <template #header>待办事项（mock 数据）</template>
      <el-table :data="todos" stripe>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" round>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="time" label="提交时间" width="120" />
      </el-table>
      <!-- TODO: 第3-4周 开发用户管理 / 档口审核 / 全局统计图表 -->
      <el-empty description="用户管理 / 档口审核 / 全局统计开发中" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
const stats = [
  { label: '注册用户数', value: '1286' },
  { label: '入驻档口数', value: '12' },
  { label: '今日订单量', value: '512' }
]

const todos = [
  { type: '档口入驻', content: '「川湘风味」档口提交入驻申请，待审核', time: '11-03' },
  { type: '用户反馈', content: '学生反馈三号档口出餐慢，建议关注', time: '11-03' }
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
  background: linear-gradient(120deg, #a4ab8d 0%, #b7bda0 55%, #d3d7c0 100%);
  box-shadow: 0 8px 22px rgba(147, 151, 122, 0.35);
}
.hero-title {
  margin: 0;
  font-size: 20px;
  color: #454a38;
}
.hero-sub {
  margin: 6px 0 0;
  color: #5c6146;
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
