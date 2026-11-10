<template>
  <div class="page">
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="🎉 限时领券" name="issuing" />
      <el-tab-pane label="🎫 我的卡包" name="mine" />
    </el-tabs>

    <div v-loading="loading" class="coupon-grid">
      <div v-for="c in coupons" :key="c.id" class="coupon-card" :class="{ used: c.used }">
        <div class="cc-value">
          <span class="num">{{ fmt(c.amount) }}</span>
          <span class="unit">元</span>
        </div>
        <div class="cc-mid">
          <div class="cc-name">{{ c.name }}</div>
          <div class="cc-cond">满 {{ fmt(c.threshold) }} 元可用</div>
          <div class="cc-expire">
            有效期至 {{ (c.expireTime || '').slice(0, 16) }}
            <template v-if="tab === 'issuing'"> · 剩余 {{ c.stock }} 张</template>
          </div>
        </div>
        <div class="cc-act">
          <template v-if="tab === 'issuing'">
            <button type="button" class="claim-btn" @click="claim(c)">立即领取</button>
          </template>
          <template v-else>
            <el-tag v-if="c.used" type="info" round size="small">已使用</el-tag>
            <el-tag v-else type="success" round size="small">未使用</el-tag>
          </template>
        </div>
      </div>
      <el-empty v-if="!loading && coupons.length === 0"
        :description="tab === 'issuing' ? '暂无可领的券，等管理员上新' : '卡包空空的，去领几张吧～'" style="grid-column: 1/-1" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { couponClaim, couponIssuing, couponMine } from '../../api/coupon'

const fmt = (v) => Number(v ?? 0).toFixed(0)
const tab = ref('issuing')
const coupons = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = tab.value === 'issuing' ? await couponIssuing() : await couponMine()
    coupons.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function claim(c) {
  await couponClaim(c.id)
  ElMessage.success(`已领取「${c.name}」，去下单使用吧！`)
  load()
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 1080px; margin: 0 auto; }
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 14px;
  min-height: 160px;
}
.coupon-card {
  display: flex;
  align-items: center;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.16);
  transition: transform 0.2s ease;
}
.coupon-card:hover { transform: translateY(-3px); }
.coupon-card.used { opacity: 0.45; filter: grayscale(0.6); }
.cc-value {
  width: 86px;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(150deg, #93977a, #b3b79a);
}
.cc-value .num { font-size: 26px; font-weight: 800; }
.cc-value .unit { font-size: 11px; }
.cc-mid { flex: 1; padding: 12px 14px; }
.cc-name { font-weight: 700; color: #4f5040; font-size: 14.5px; }
.cc-cond { font-size: 12px; color: #8a8b7a; margin-top: 3px; }
.cc-expire { font-size: 11px; color: #b3b5a0; margin-top: 3px; }
.cc-act { padding-right: 14px; }
.claim-btn {
  border: 1.5px solid #93977a;
  background: #fff;
  color: #83896a;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12.5px;
  cursor: pointer;
  transition: all 0.2s;
}
.claim-btn:hover { background: #93977a; color: #fff; }
</style>
