<template>
  <!-- 管理端布局：顶栏 + 侧栏，菜单为 mock，后续填充真实页面 -->
  <FrameLayout :menus="menus">
    <div class="stat-cards">
      <el-card v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card>
      <template #header>待办事项（mock 数据）</template>
      <el-table :data="todos" stripe>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="time" label="提交时间" width="120" />
      </el-table>
      <!-- TODO: 第3-4周 开发用户管理 / 档口审核 / 全局统计图表 -->
      <el-empty description="用户管理 / 档口审核 / 全局统计开发中" :image-size="60" />
    </el-card>
  </FrameLayout>
</template>

<script setup>
import FrameLayout from '../../layouts/FrameLayout.vue'

const menus = [
  { path: '/admin', title: '总览首页', icon: 'HomeFilled' },
  { path: '/admin/users', title: '用户管理', icon: 'User' },
  { path: '/admin/merchants', title: '档口审核', icon: 'Shop' },
  { path: '/admin/stats', title: '全局统计', icon: 'TrendCharts' }
]

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
