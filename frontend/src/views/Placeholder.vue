<template>
  <el-card shadow="never" class="placeholder-card">
    <template #header>🚧 {{ title }}</template>
    <el-empty :description="`${title} 页面开发中（第 2-4 周迭代）`" />
  </el-card>
</template>

<script setup>
defineProps({
  title: { type: String, default: '功能' }
})
</script>

<style scoped>
.placeholder-card {
  max-width: 1080px;
  margin: 0 auto;
  border-radius: 14px;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.16);
}
</style>
