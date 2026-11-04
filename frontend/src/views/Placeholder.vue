<template>
  <el-card>
    <template #header>{{ title }}</template>
    <el-empty :description="`${title} 页面开发中（第 2-4 周迭代）`" />
  </el-card>
</template>

<script setup>
defineProps({
  title: { type: String, default: '功能' }
})
</script>
