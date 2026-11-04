<template>
  <!-- 学生端布局：顶栏 + 侧栏，菜单为 mock，后续按第2周计划填充真实页面 -->
  <FrameLayout :menus="menus">
    <div class="stat-cards">
      <el-card v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card>
      <template #header>今日推荐菜品（mock 数据）</template>
      <el-table :data="dishes" stripe>
        <el-table-column prop="name" label="菜品" />
        <el-table-column prop="window" label="档口" />
        <el-table-column prop="price" label="价格(元)" width="100" />
        <el-table-column prop="stock" label="今日限量剩余" width="120" />
      </el-table>
      <!-- TODO: 第2周 B 开发菜品浏览（分类Tab + 菜品卡片 + 加入购物车） -->
      <el-empty description="菜品浏览 / 购物车开发中（第2周 D1-D3）" :image-size="60" />
    </el-card>
  </FrameLayout>
</template>

<script setup>
import FrameLayout from '../../layouts/FrameLayout.vue'

const menus = [
  { path: '/student', title: '首页', icon: 'HomeFilled' },
  { path: '/student/menu', title: '菜品浏览', icon: 'Dish' },
  { path: '/student/orders', title: '我的订单', icon: 'List' },
  { path: '/student/wallet', title: '我的钱包', icon: 'Wallet' },
  { path: '/student/coupons', title: '优惠券', icon: 'Ticket' }
]

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
