<template>
  <!-- 商家端布局：顶栏 + 侧栏，菜单为 mock，后续填充真实页面 -->
  <FrameLayout :menus="menus">
    <div class="stat-cards">
      <el-card v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-value">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </el-card>
    </div>

    <el-card>
      <template #header>今日订单看板（mock 数据）</template>
      <el-table :data="orders" stripe>
        <el-table-column prop="id" label="订单号" width="140" />
        <el-table-column prop="dish" label="菜品" />
        <el-table-column prop="count" label="份数" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '待出餐' ? 'danger' : 'success'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="下单时间" width="120" />
      </el-table>
      <!-- TODO: 第2-3周 开发菜品管理 / 出餐看板 / 销量图表 -->
      <el-empty description="菜品管理 / 销量图表开发中" :image-size="60" />
    </el-card>
  </FrameLayout>
</template>

<script setup>
import FrameLayout from '../../layouts/FrameLayout.vue'

const menus = [
  { path: '/merchant', title: '经营首页', icon: 'HomeFilled' },
  { path: '/merchant/dishes', title: '菜品管理', icon: 'Dish' },
  { path: '/merchant/orders', title: '出餐看板', icon: 'List' },
  { path: '/merchant/stats', title: '销量统计', icon: 'TrendCharts' }
]

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
