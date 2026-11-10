<template>
  <div class="page">
    <!-- 筛选区 -->
    <div class="filter-bar">
      <el-radio-group v-model="query.canteenId" @change="onFilterChange">
        <el-radio-button :value="undefined">全部食堂</el-radio-button>
        <el-radio-button v-for="c in canteens" :key="c.id" :value="c.id">{{ c.name }}</el-radio-button>
      </el-radio-group>
      <el-select v-model="query.stallId" placeholder="全部档口" clearable style="width: 160px" @change="loadDishes">
        <el-option v-for="s in stalls" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索菜名…"
        clearable
        style="width: 200px"
        :prefix-icon="'Search'"
        @keyup.enter="loadDishes"
        @clear="loadDishes"
      />
    </div>

    <!-- 分类 tab -->
    <el-tabs v-model="query.categoryId" class="cate-tabs" @tab-change="loadDishes">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane v-for="c in categories" :key="c.id" :label="c.name" :name="String(c.id)" />
    </el-tabs>

    <!-- 菜品卡片 -->
    <div v-loading="loading" class="dish-grid">
      <div v-for="d in dishes" :key="d.id" class="dish-card" @click="showDetail(d)">
        <div class="dish-img">
          <img v-if="d.image" :src="d.image" alt="">
          <span v-else>🍜</span>
        </div>
        <div class="dish-body">
          <div class="dish-name">{{ d.name }}</div>
          <div class="dish-meta">
            <span class="dish-stall">{{ d.stallName }}</span>
            <el-tag v-if="d.avgScore" type="warning" size="small" round>⭐{{ d.avgScore }}</el-tag>
          </div>
          <div class="dish-bottom">
            <span class="dish-price">¥{{ fmt(d.price) }}</span>
            <el-tag v-if="d.dailyStock > 0" :type="d.dailyStock <= 3 ? 'danger' : 'info'" size="small" round>
              {{ d.dailyStock <= 3 ? `仅剩${d.dailyStock}份` : `今日${d.dailyStock}份` }}
            </el-tag>
            <el-tag v-else size="small" type="success" round>供应中</el-tag>
          </div>
        </div>
        <el-button class="add-btn" type="primary" size="small" round :disabled="d.status !== 1" @click.stop="add(d)">
          ＋ 加购
        </el-button>
      </div>
      <el-empty v-if="!loading && dishes.length === 0" description="这个筛选下暂时没有菜品哦" style="grid-column: 1/-1" />
    </div>

    <el-pagination
      v-model:current-page="query.pageNum"
      :page-size="query.pageSize"
      :total="total"
      layout="prev, pager, next, total"
      background
      class="pager"
      @current-change="loadDishes"
    />

    <!-- 菜品详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="detail?.name" width="420px">
      <div v-if="detail" class="detail-box">
        <p class="desc">{{ detail.description || '（暂无描述）' }}</p>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="价格">¥{{ fmt(detail.price) }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ detail.avgScore ? '⭐ ' + detail.avgScore : '暂无评价' }}</el-descriptions-item>
          <el-descriptions-item label="累计销量">{{ detail.saleCount }} 份</el-descriptions-item>
          <el-descriptions-item label="档口">{{ detail.stallName }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 购物车抽屉 -->
    <el-drawer v-model="cartVisible" size="380px">
      <template #header>
        <div class="cart-header">
          <span>🛒 我的购物车</span>
          <el-button v-if="cart.length" link type="danger" size="small" @click="clearCart">清空</el-button>
        </div>
      </template>
      <div v-loading="cartLoading" class="cart-list">
        <div v-for="item in cart" :key="item.id" class="cart-item">
          <div class="ci-info">
            <div class="ci-name">{{ item.dishName }}</div>
            <div class="ci-meta">{{ item.stallName }} · ¥{{ fmt(item.price) }}</div>
          </div>
          <div class="ci-ops">
            <el-input-number
              :model-value="item.quantity"
              :min="1" :max="99" size="small"
              @change="(v) => changeQty(item, v)"
            />
            <el-button link type="danger" size="small" @click="removeItem(item)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!cartLoading && cart.length === 0" description="购物车空空的，快去加菜吧～" />
      </div>
      <template #footer>
        <el-button type="primary" size="large" class="go-checkout" :disabled="cart.length === 0" @click="openCheckout">
          去结算 · ¥{{ fmt(cartTotal) }}
        </el-button>
      </template>
    </el-drawer>

    <!-- 结算弹窗 -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="440px">
      <div class="checkout">
        <div class="ck-row">
          <span class="ck-label">用餐方式</span>
          <el-radio-group v-model="checkout.deliveryType">
            <el-radio :value="0">自取（凭出餐码）</el-radio>
            <el-radio :value="1">配送（骑手上门）</el-radio>
          </el-radio-group>
        </div>
        <div class="ck-row">
          <span class="ck-label">优惠券</span>
          <el-select v-model="checkout.couponId" placeholder="自动选择最优券" clearable style="flex:1">
            <el-option
              v-for="c in usableCoupons" :key="c.id"
              :label="`${c.name}（满${fmt(c.threshold)}减${fmt(c.amount)}）`" :value="c.id"
            />
          </el-select>
        </div>
        <el-alert v-if="usableCoupons.length === 0" type="info" :closable="false"
          title="暂无可用优惠券，可去「优惠券」页面领取" style="margin-bottom:8px" />
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="商品合计">¥{{ fmt(cartTotal) }}</el-descriptions-item>
          <el-descriptions-item label="优惠减免">- ¥{{ fmt(discount) }}</el-descriptions-item>
          <el-descriptions-item label="实付">
            <span class="pay-amount">¥{{ fmt(cartTotal - discount) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-alert type="warning" :closable="false" style="margin-top:8px"
          title="下单将直接从校园卡余额扣款，余额不足请先去「我的钱包」充值" />
      </div>
      <template #footer>
        <el-button @click="checkoutVisible = false">再想想</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">确认下单</el-button>
      </template>
    </el-dialog>

    <!-- 下单成功 -->
    <el-dialog v-model="doneVisible" title="🎉 下单成功" width="380px">
      <div v-if="orderResult" class="done-box">
        <div class="done-item">订单号 <b>{{ orderResult.orderNo }}</b></div>
        <div class="pickup-code">{{ orderResult.deliveryType === 0 ? orderResult.pickupCode : '🛵' }}</div>
        <div class="done-sub">{{ orderResult.deliveryType === 0 ? '出餐后凭取餐码取餐' : '商家出餐后将由骑手送至目的地' }}</div>
        <el-descriptions :column="3" size="small" style="margin-top:12px">
          <el-descriptions-item label="合计">{{ fmt(orderResult.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="优惠">-{{ fmt(orderResult.discountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付">¥{{ fmt(orderResult.payAmount) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="doneVisible = false">继续逛</el-button>
        <el-button type="primary" @click="$router.push('/student/orders')">查看订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { canteenList, categoryList, stallList } from '../../api/basic'
import { cartAdd, cartClear, cartList, cartRemove, cartUpdate, dishPage } from '../../api/dish'
import { couponMine } from '../../api/coupon'
import { orderCreate } from '../../api/order'

const fmt = (v) => Number(v ?? 0).toFixed(2)

// ---------- 筛选 / 菜品列表 ----------
const query = reactive({ canteenId: undefined, stallId: undefined, categoryId: 'all', keyword: '', pageNum: 1, pageSize: 12 })
const canteens = ref([])
const stalls = ref([])
const categories = ref([])
const dishes = ref([])
const total = ref(0)
const loading = ref(false)

async function loadBase() {
  const [a, b, c] = await Promise.all([canteenList(), categoryList(), stallList()])
  canteens.value = a.data || []
  categories.value = b.data || []
  stalls.value = c.data || []
}

async function loadDishes() {
  loading.value = true
  try {
    const res = await dishPage({
      ...query,
      categoryId: query.categoryId === 'all' ? undefined : Number(query.categoryId)
    })
    dishes.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function onFilterChange() {
  query.stallId = undefined
  stallList(query.canteenId ? { canteenId: query.canteenId } : {}).then(r => { stalls.value = r.data || [] })
  await loadDishes()
}

const detailVisible = ref(false)
const detail = ref(null)
function showDetail(d) { detail.value = d; detailVisible.value = true }

// ---------- 购物车 ----------
const cartVisible = ref(false)
const cart = ref([])
const cartLoading = ref(false)
const cartTotal = computed(() => cart.value.reduce((s, i) => s + Number(i.price) * Number(i.quantity), 0))

async function refreshCart() {
  cartLoading.value = true
  try {
    const res = await cartList()
    cart.value = res.data || []
  } finally {
    cartLoading.value = false
  }
}

async function add(d) {
  await cartAdd({ dishId: d.id, quantity: 1 })
  ElMessage.success(`已加入购物车：「${d.name}」`)
  await refreshCart()
  cartVisible.value = true
}

async function changeQty(item, v) {
  if (!v || v < 1) return
  await cartUpdate(item.dishId, { quantity: v })
  await refreshCart()
}

async function removeItem(item) {
  await cartRemove(item.id)
  ElMessage.success('已移除')
  await refreshCart()
}

async function clearCart() {
  await ElMessageBox.confirm('确定清空购物车吗？', '提示', { type: 'warning' })
  await cartClear()
  ElMessage.success('已清空')
  await refreshCart()
}

// ---------- 结算 ----------
const checkoutVisible = ref(false)
const submitting = ref(false)
const doneVisible = ref(false)
const orderResult = ref(null)
const usableCoupons = ref([])
const checkout = reactive({ deliveryType: 0, couponId: undefined })

const pickedCoupon = computed(() => usableCoupons.value.find(c => c.id === checkout.couponId))
const discount = computed(() => {
  const c = pickedCoupon.value
  if (!c || cartTotal.value < Number(c.threshold)) return 0
  return Math.min(Number(c.amount), cartTotal.value)
})

function openCheckout() {
  cartVisible.value = false
  Object.assign(checkout, { deliveryType: 0, couponId: undefined })
  couponMine().then(r => { usableCoupons.value = (r.data || []).filter(c => !c.used) })
  checkoutVisible.value = true
}

async function submitOrder() {
  submitting.value = true
  try {
    const res = await orderCreate({
      deliveryType: checkout.deliveryType,
      couponId: checkout.couponId || undefined,
      items: cart.value.map(i => ({ dishId: i.dishId, quantity: i.quantity }))
    })
    orderResult.value = res.data
    checkoutVisible.value = false
    doneVisible.value = true
    await refreshCart()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadBase()
  await loadDishes()
  refreshCart()
})
</script>

<style scoped>
.page {
  max-width: 1180px;
  margin: 0 auto;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.cate-tabs :deep(.el-tabs__item.is-active) {
  color: #93977a;
}
.cate-tabs :deep(.el-tabs__active-bar) {
  background-color: #93977a;
}

/* 菜品网格 */
.dish-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(215px, 1fr));
  gap: 16px;
  min-height: 200px;
}
.dish-card {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 4px 16px rgba(147, 151, 122, 0.16);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  padding-bottom: 12px;
}
.dish-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(147, 151, 122, 0.3);
}
.dish-img {
  height: 110px;
  display: grid;
  place-items: center;
  font-size: 40px;
  background: linear-gradient(120deg, #eff0e2, #e4e6d4);
}
.dish-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.dish-body { padding: 10px 12px 0; }
.dish-name {
  font-weight: 600;
  font-size: 14.5px;
  color: #4f5040;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dish-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  font-size: 12px;
  color: #9b9d84;
}
.dish-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}
.dish-price {
  color: #b0714f;
  font-weight: 700;
  font-size: 16px;
}
.add-btn {
  margin: 8px 12px 0;
  background: #93977a;
  border-color: #93977a;
  width: calc(100% - 24px);
}
.add-btn:hover, .add-btn:focus {
  background: #83896a;
  border-color: #83896a;
}
.pager {
  margin-top: 18px;
  justify-content: center;
}
.detail-box .desc {
  font-size: 13px;
  color: #8a8b7a;
}

/* 购物车抽屉 */
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.cart-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f6f4ec;
}
.ci-name { font-weight: 600; font-size: 14px; color: #4f5040; }
.ci-meta { font-size: 12px; color: #9b9d84; margin-top: 2px; }
.ci-ops { display: flex; align-items: center; gap: 6px; }
.go-checkout {
  width: 100%;
  background: #93977a;
  border-color: #93977a;
}
.go-checkout:hover, .go-checkout:focus {
  background: #83896a;
  border-color: #83896a;
}

/* 结算 */
.checkout .ck-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
}
.checkout .ck-label { color: #5e5f4e; font-size: 13px; width: 56px; flex: none; }
.pay-amount { color: #b0714f; font-weight: 700; font-size: 18px; }

/* 下单成功 */
.done-box { text-align: center; }
.done-item { color: #8a8b7a; font-size: 13px; }
.pickup-code {
  margin-top: 10px;
  font-size: 40px;
  font-weight: 800;
  letter-spacing: 6px;
  color: #93977a;
}
.done-sub { color: #9b9d84; font-size: 12px; margin-top: 4px; }
</style>
