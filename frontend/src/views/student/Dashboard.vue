<template>
  <div class="page student-dash">
    <!-- 美食氛围 hero：悬浮微透的餐具 emoji -->
    <div class="hero">
      <span v-for="n in 10" :key="n" class="float-food" :style="foodStyle(n)">{{ foodEmojis[n % foodEmojis.length] }}</span>
      <div class="hero-inner">
        <div>
          <h2 class="hero-title">{{ greeting }}，今天吃什么？ 🤤</h2>
          <p class="hero-sub">全校 {{ stallCount }} 个营业档口 · {{ hotList.length }} 道热销菜等翻牌</p>
          <div class="hero-btns">
            <el-button round class="hb primary" @click="$router.push('/student/menu')">开始点餐 →</el-button>
            <el-button round class="hb" @click="$router.push('/student/orders')">查看订单</el-button>
          </div>
        </div>
        <div class="hero-card">
          <div class="hc-balance">{{ fmt(balance) }}</div>
          <div class="hc-label">校园卡余额（元）</div>
          <el-button size="small" round link @click="$router.push('/student/wallet')" style="color:#fff">去充值 ›</el-button>
        </div>
      </div>
    </div>

    <!-- 数据行 -->
    <div class="mini-stats">
      <div class="ms-item" @click="$router.push('/student/orders')">
        <span class="ms-num">{{ pendingOrders.length }}</span>
        <span class="ms-label">进行中订单</span>
      </div>
      <div class="ms-item" @click="$router.push('/student/coupons')">
        <span class="ms-num">{{ usableCoupons.length }}</span>
        <span class="ms-label">可用优惠券</span>
      </div>
      <div class="ms-item" @click="$router.push('/student/menu')">
        <span class="ms-num">{{ hotList[0]?.name || '—' }}</span>
        <span class="ms-label">今日最热菜</span>
      </div>
    </div>

    <!-- 热销榜 -->
    <el-card shadow="never" class="panel">
      <template #header>🔥 全校热销榜（实时）</template>
      <div class="hot-grid">
        <div v-for="(d, i) in hotList" :key="d.dishId" class="hot-item" @click="$router.push('/student/menu')">
          <span class="rank" :class="'r' + (i < 3 ? i + 1 : 'n')">{{ i + 1 }}</span>
          <span class="hname">{{ d.name }}</span>
          <span class="hcount">已售 {{ d.saleCount }} 份</span>
        </div>
        <el-empty v-if="hotList.length === 0" description=" getting 数据中…" :image-size="50" style="grid-column:1/-1" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dishHot } from '../../api/dish'
import { orderPage } from '../../api/order'
import { walletInfo } from '../../api/wallet'
import { couponMine } from '../../api/coupon'
import { stallList } from '../../api/basic'
import { useUserStore } from '../../stores/user'
import { useRouter } from 'vue-router'

const fmt = (v) => Number(v ?? 0).toFixed(2)
const userStore = useUserStore()
const $router = useRouter()

const foodEmojis = ['🍚', '🍜', '🥟', '🍱', '🍲', '🥘', '🧆', '🥙']
function foodStyle(n) {
  const seed = (n * 173) % 97
  return {
    left: `${(seed * 1.03) % 90 + 3}%`,
    top: `${(n * 37) % 60 + 10}%`,
    animationDuration: `${9 + (seed % 7)}s`,
    animationDelay: `-${seed % 9}s`,
    fontSize: `${26 + (seed % 5) * 6}px`
  }
}

const greeting = computed(() => {
  const h = new Date().getHours()
  const span = h < 10 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username
  return `${span}${name ? '，' + name : ''}`
})

const hotList = ref([])
const balance = ref(0)
const pendingOrders = ref([])
const usableCoupons = ref([])
const stallCount = ref('—')

function jumpMenu() { $router.push('/student/menu') }
void jumpMenu

onMounted(async () => {
  dishHot({ limit: 8 }).then(r => { hotList.value = r.data || [] })
  walletInfo({ pageNum: 1, pageSize: 1 }).then(r => { balance.value = r.data?.balance ?? 0 })
  orderPage({ status: 0, pageNum: 1, pageSize: 10 }).then(r => { pendingOrders.value = r.data?.list || [] })
  couponMine().then(r => { usableCoupons.value = (r.data || []).filter(c => !c.used) })
  stallList({}).then(r => { stallCount.value = (r.data || []).filter(s => s.status === 1).length })
})
</script>

<style scoped>
/* 学生端主题：鼠尾草绿 x 奶油白 */
.student-dash {
  --ac: #93977a;
  --ac-soft: #83886a;
}
.page { max-width: 1080px; margin: 0 auto; }

/* hero：美食氛围，微透不过量 */
.hero {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  padding: 30px 32px;
  background: linear-gradient(120deg, #93977a 0%, #a7ab8c 55%, #c2c5a7 100%);
  box-shadow: 0 14px 34px rgba(147, 151, 122, 0.4);
}
.float-food {
  position: absolute;
  animation: float-up ease-in-out infinite alternate;
  opacity: 0.14;
  pointer-events: none;
  filter: saturate(0.7);
}
@keyframes float-up {
  from { transform: translateY(16px) rotate(-8deg); }
  to { transform: translateY(-14px) rotate(10deg); }
}
.hero-inner {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}
.hero-title { margin: 0; font-size: 21px; color: #434537; }
.hero-sub { margin: 7px 0 0; font-size: 13px; color: #5c5e4c; }
.hero-btns { margin-top: 16px; display: flex; gap: 10px; }
.hb.primary { background: #fffdf2; border: none; color: #5f6543; font-weight: 600; }
.hb.primary:hover { background: #fff; color: #4a5033; }
.hb { border-color: rgba(255, 255, 255, 0.6); color: #fff; }
.hb:hover { border-color: #fff; color: #fff; }

/* 余额玻璃小卡 */
.hero-card {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 14px;
  padding: 14px 22px;
  text-align: center;
  color: #fff;
  min-width: 180px;
}
.hc-balance { font-size: 27px; font-weight: 800; letter-spacing: 1px; }
.hc-label { font-size: 11.5px; opacity: 0.85; margin: 2px 0 4px; }

/* 数据行 */
.mini-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin: 16px 0;
}
.ms-item {
  cursor: pointer;
  background: #fff;
  border-radius: 13px;
  padding: 14px 18px;
  box-shadow: 0 4px 14px rgba(147, 151, 122, 0.14);
  transition: transform 0.2s;
  display: flex;
  align-items: baseline;
  gap: 10px;
  overflow: hidden;
}
.ms-item:hover { transform: translateY(-2px); }
.ms-num {
  font-size: 19px;
  font-weight: 800;
  color: var(--ac);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 65%;
}
.ms-label { font-size: 12.5px; color: #9b9d84; }

/* 热销榜 */
.panel {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.14);
}
.hot-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 26px;
}
@media (max-width: 900px) { .hot-grid { grid-template-columns: 1fr; } }
.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 6px;
  border-bottom: 1px dashed #eceada;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.hot-item:hover { background: #f7f6ea; }
.rank {
  width: 24px; height: 24px;
  display: grid; place-items: center;
  border-radius: 8px;
  font-size: 12px; font-weight: 700;
  background: #efede1; color: #8a8b7a;
  flex: none;
}
.rank.r1 { background: #d9a05b; color: #fff; }
.rank.r2 { background: #b3b79a; color: #fff; }
.rank.r3 { background: #c9a37c; color: #fff; }
.hname { flex: 1; color: #4f5040; font-size: 13.5px; }
.hcount { color: #9b9d84; font-size: 12px; }
</style>
